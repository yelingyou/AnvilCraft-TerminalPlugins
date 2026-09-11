/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.plugin.impl;

import dev.anvilcraft.addon.terminalplugins.component.FeedingSettings;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginContext;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginKind;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPlugin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * 自动喂食插件：玩家饥饿时自动从终端连接的存储中取出食物进食。
 *
 * <p>默认拒绝带负面效果的食物（腐肉、河豚、蜘蛛眼等）；碗一类的容器物品会被放回存储。</p>
 */
public class FeedingPlugin implements TerminalPlugin {
    @Override
    public PluginKind kind() {
        return PluginKind.FEEDING;
    }

    @Override
    public int intervalTicks() {
        return 20;
    }

    @Override
    public void onPlayerTick(PluginContext context) {
        ServerPlayer player = context.player();
        if (player == null || !context.storageReachable() || !player.isAlive()) {
            return;
        }
        FeedingSettings settings = context.pluginStack().getOrDefault(
            AddonDataComponents.FEEDING_SETTINGS,
            FeedingSettings.DEFAULT
        );
        FoodData foodData = player.getFoodData();
        if (foodData.getFoodLevel() > settings.hungerThreshold()) {
            return;
        }
        if (settings.keepSaturation() && foodData.getSaturationLevel() > 0.0F) {
            return;
        }
        Predicate<ItemStack> edible = stack -> {
            FoodProperties properties = stack.get(DataComponents.FOOD);
            if (properties == null) {
                return false;
            }
            return settings.allowHarmful() || !FeedingPlugin.isHarmful(properties);
        };
        ItemStack food = context.storage().extractFirst(edible, 1);
        if (food.isEmpty()) {
            return;
        }
        FoodProperties properties = food.get(DataComponents.FOOD);
        if (properties == null) {
            context.insertIntoStorage(food);
            return;
        }
        foodData.eat(properties);
        for (FoodProperties.PossibleEffect possible : properties.effects()) {
            if (player.getRandom().nextFloat() < possible.probability()) {
                player.addEffect(new MobEffectInstance(possible.effect()));
            }
        }
        properties.usingConvertsTo().ifPresent(container -> context.insertIntoStorage(container.copy()));
        player.containerMenu.broadcastChanges();
    }

    /** 食物是否含有负面效果。 */
    public static boolean isHarmful(FoodProperties properties) {
        for (FoodProperties.PossibleEffect possible : properties.effects()) {
            MobEffectInstance instance = possible.effect();
            if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                return true;
            }
        }
        return false;
    }
}
