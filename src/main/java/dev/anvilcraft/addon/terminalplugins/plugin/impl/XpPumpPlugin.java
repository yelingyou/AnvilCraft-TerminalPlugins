/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.plugin.impl;

import dev.anvilcraft.addon.terminalplugins.component.XpPumpSettings;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginContext;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginKind;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPlugin;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 经验泵插件：在玩家经验与存储里的**经验宝石**之间搬运经验。
 *
 * <p>存入：等级高于阈值且身上经验够 50 点时，扣 50 点经验并把一颗经验宝石放进存储；
 * 取出：等级低于阈值时从存储取一颗经验宝石，换成 50 点经验。</p>
 *
 * <p>写入存储会经过终端自己的过滤插件（{@link PluginContext#insertIntoStorage}），
 * 如果宝石被过滤插件拦下，扣掉的经验会**原样退回**，不会凭空消失。</p>
 */
public class XpPumpPlugin implements TerminalPlugin {
    @Override
    public PluginKind kind() {
        return PluginKind.XP_PUMP;
    }

    @Override
    public int intervalTicks() {
        return 20;
    }

    @Override
    public void onPlayerTick(PluginContext context) {
        ServerPlayer player = context.player();
        if (player == null || !context.storageReachable()) {
            return;
        }
        XpPumpSettings settings = context.pluginStack().getOrDefault(
            AddonDataComponents.XP_PUMP_SETTINGS, XpPumpSettings.DEFAULT);

        if (settings.mode() == XpPumpSettings.XpPumpMode.STORE) {
            XpPumpPlugin.store(context, player, settings);
        } else if (settings.mode() == XpPumpSettings.XpPumpMode.WITHDRAW) {
            XpPumpPlugin.withdraw(context, player, settings);
        }
    }

    private static void store(PluginContext context, ServerPlayer player, XpPumpSettings settings) {
        for (int index = 0; index < settings.gemsPerCycle(); index++) {
            if (player.experienceLevel < settings.storeLevel()) {
                return;
            }
            if (player.totalExperience < XpPumpSettings.XP_PER_GEM) {
                return;
            }
            player.giveExperiencePoints(-XpPumpSettings.XP_PER_GEM);
            int inserted = context.insertIntoStorage(new ItemStack(ModItems.EXP_GEM.get()));
            if (inserted <= 0) {
                // 存储写不进去（例如被过滤插件拦下），把经验退回，避免经验凭空消失
                player.giveExperiencePoints(XpPumpSettings.XP_PER_GEM);
                return;
            }
        }
    }

    private static void withdraw(PluginContext context, ServerPlayer player, XpPumpSettings settings) {
        for (int index = 0; index < settings.gemsPerCycle(); index++) {
            if (player.experienceLevel > settings.keepLevel()) {
                return;
            }
            ItemStack gem = context.storage().extractFirst(
                stack -> stack.is(ModItems.EXP_GEM.get()),
                settings.gemsPerCycle()
            );
            if (gem.isEmpty()) {
                return;
            }
            player.giveExperiencePoints(XpPumpSettings.XP_PER_GEM * gem.getCount());
            return;
        }
    }
}