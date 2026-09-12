/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for full license text.
 */
package dev.anvilcraft.addon.terminalplugins.plugin.impl;

import dev.anvilcraft.addon.terminalplugins.component.AnvilProcessSettings;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.network.PluginActionPacket;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginContext;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginKind;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPlugin;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * 铁砧加工插件：在界面里**手动批量**执行本体的铁砧加工方式。
 *
 * <p>这些加工在原版铁砧工艺里要靠铁砧砸方块、或者在多方块结构里完成；本插件把它变成
 * 「选加工方式 + 选批量 + 点开始加工」的一步操作。**只由按钮触发**，不做任何自动轮询。</p>
 *
 * <p>配方判定完全交给本体的 {@link AbstractProcessRecipe}：
 * 输入用 {@code getInputItems()}（同时也是过滤表筛选项），输出用 {@code getResultItems()} 的
 * {@code getResult(ServerLevel)} 掷出实际数量，概率产出也一并保留。</p>
 *
 * <p>需要方块（{@code getInputBlocks()} 非空）或需要炼药锅流体 / 点燃的配方会被跳过 ——
 * 那些在存储物品的语境下没法诚实还原。</p>
 */
public class AnvilProcessPlugin implements TerminalPlugin {
    @Override
    public PluginKind kind() {
        return PluginKind.ANVIL_PROCESS;
    }

    /** 只由面板按钮触发：返回 0 让派发器直接跳过，不做任何周期行为。 */
    @Override
    public int intervalTicks() {
        return 0;
    }

    @Override
    public boolean onAction(PluginContext context, int action) {
        if (action != PluginActionPacket.ANVIL_PROCESS_NOW || context.player() == null) {
            return false;
        }
        if (!context.storageReachable()) {
            return false;
        }
        AnvilProcessSettings settings = context.pluginStack().getOrDefault(
            AddonDataComponents.ANVIL_PROCESS_SETTINGS, AnvilProcessSettings.DEFAULT);
        FilterContent filter = context.pluginStack().get(ModComponents.FILTER_CONTENT);
        if (filter == null) {
            return false;
        }
        ServerLevel level = context.player().serverLevel();
        List<RecipeHolder<AbstractProcessRecipe<?>>> recipes = AnvilProcessPlugin.recipes(level, settings.process());
        if (recipes.isEmpty()) {
            return false;
        }

        int done = 0;
        for (int attempt = 0; attempt < settings.batch(); attempt++) {
            boolean crafted = false;
            for (RecipeHolder<AbstractProcessRecipe<?>> holder : recipes) {
                if (AnvilProcessPlugin.craftOnce(context, level, holder.value(), filter)) {
                    crafted = true;
                    break;
                }
            }
            if (!crafted) {
                break;
            }
            done++;
        }
        return done > 0;
    }

    /** 取某个加工方式对应的配方表。加工方式的 RecipeType 泛型各不相同，所以这里统一擦除成 AbstractProcessRecipe。 */
    @SuppressWarnings("unchecked")
    private static List<RecipeHolder<AbstractProcessRecipe<?>>> recipes(ServerLevel level,
                                                                        AnvilProcessSettings.AnvilProcess process) {
        RecipeType<?> type = AnvilProcessPlugin.typeOf(process).get();
        return (List<RecipeHolder<AbstractProcessRecipe<?>>>) (List<?>) level.getRecipeManager()
            .getAllRecipesFor((RecipeType) type);
    }

    private static DeferredHolder<RecipeType<?>, ?> typeOf(AnvilProcessSettings.AnvilProcess process) {
        return switch (process) {
            case STAMPING -> ModRecipeTypes.STAMPING_TYPE;
            case CRUSH -> ModRecipeTypes.ITEM_CRUSH_TYPE;
            case COMPRESS -> ModRecipeTypes.ITEM_COMPRESS_TYPE;
            case UNPACK -> ModRecipeTypes.UNPACK_TYPE;
            case MESH -> ModRecipeTypes.MESH_TYPE;
            case SUPER_HEATING -> ModRecipeTypes.SUPER_HEATING_TYPE;
            case TIME_WARP -> ModRecipeTypes.TIME_WARP_TYPE;
            case ITEM_INJECT -> ModRecipeTypes.ITEM_INJECT_TYPE;
            case NEUTRON_IRRADIATION -> ModRecipeTypes.NEUTRON_IRRADIATION_TYPE;
        };
    }

    /** 尝试用存储里的物品完成一次该配方；不满足条件就原样返回 false，不动存储。 */
    private static boolean craftOnce(PluginContext context, ServerLevel level,
                                     AbstractProcessRecipe<?> recipe, FilterContent filter) {
        // 需要炼药锅流体的配方跳过：存储物品的语境下没法诚实还原
        // （需要特定方块的配方不再跳过 —— 本插件本来就是这些加工方式的「快捷化」）
        if (recipe.getHasCauldron() != null
            && (recipe.getHasCauldron().hasFluid() || recipe.getHasCauldron().ignited())) {
            return false;
        }
        List<ItemIngredientPredicate> inputs = recipe.getInputItems();
        List<ChanceItemStack> results = recipe.getResultItems();
        if (inputs.isEmpty() || results.isEmpty()) {
            return false;
        }
        // 过滤表可选：留空 = 不限制（点了按钮才加工，玩家是主动触发的）
        for (ItemIngredientPredicate input : inputs) {
            if (context.storage().count(
                stack -> input.test(stack) && AnvilProcessPlugin.allowed(filter, stack)
            ) < input.count()) {
                return false;
            }
        }

        // 真正取出：任何一步失败都原样放回
        List<ItemStack> taken = new ArrayList<>();
        for (ItemIngredientPredicate input : inputs) {
            ItemStack stack = context.storage().extractFirst(
                candidate -> input.test(candidate) && AnvilProcessPlugin.allowed(filter, candidate),
                input.count()
            );
            if (stack.isEmpty() || stack.getCount() < input.count()) {
                if (!stack.isEmpty()) {
                    taken.add(stack);
                }
                AnvilProcessPlugin.rollback(context, taken);
                return false;
            }
            taken.add(stack);
        }

        List<ItemStack> produced = new ArrayList<>();
        for (ChanceItemStack result : results) {
            ItemStack stack = result.getResult(level);
            if (!stack.isEmpty()) {
                produced.add(stack);
            }
        }
        if (produced.isEmpty()) {
            AnvilProcessPlugin.rollback(context, taken);
            return false;
        }
        for (ItemStack stack : produced) {
            context.insertIntoStorage(stack);
        }
        return true;
    }

    /** 过滤表留空（或全是空格子）= 不限制；填了内容就只允许表里的物品。 */
    private static boolean allowed(FilterContent filter, ItemStack stack) {
        if (filter == null || filter.list().stream().allMatch(ItemStack::isEmpty)) {
            return true;
        }
        return filter.filter(stack);
    }

    private static void rollback(PluginContext context, List<ItemStack> taken) {
        for (ItemStack stack : taken) {
            context.insertIntoStorage(stack);
        }
    }
}