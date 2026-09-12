/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.plugin.impl;

import dev.anvilcraft.addon.terminalplugins.component.MobCatcherSettings;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginContext;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginKind;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPlugin;
import dev.dubhe.anvilcraft.block.item.HasMobBlockItem;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;

/**
 * 生物捕捉插件：把存储里的空树脂块变成「装着生物的树脂块」。
 *
 * <p>本体的树脂块已经能手动捕捉（手持右键生物），本插件不重复实现规则，而是直接调用
 * {@link HasMobBlockItem#canMobBeSaved(Mob, net.minecraft.world.entity.player.Player, ItemStack)} 与
 * {@link HasMobBlockItem#saveMobInItem(net.minecraft.world.level.Level, Mob, net.minecraft.world.entity.player.Player, ItemStack)}，
 * 因此「碰撞箱过大不能抓」「敌对 / 中立生物需要虚弱」这些判定与本体完全一致。</p>
 *
 * <p>注意 {@code player} 参数传 {@code null}：本体的实现会在 player 非空时把抓到的树脂块直接塞进玩家背包，
 * 而我们要的是「收回终端连接的存储」，所以传 null 让它把结果返回给我们。</p>
 */
public class MobCatcherPlugin implements TerminalPlugin {
    @Override
    public PluginKind kind() {
        return PluginKind.MOB_CATCHER;
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
        MobCatcherSettings settings = context.pluginStack().getOrDefault(
            AddonDataComponents.MOB_CATCHER_SETTINGS, MobCatcherSettings.DEFAULT);

        List<Mob> candidates = player.level().getEntitiesOfClass(
            Mob.class,
            player.getBoundingBox().inflate(settings.radius()),
            mob -> mob.isAlive() && !mob.isRemoved() && MobCatcherPlugin.selectable(mob, settings)
        );
        if (candidates.isEmpty()) {
            return;
        }
        candidates.sort(Comparator.comparingDouble(player::distanceToSqr));

        ItemStack resin = context.storage().extractFirst(
            stack -> stack.is(ModBlocks.RESIN_BLOCK.asItem()) && !HasMobBlockItem.hasMob(stack),
            1
        );
        if (resin.isEmpty()) {
            return;
        }

        for (Mob mob : candidates) {
            if (!HasMobBlockItem.canMobBeSaved(mob, null, resin.copy())) {
                continue;
            }
            ItemStack captured = HasMobBlockItem.saveMobInItem(player.level(), mob, null, resin.copy());
            if (captured.isEmpty() || !HasMobBlockItem.hasMob(captured)) {
                continue;
            }
            context.insertIntoStorage(captured);
            return;
        }
        // 没有抓成功，把树脂块原样放回存储
        context.insertIntoStorage(resin);
    }

    /** 是否纳入候选：默认只碰被动生物，开启后连敌对 / 中立一起试（能否成功仍由本体判定）。 */
    private static boolean selectable(Mob mob, MobCatcherSettings settings) {
        return settings.allowHostile() || !(mob instanceof Enemy);
    }
}