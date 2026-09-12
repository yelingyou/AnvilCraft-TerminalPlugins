/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for full license text.
 */
package dev.anvilcraft.addon.terminalplugins.plugin.impl;

import dev.anvilcraft.addon.terminalplugins.AnvilCraftTerminalPlugins;
import dev.anvilcraft.addon.terminalplugins.component.ChargingSettings;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginContext;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginKind;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPlugin;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.DynamicPowerComponent;
import dev.dubhe.anvilcraft.api.power.IDynamicPowerComponentHolder;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.ChargerChargingRecipe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.List;

/**
 * 充能插件：接入铁砧工艺电网，用本体那套规则给东西充能。
 *
 * <p><b>接电网</b>：把需求功率写进玩家 {@link DynamicPowerComponent} 的用电集合（和本体飘升机背包同款 API），
 * 电网自己判断够不够；过载时 {@code PowerGrid#isWorking()} 为 false，本周期就什么都不做。</p>
 *
 * <p><b>KW → FE</b>：直接读本体配置文件 `AnvilCraft.CONFIG.powerConverter`，
 * 速率 = `申请功率 × powerConverterEfficiency`（默认 1kW = 100FE/t），
 * 批量口径与本体充电器一致（一次派发 = {@code pluginTickInterval} tick）。
 * 这条路径**不**乘 `powerConverterLoss` —— 本体充电器分支也没有乘，损耗只出现在能量转换器方块上。</p>
 *
 * <p><b>充能配方</b>：走 {@code anvilcraft:charger_charging}，配方里的 {@code power} 是负的千瓦数、
 * {@code time} 是总时长。只有 `申请功率 ≥ |power|` 时才推进，进度存在插件物品的数据组件里。</p>
 */
public class ChargingPlugin implements TerminalPlugin {
    @Override
    public PluginKind kind() {
        return PluginKind.CHARGING;
    }

    /** 派发器本身就是每 {@code pluginTickInterval} tick 跑一次，这里跟上即可。 */
    @Override
    public int intervalTicks() {
        return 10;
    }

    @Override
    public void onPlayerTick(PluginContext context) {
        ServerPlayer player = context.player();
        if (player == null) {
            return;
        }
        ItemStack pluginStack = context.pluginStack();
        ChargingSettings settings = pluginStack.getOrDefault(
            AddonDataComponents.CHARGING_SETTINGS, ChargingSettings.DEFAULT);
        if (settings.idle()) {
            ChargingPower.release(player);
            return;
        }

        DynamicPowerComponent component = IDynamicPowerComponentHolder.of(player).anvilcraft$getPowerComponent();
        ChargingPower.reserve(player, component, settings.powerKw());
        PowerGrid grid = component.getPowerGrid();
        if (grid == null || !(grid.isWorking() || grid.isHasInfinitePower())) {
            return;
        }

        int ticks = Math.max(1, AnvilCraftTerminalPlugins.CONFIG.pluginTickInterval);
        if (settings.chargeItems()) {
            ChargingPlugin.chargeItems(player, settings, ticks);
        }
        if (settings.runRecipes() && context.storageReachable()) {
            ChargingPlugin.runRecipe(context, pluginStack, player, settings, ticks);
        }
    }

    /** 给玩家身上的 FE 物品充电：本周期能注入的 FE = 功率 × 效率 × tick 数。 */
    private static void chargeItems(ServerPlayer player, ChargingSettings settings, int ticks) {
        int efficiency = AnvilCraft.CONFIG.powerConverter.powerConverterEfficiency;
        int budget = settings.powerKw() * efficiency * ticks;
        if (budget <= 0) {
            return;
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && budget > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            IEnergyStorage storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (storage == null || !storage.canReceive()) {
                continue;
            }
            int accepted = storage.receiveEnergy(budget, false);
            if (accepted > 0) {
                budget -= accepted;
                inventory.setChanged();
            }
        }
    }

    /** 推进（或开始）一个充能配方；原料从存储取、产物写回存储。 */
    private static void runRecipe(PluginContext context, ItemStack pluginStack, ServerPlayer player,
                                  ChargingSettings settings, int ticks) {
        ServerLevel level = player.serverLevel();
        List<RecipeHolder<ChargerChargingRecipe>> recipes = level.getRecipeManager()
            .getAllRecipesFor(ModRecipeTypes.CHARGER_CHARGING_TYPE.get());
        if (recipes.isEmpty()) {
            return;
        }

        if (!settings.activeRecipe().isEmpty()) {
            for (RecipeHolder<ChargerChargingRecipe> holder : recipes) {
                if (!holder.id().toString().equals(settings.activeRecipe())) {
                    continue;
                }
                ChargingPlugin.advance(context, pluginStack, settings, holder, ticks);
                return;
            }
            // 配方已经不存在（数据包改动）：清掉进度
            pluginStack.set(AddonDataComponents.CHARGING_SETTINGS, settings.cleared());
            return;
        }

        for (RecipeHolder<ChargerChargingRecipe> holder : recipes) {
            ChargerChargingRecipe recipe = holder.value();
            if (settings.powerKw() < Math.abs(recipe.getPower())) {
                continue;
            }
            if (context.storage().count(recipe.getIngredient()) <= 0) {
                continue;
            }
            pluginStack.set(
                AddonDataComponents.CHARGING_SETTINGS,
                settings.withProgress(holder.id().toString(), ticks)
            );
            return;
        }
    }

    private static void advance(PluginContext context, ItemStack pluginStack, ChargingSettings settings,
                                RecipeHolder<ChargerChargingRecipe> holder, int ticks) {
        ChargerChargingRecipe recipe = holder.value();
        if (settings.powerKw() < Math.abs(recipe.getPower())) {
            // 档位调低了，跑不动这个配方
            pluginStack.set(AddonDataComponents.CHARGING_SETTINGS, settings.cleared());
            return;
        }
        int progress = settings.progress() + ticks;
        if (progress < recipe.getTime()) {
            pluginStack.set(
                AddonDataComponents.CHARGING_SETTINGS,
                settings.withProgress(settings.activeRecipe(), progress)
            );
            return;
        }

        ItemStack input = context.storage().extractFirst(recipe.getIngredient(), 1);
        if (input.isEmpty()) {
            pluginStack.set(AddonDataComponents.CHARGING_SETTINGS, settings.cleared());
            return;
        }
        ItemStack result = recipe.getResult().copy();
        if (result.isEmpty()) {
            context.insertIntoStorage(input);
            pluginStack.set(AddonDataComponents.CHARGING_SETTINGS, settings.cleared());
            return;
        }
        context.insertIntoStorage(result);
        pluginStack.set(AddonDataComponents.CHARGING_SETTINGS, settings.cleared());
    }
}