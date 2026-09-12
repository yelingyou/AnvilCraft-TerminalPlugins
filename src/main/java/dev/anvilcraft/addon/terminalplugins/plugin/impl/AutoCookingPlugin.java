/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.plugin.impl;

import dev.anvilcraft.addon.terminalplugins.AnvilCraftTerminalPlugins;
import dev.anvilcraft.addon.terminalplugins.component.AutoCookingSettings;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginContext;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginKind;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPlugin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.function.Predicate;

/**
 * 自动熔炼 · 烹饪插件：把终端连接存储里的原料按原版烹饪配方自动加工，产物写回存储。
 *
 * <p>支持四种配方类型：熔炼（熔炉）、高炉、烟熏、营火。可选消耗燃料
 * （按原版燃烧时间判定是否为燃料，每批消耗一枚）。</p>
 */
public class AutoCookingPlugin implements TerminalPlugin {
    @Override
    public PluginKind kind() {
        return PluginKind.AUTO_COOKING;
    }

    @Override
    public int intervalTicks() {
        return 10;
    }

    @Override
    public void onPlayerTick(PluginContext context) {
        if (context.player() == null || !context.storageReachable()) {
            return;
        }
        AutoCookingSettings settings = context.pluginStack().getOrDefault(
            AddonDataComponents.COOKING_SETTINGS,
            AutoCookingSettings.DEFAULT
        );
        if (settings.intervalTicks() <= 0 || context.tick() % settings.intervalTicks() != 0) {
            return;
        }
        ServerLevel level = context.player().serverLevel();
        RecipeManager manager = level.getRecipeManager();
        switch (settings.mode()) {
            case SMELTING -> AutoCookingPlugin.process(context, settings, manager, level, RecipeType.SMELTING);
            case BLASTING -> AutoCookingPlugin.process(context, settings, manager, level, RecipeType.BLASTING);
            case SMOKING -> AutoCookingPlugin.process(context, settings, manager, level, RecipeType.SMOKING);
            case CAMPFIRE -> AutoCookingPlugin.process(context, settings, manager, level, RecipeType.CAMPFIRE_COOKING);
        }
    }

    /** 用指定配方类型处理一批物品。 */
    private static <T extends AbstractCookingRecipe> void process(
        PluginContext context,
        AutoCookingSettings settings,
        RecipeManager manager,
        ServerLevel level,
        RecipeType<T> recipeType
    ) {
        Predicate<ItemStack> isCookable = stack -> AutoCookingPlugin.recipeFor(manager, level, recipeType, stack) != null;

        if (settings.consumeFuel() && !AutoCookingPlugin.consumeFuel(context, recipeType)) {
            return;
        }

        int budget = AnvilCraftTerminalPlugins.CONFIG.autoCookingBatchSize;
        for (int processed = 0; processed < budget; processed++) {
            ItemStack input = context.storage().extractFirst(isCookable, 1);
            if (input.isEmpty()) {
                return;
            }
            T recipe = AutoCookingPlugin.recipeFor(manager, level, recipeType, input);
            if (recipe == null) {
                // 理论不可达：把物品放回，避免丢失
                context.insertIntoStorage(input);
                return;
            }
            SingleRecipeInput recipeInput = new SingleRecipeInput(input);
            ItemStack result = recipe.assemble(recipeInput, level.registryAccess());
            ItemStack remaining = input.getCraftingRemainingItem();
            if (!remaining.isEmpty()) {
                context.insertIntoStorage(remaining);
            }
            if (!result.isEmpty()) {
                context.insertIntoStorage(result);
            }
        }
    }

    private static <T extends AbstractCookingRecipe> T recipeFor(
        RecipeManager manager,
        ServerLevel level,
        RecipeType<T> recipeType,
        ItemStack input
    ) {
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        return manager.getRecipeFor(recipeType, recipeInput, level).map(RecipeHolder::value).orElse(null);
    }

    /** 从存储中消耗一枚燃料；存储里没有可燃物时返回 {@code false}。 */
    private static boolean consumeFuel(PluginContext context, RecipeType<?> recipeType) {
        Predicate<ItemStack> isFuel = stack -> stack.getBurnTime(recipeType) > 0;
        ItemStack fuel = context.storage().extractFirst(isFuel, 1);
        return !fuel.isEmpty();
    }
}
