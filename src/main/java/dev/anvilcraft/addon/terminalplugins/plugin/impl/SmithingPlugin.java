/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.plugin.impl;

import dev.anvilcraft.addon.terminalplugins.component.PluginSample;
import dev.anvilcraft.addon.terminalplugins.component.SmithingSettings;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginContext;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginKind;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPlugin;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;

import java.util.List;
import java.util.function.Predicate;

/**
 * 锻造插件：用存储里的模板 / 基底 / 附加物完成锻造台配方（下界合金升级、盔甲纹饰等）。
 *
 * <p>本体存储界面的合成窗口只有工作台与切石机，锻造台没有，所以这不是重复实现。</p>
 *
 * <p><b>消耗型插件的过滤表语义</b>：留空时什么都不做，必须显式把允许当「基底」的物品写进过滤表，
 * 避免把存储里的下界合金锭、模板这类贵重物品悄悄消耗掉。</p>
 *
 * <p><b>皇家锻造台语义</b>：仿照本体的{@code anvilcraft:royal_smithing_table} —— **锻造模板只当作钥匙、不消耗**，
 * 真正消耗的是基底与附加物；只要存储里有对应模板就能一直锻造。</p>
 *
 * <p><b>回滚</b>：基底与附加物是分两次从存储取出的，任何一步失败都会把已取出的部分原样插回。</p>
 */
public class SmithingPlugin implements TerminalPlugin {
    @Override
    public PluginKind kind() {
        return PluginKind.SMITHING;
    }

    @Override
    public int intervalTicks() {
        return 40;
    }

    @Override
    public void onPlayerTick(PluginContext context) {
        ServerPlayer player = context.player();
        if (player == null || !context.storageReachable()) {
            return;
        }
        FilterContent filter = context.pluginStack().get(ModComponents.FILTER_CONTENT);
        if (filter == null) {
            return;
        }
        SmithingSettings settings = context.pluginStack().getOrDefault(
            AddonDataComponents.SMITHING_SETTINGS, SmithingSettings.DEFAULT);
        ServerLevel level = player.serverLevel();
        List<RecipeHolder<SmithingRecipe>> recipes = level.getRecipeManager()
            .getAllRecipesFor(RecipeType.SMITHING);
        if (recipes.isEmpty()) {
            return;
        }

        int done = 0;
        PluginSample sample = context.pluginStack().getOrDefault(
            AddonDataComponents.PLUGIN_SAMPLE, PluginSample.EMPTY);
        for (ItemStack base : context.storage().types()) {
            if (done >= settings.batch()) {
                break;
            }
            if (!filter.filter(base)) {
                continue;
            }
            // 输入槽指定了就只锻造这一种基底
            if (sample.hasSample() && !ItemStack.isSameItemSameComponents(base, sample.sample())) {
                continue;
            }
            for (RecipeHolder<SmithingRecipe> holder : recipes) {
                if (done >= settings.batch()) {
                    break;
                }
                if (SmithingPlugin.craftOnce(context, level, holder.value(), base, sample)) {
                    done++;
                }
            }
        }
    }

    /** 尝试用这个基底完成一次该配方的锻造。 */
    private static boolean craftOnce(PluginContext context, ServerLevel level,
                                     SmithingRecipe recipe, ItemStack base, PluginSample sample) {
        if (!recipe.isBaseIngredient(base)) {
            return false;
        }
        ItemStack template = SmithingPlugin.peek(context, recipe::isTemplateIngredient);
        ItemStack addition = SmithingPlugin.peek(context, recipe::isAdditionIngredient);
        if (!recipe.matches(
            new SmithingRecipeInput(template, base.copyWithCount(1), addition),
            level
        )) {
            return false;
        }

        ItemStack takenBase = context.storage().extractFirst(
            stack -> ItemStack.isSameItemSameComponents(stack, base), 1);
        if (takenBase.isEmpty()) {
            return false;
        }
        ItemStack takenTemplate = ItemStack.EMPTY;
        ItemStack takenAddition = ItemStack.EMPTY;
        if (!template.isEmpty()) {
            // 皇家锻造台的语义：模板只是「钥匙」，不消耗，所以这里只取一个副本来校验配方
            takenTemplate = template.copyWithCount(1);
        }
        if (!addition.isEmpty()) {
            takenAddition = context.storage().extractFirst(
                stack -> ItemStack.isSameItemSameComponents(stack, addition), 1);
            if (takenAddition.isEmpty()) {
                SmithingPlugin.rollback(context, takenBase, takenTemplate);
                return false;
            }
        }

        SmithingRecipeInput input = new SmithingRecipeInput(takenTemplate, takenBase, takenAddition);
        if (!recipe.matches(input, level)) {
            SmithingPlugin.rollback(context, takenBase, takenTemplate, takenAddition);
            return false;
        }
        ItemStack result = recipe.assemble(input, level.registryAccess());
        if (result.isEmpty()) {
            SmithingPlugin.rollback(context, takenBase, takenTemplate, takenAddition);
            return false;
        }
        context.insertIntoStorage(result);
        context.pluginStack().set(AddonDataComponents.PLUGIN_SAMPLE, sample.withOutput(result.copy()));
        return true;
    }

    /** 只读地找一个满足条件的存储物品类型（返回的是数量 1 的展示栈）。 */
    private static ItemStack peek(PluginContext context, Predicate<ItemStack> matcher) {
        for (ItemStack type : context.storage().types()) {
            if (matcher.test(type)) {
                return type;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void rollback(PluginContext context, ItemStack... stacks) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                context.insertIntoStorage(stack);
            }
        }
    }
}