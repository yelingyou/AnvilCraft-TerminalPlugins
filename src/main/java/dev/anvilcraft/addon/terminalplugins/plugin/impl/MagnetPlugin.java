/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.plugin.impl;

import dev.anvilcraft.addon.terminalplugins.component.MagnetSettings;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginContext;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginKind;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPlugin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 磁吸 / 拾取插件。
 *
 * <ul>
 *   <li><b>磁吸</b>：把半径内的掉落物逐渐拉向玩家；</li>
 *   <li><b>拾取</b>：把半径内的掉落物直接收进终端连接的存储（先经过过滤插件）。</li>
 * </ul>
 */
public class MagnetPlugin implements TerminalPlugin {
    @Override
    public PluginKind kind() {
        return PluginKind.MAGNET;
    }

    @Override
    public int intervalTicks() {
        return 5;
    }

    @Override
    public void onPlayerTick(PluginContext context) {
        if (context.player() == null) {
            return;
        }
        MagnetSettings settings = context.pluginStack().getOrDefault(
            AddonDataComponents.MAGNET_SETTINGS,
            MagnetSettings.DEFAULT
        );
        if (!settings.magnetEnabled() && !settings.pickupEnabled()) {
            return;
        }
        ServerLevel level = context.player().serverLevel();
        AABB area = context.player().getBoundingBox().inflate(settings.range());
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area);
        Vec3 target = context.player().position().add(0.0, 0.5, 0.0);
        for (ItemEntity entity : items) {
            if (!entity.isAlive()) {
                continue;
            }
            ItemStack stack = entity.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            if (settings.pickupEnabled() && context.storageReachable() && !entity.hasPickUpDelay()) {
                int inserted = context.insertIntoStorage(stack);
                if (inserted >= stack.getCount()) {
                    entity.discard();
                    continue;
                }
                if (inserted > 0) {
                    stack.shrink(inserted);
                    entity.setItem(stack);
                }
            }
            if (settings.magnetEnabled() && !entity.hasPickUpDelay()) {
                Vec3 delta = target.subtract(entity.position());
                double distance = delta.length();
                if (distance > 0.35) {
                    entity.setDeltaMovement(entity.getDeltaMovement().add(delta.normalize().scale(0.12)));
                    entity.hurtMarked = true;
                }
            }
        }
    }
}
