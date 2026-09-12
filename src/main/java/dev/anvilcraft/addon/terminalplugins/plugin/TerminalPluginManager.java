/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.plugin;

import dev.anvilcraft.addon.terminalplugins.AnvilCraftTerminalPlugins;
import dev.anvilcraft.addon.terminalplugins.component.AlchemySettings;
import dev.anvilcraft.addon.terminalplugins.component.InstalledPlugins;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.item.TerminalPluginItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;

/**
 * 终端插件的读写操作：安装 / 拆卸 / 排序 / 开关 / 改配置。
 *
 * <p>安装台界面与「终端内插件面板」都走这里，保证两条路径行为一致。</p>
 */
public final class TerminalPluginManager {
    private TerminalPluginManager() {
    }

    public static List<ItemStack> installed(ItemStack terminal) {
        InstalledPlugins installed = terminal.get(AddonDataComponents.INSTALLED_PLUGINS);
        return installed == null ? List.of() : installed.plugins();
    }

    public static boolean isTerminal(ItemStack stack) {
        return stack.getItem() instanceof dev.dubhe.anvilcraft.item.TerminalItem;
    }

    /** 安装一枚插件；超出上限时返回 false。 */
    public static boolean install(ItemStack terminal, ItemStack plugin) {
        if (plugin.isEmpty() || !(plugin.getItem() instanceof TerminalPluginItem)) {
            return false;
        }
        InstalledPlugins installed = terminal.getOrDefault(AddonDataComponents.INSTALLED_PLUGINS, InstalledPlugins.EMPTY);
        int max = Math.min(AnvilCraftTerminalPlugins.CONFIG.maxPluginsPerTerminal, InstalledPlugins.MAX_SLOTS);
        if (installed.size() >= max) {
            return false;
        }
        terminal.set(AddonDataComponents.INSTALLED_PLUGINS, installed.with(plugin));
        return true;
    }

    /** 卸下第 index 枚插件并返回它（无则返回空栈）。 */
    public static ItemStack remove(ItemStack terminal, int index) {
        InstalledPlugins installed = terminal.getOrDefault(AddonDataComponents.INSTALLED_PLUGINS, InstalledPlugins.EMPTY);
        if (index < 0 || index >= installed.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = installed.plugins().get(index).copyWithCount(1);
        terminal.set(AddonDataComponents.INSTALLED_PLUGINS, installed.without(index));
        return removed;
    }

    public static boolean move(ItemStack terminal, int index, int delta) {
        InstalledPlugins installed = terminal.getOrDefault(AddonDataComponents.INSTALLED_PLUGINS, InstalledPlugins.EMPTY);
        int target = index + delta;
        if (index < 0 || index >= installed.size() || target < 0 || target >= installed.size()) {
            return false;
        }
        List<ItemStack> list = new ArrayList<>(installed.plugins());
        ItemStack moved = list.remove(index);
        list.add(target, moved);
        terminal.set(AddonDataComponents.INSTALLED_PLUGINS, new InstalledPlugins(list));
        return true;
    }

    /** 通用更新：把第 index 枚插件交给 operator 修改后写回终端（新插件一律走这里）。 */
    public static boolean update(ItemStack terminal, int index, UnaryOperator<ItemStack> operator) {
        InstalledPlugins installed = terminal.getOrDefault(AddonDataComponents.INSTALLED_PLUGINS, InstalledPlugins.EMPTY);
        if (index < 0 || index >= installed.size()) {
            return false;
        }
        ItemStack plugin = installed.plugins().get(index).copy();
        ItemStack updated = operator.apply(plugin);
        terminal.set(AddonDataComponents.INSTALLED_PLUGINS, installed.replaced(index, updated));
        return true;
    }

    /** 打开 / 关闭某枚插件。 */
    public static boolean toggleEnabled(ItemStack terminal, int index) {
        InstalledPlugins installed = terminal.getOrDefault(AddonDataComponents.INSTALLED_PLUGINS, InstalledPlugins.EMPTY);
        if (index < 0 || index >= installed.size()) {
            return false;
        }
        ItemStack plugin = installed.plugins().get(index).copy();
        plugin.set(AddonDataComponents.PLUGIN_ENABLED, !TerminalPluginManager.isEnabled(plugin));
        terminal.set(AddonDataComponents.INSTALLED_PLUGINS, installed.replaced(index, plugin));
        return true;
    }

    public static boolean isEnabled(ItemStack pluginStack) {
        return pluginStack.getOrDefault(AddonDataComponents.PLUGIN_ENABLED, Boolean.TRUE);
    }

    /** 切换某枚插件的主 / 副档位。 */
    public static boolean cycleSetting(ItemStack terminal, int index, boolean secondary) {
        InstalledPlugins installed = terminal.getOrDefault(AddonDataComponents.INSTALLED_PLUGINS, InstalledPlugins.EMPTY);
        if (index < 0 || index >= installed.size()) {
            return false;
        }
        ItemStack plugin = installed.plugins().get(index).copy();
        TerminalPluginItem.cycleSetting(plugin, secondary);
        terminal.set(AddonDataComponents.INSTALLED_PLUGINS, installed.replaced(index, plugin));
        return true;
    }

    @Nullable
    public static AlchemySettings alchemySettings(ItemStack terminal, int index) {
        InstalledPlugins installed = terminal.getOrDefault(AddonDataComponents.INSTALLED_PLUGINS, InstalledPlugins.EMPTY);
        if (index < 0 || index >= installed.size()) {
            return null;
        }
        ItemStack plugin = installed.plugins().get(index);
        if (!(plugin.getItem() instanceof TerminalPluginItem item) || item.kind() != PluginKind.ALCHEMY) {
            return null;
        }
        return plugin.getOrDefault(AddonDataComponents.ALCHEMY_SETTINGS, AlchemySettings.DEFAULT);
    }

    /** 修改炼金插件的配置（条目 / 附近目标等）。 */
    public static boolean updateAlchemy(ItemStack terminal, int index, UnaryOperator<AlchemySettings> operator) {
        InstalledPlugins installed = terminal.getOrDefault(AddonDataComponents.INSTALLED_PLUGINS, InstalledPlugins.EMPTY);
        if (index < 0 || index >= installed.size()) {
            return false;
        }
        ItemStack plugin = installed.plugins().get(index).copy();
        if (!(plugin.getItem() instanceof TerminalPluginItem item) || item.kind() != PluginKind.ALCHEMY) {
            return false;
        }
        AlchemySettings current = plugin.getOrDefault(AddonDataComponents.ALCHEMY_SETTINGS, AlchemySettings.DEFAULT);
        plugin.set(AddonDataComponents.ALCHEMY_SETTINGS, operator.apply(current));
        terminal.set(AddonDataComponents.INSTALLED_PLUGINS, installed.replaced(index, plugin));
        return true;
    }
}
