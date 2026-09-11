/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.init;

import dev.anvilcraft.addon.terminalplugins.component.AlchemySettings;
import dev.anvilcraft.addon.terminalplugins.component.AutoCookingSettings;
import dev.anvilcraft.addon.terminalplugins.component.FeedingSettings;
import dev.anvilcraft.addon.terminalplugins.component.MagnetSettings;
import dev.anvilcraft.addon.terminalplugins.item.TerminalPluginItem;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginKind;
import dev.anvilcraft.lib.v2.registrum.util.entry.ItemEntry;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.world.item.Item;

import static dev.anvilcraft.addon.terminalplugins.AnvilCraftTerminalPlugins.REGISTRUM;

public class AddonItems {
    static {
        REGISTRUM.defaultCreativeTab(AddonItemGroups.TERMINAL_PLUGINS.getKey());
    }

    /** 过滤插件：决定哪些物品允许进入终端连接的存储。 */
    public static final ItemEntry<TerminalPluginItem> FILTER_PLUGIN = REGISTRUM
        .item("filter_plugin", properties -> new TerminalPluginItem(PluginKind.FILTER, properties))
        .properties(properties -> properties.component(ModComponents.FILTER_CONTENT, new FilterContent()))
        .lang("Terminal Filter Plugin")
        .register();

    /** 磁吸 / 拾取插件：把附近的掉落物收进存储。 */
    public static final ItemEntry<TerminalPluginItem> MAGNET_PLUGIN = REGISTRUM
        .item("magnet_plugin", properties -> new TerminalPluginItem(PluginKind.MAGNET, properties))
        .properties(properties -> properties.component(AddonDataComponents.MAGNET_SETTINGS, MagnetSettings.DEFAULT))
        .lang("Terminal Magnet Plugin")
        .register();

    /** 自动熔炼 · 烹饪插件。 */
    public static final ItemEntry<TerminalPluginItem> AUTO_COOKING_PLUGIN = REGISTRUM
        .item("auto_cooking_plugin", properties -> new TerminalPluginItem(PluginKind.AUTO_COOKING, properties))
        .properties(properties -> properties.component(
            AddonDataComponents.COOKING_SETTINGS,
            AutoCookingSettings.DEFAULT
        ))
        .lang("Terminal Auto Cooking Plugin")
        .register();

    /** 自动喂食插件。 */
    public static final ItemEntry<TerminalPluginItem> FEEDING_PLUGIN = REGISTRUM
        .item("feeding_plugin", properties -> new TerminalPluginItem(PluginKind.FEEDING, properties))
        .properties(properties -> properties.component(AddonDataComponents.FEEDING_SETTINGS, FeedingSettings.DEFAULT))
        .lang("Terminal Feeding Plugin")
        .register();

    /** 炼金插件：存储内自动酿造。 */
    public static final ItemEntry<TerminalPluginItem> ALCHEMY_PLUGIN = REGISTRUM
        .item("alchemy_plugin", properties -> new TerminalPluginItem(PluginKind.ALCHEMY, properties))
        .properties(properties -> properties.component(AddonDataComponents.ALCHEMY_SETTINGS, AlchemySettings.DEFAULT))
        .lang("Terminal Alchemy Plugin")
        .register();

    public static void register() {
    }
}
