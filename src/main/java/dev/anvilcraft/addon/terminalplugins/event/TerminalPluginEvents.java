/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.event;

import dev.anvilcraft.addon.terminalplugins.AnvilCraftTerminalPlugins;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginContext;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPlugin;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPluginManager;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPluginRegistry;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalStorage;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalStorageResolver;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * 服务端驱动：定期扫描玩家身上的终端，调用已安装插件的行为。
 */
@EventBusSubscriber(modid = AnvilCraftTerminalPlugins.MOD_ID)
public class TerminalPluginEvents {
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int interval = AnvilCraftTerminalPlugins.CONFIG.pluginTickInterval;
        if (interval <= 0 || player.tickCount % interval != 0) {
            return;
        }
        long gameTime = player.level().getGameTime();
        for (ItemStack terminal : TerminalPluginEvents.findInstalledTerminals(player)) {
            List<ItemStack> plugins = TerminalPluginRegistry.installed(terminal);
            if (plugins.isEmpty()) {
                continue;
            }
            TerminalStorage storage = TerminalStorageResolver.resolve(player, terminal);
            for (ItemStack pluginStack : plugins) {
                if (!TerminalPluginManager.isEnabled(pluginStack)) {
                    continue;
                }
                TerminalPlugin plugin = TerminalPluginRegistry.behaviorOf(pluginStack).orElse(null);
                if (plugin == null || plugin.intervalTicks() <= 0) {
                    continue;
                }
                if (gameTime % plugin.intervalTicks() != 0) {
                    continue;
                }
                plugin.onPlayerTick(new PluginContext(player, terminal, pluginStack, storage, gameTime));
            }
        }
    }

    /**
     * 找出玩家身上所有已安装插件的终端。
     *
     * <p>按物品堆栈实例去重：主手 / 副手与背包槽位指向同一个实例，避免同一终端被重复驱动。</p>
     */
    public static List<ItemStack> findInstalledTerminals(ServerPlayer player) {
        Set<ItemStack> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ItemStack> result = new ArrayList<>();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            TerminalPluginEvents.collect(result, seen, inventory.getItem(slot));
        }
        TerminalPluginEvents.collect(result, seen, player.getMainHandItem());
        TerminalPluginEvents.collect(result, seen, player.getOffhandItem());
        return result;
    }

    private static void collect(List<ItemStack> result, Set<ItemStack> seen, ItemStack stack) {
        if (stack.isEmpty() || !seen.add(stack)) {
            return;
        }
        if (!TerminalPluginRegistry.installed(stack).isEmpty()) {
            result.add(stack);
        }
    }
}
