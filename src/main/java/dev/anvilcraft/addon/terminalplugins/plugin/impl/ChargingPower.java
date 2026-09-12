/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for full license text.
 */
package dev.anvilcraft.addon.terminalplugins.plugin.impl;

import dev.anvilcraft.addon.terminalplugins.event.TerminalPluginEvents;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginKind;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPluginManager;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPluginRegistry;
import dev.dubhe.anvilcraft.api.power.DynamicPowerComponent;
import dev.dubhe.anvilcraft.api.power.IDynamicPowerComponentHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 充能插件的「电网用电登记」。
 *
 * <p>做法与本体飘升机背包一致：把需求功率写进玩家 {@link DynamicPowerComponent} 的
 * `getPowerConsumptions()` 集合，电网自己判断够不够（过载时 `PowerGrid#isWorking()` 为 false）。
 * 插件不自己算电网余量，避免和本体的过载逻辑打架。</p>
 *
 * <p>{@link #RESERVED} 记录我们加进去的那一条，改档位时先删旧的再加新的；
 * 插件被拆下 / 关闭后由 {@link #sweep(MinecraftServer)} 定期回收，不会一直占着电网功率。</p>
 */
public final class ChargingPower {
    private static final Map<UUID, Integer> RESERVED = new HashMap<>();

    private ChargingPower() {
    }

    static void reserve(ServerPlayer player, DynamicPowerComponent component, int kw) {
        UUID id = player.getUUID();
        Integer previous = ChargingPower.RESERVED.get(id);
        if (previous != null && previous == kw) {
            return;
        }
        if (previous != null) {
            component.getPowerConsumptions().remove(new DynamicPowerComponent.PowerConsumption(previous));
        }
        component.getPowerConsumptions().add(new DynamicPowerComponent.PowerConsumption(kw));
        ChargingPower.RESERVED.put(id, kw);
    }

    static void release(ServerPlayer player) {
        Integer previous = ChargingPower.RESERVED.remove(player.getUUID());
        if (previous == null) {
            return;
        }
        DynamicPowerComponent component = IDynamicPowerComponentHolder.of(player).anvilcraft$getPowerComponent();
        component.getPowerConsumptions().remove(new DynamicPowerComponent.PowerConsumption(previous));
    }

    /**
     * 回收没人认领的用电登记。
     *
     * <p>覆盖三种情况：插件被拆下、插件被关闭、玩家下线（连同离线玩家在表里的残留一起清掉）。</p>
     */
    public static void sweep(MinecraftServer server) {
        Set<UUID> online = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            online.add(player.getUUID());
            if (ChargingPower.hasActiveChargingPlugin(player)) {
                continue;
            }
            ChargingPower.release(player);
        }
        ChargingPower.RESERVED.keySet().removeIf(id -> !online.contains(id));
    }

    private static boolean hasActiveChargingPlugin(ServerPlayer player) {
        for (ItemStack terminal : TerminalPluginEvents.findInstalledTerminals(player)) {
            for (ItemStack plugin : TerminalPluginRegistry.installedOfKind(terminal, PluginKind.CHARGING)) {
                if (TerminalPluginManager.isEnabled(plugin)) {
                    return true;
                }
            }
        }
        return false;
    }
}