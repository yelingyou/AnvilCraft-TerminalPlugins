/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.plugin;

import dev.anvilcraft.addon.terminalplugins.component.InstalledPlugins;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.item.TerminalPluginItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 插件行为注册表 + 终端插件列表的读写工具。
 */
public final class TerminalPluginRegistry {
    private static final Map<PluginKind, TerminalPlugin> BEHAVIORS = new EnumMap<>(PluginKind.class);

    private TerminalPluginRegistry() {
    }

    public static void register(TerminalPlugin plugin) {
        TerminalPluginRegistry.BEHAVIORS.put(plugin.kind(), plugin);
    }

    public static Optional<TerminalPlugin> behavior(PluginKind kind) {
        return Optional.ofNullable(TerminalPluginRegistry.BEHAVIORS.get(kind));
    }

    public static Optional<TerminalPlugin> behaviorOf(ItemStack pluginStack) {
        if (pluginStack.getItem() instanceof TerminalPluginItem item) {
            return TerminalPluginRegistry.behavior(item.kind());
        }
        return Optional.empty();
    }

    /** 终端上已安装的插件物品列表。 */
    public static List<ItemStack> installed(ItemStack terminalStack) {
        InstalledPlugins installed = terminalStack.get(AddonDataComponents.INSTALLED_PLUGINS);
        return installed == null ? List.of() : installed.plugins();
    }

    public static List<ItemStack> installedOfKind(ItemStack terminalStack, PluginKind kind) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : TerminalPluginRegistry.installed(terminalStack)) {
            if (stack.getItem() instanceof TerminalPluginItem item && item.kind() == kind) {
                result.add(stack);
            }
        }
        return result;
    }

    public static boolean hasPlugin(ItemStack terminalStack, PluginKind kind) {
        return !TerminalPluginRegistry.installedOfKind(terminalStack, kind).isEmpty();
    }

    /** 读取终端上某个插件的配置；未安装时返回 {@code null}。 */
    public static <T> T settings(ItemStack terminalStack, PluginKind kind, DataComponentType<T> type) {
        for (ItemStack pluginStack : TerminalPluginRegistry.installedOfKind(terminalStack, kind)) {
            T value = pluginStack.get(type);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * 过滤判定：终端上装了过滤插件时，按过滤插件的规则决定是否放行；
     * 装了多枚过滤插件时，全部放行才算通过。
     */
    public static boolean passesFilter(ItemStack terminalStack, ItemStack stack) {
        List<ItemStack> filters = TerminalPluginRegistry.installedOfKind(terminalStack, PluginKind.FILTER);
        if (filters.isEmpty()) {
            return true;
        }
        for (ItemStack filterStack : filters) {
            TerminalPlugin plugin = TerminalPluginRegistry.behaviorOf(filterStack).orElse(null);
            if (plugin == null) {
                continue;
            }
            TerminalStorage storage = TerminalStorage.empty();
            PluginContext context = new PluginContext(
                null, terminalStack, filterStack, storage, 0L
            );
            if (!plugin.allowsInsert(context, stack)) {
                return false;
            }
        }
        return true;
    }

    /** 已安装插件的数量（按配置上限校验用）。 */
    public static int installedCount(ItemStack terminalStack) {
        return TerminalPluginRegistry.installed(terminalStack).size();
    }
}
