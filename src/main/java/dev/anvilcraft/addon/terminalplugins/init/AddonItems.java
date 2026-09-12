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
import dev.anvilcraft.addon.terminalplugins.component.CompactingSettings;
import dev.anvilcraft.addon.terminalplugins.component.VoidSettings;
import dev.anvilcraft.addon.terminalplugins.component.FeedingSettings;
import dev.anvilcraft.addon.terminalplugins.component.AnvilProcessSettings;
import dev.anvilcraft.addon.terminalplugins.component.ChargingSettings;
import dev.anvilcraft.addon.terminalplugins.component.PluginSample;
import dev.anvilcraft.addon.terminalplugins.component.FluidSettings;
import dev.anvilcraft.addon.terminalplugins.component.MobCatcherSettings;
import dev.anvilcraft.addon.terminalplugins.component.SmithingSettings;
import dev.anvilcraft.addon.terminalplugins.component.ToolSwapSettings;
import dev.anvilcraft.addon.terminalplugins.component.XpPumpSettings;
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

    /** 销毁插件：匹配过滤的物品只保留指定组数。 */
    public static final ItemEntry<TerminalPluginItem> VOID_PLUGIN = REGISTRUM
        .item("void_plugin", properties -> new TerminalPluginItem(PluginKind.VOID, properties))
        .properties(properties -> properties
            .component(ModComponents.FILTER_CONTENT, new FilterContent())
            .component(AddonDataComponents.VOID_SETTINGS, VoidSettings.DEFAULT))
        .lang("Terminal Void Plugin")
        .register();

    /** 压缩插件：9 个同类物品自动压成 1 个。 */
    public static final ItemEntry<TerminalPluginItem> COMPACTING_PLUGIN = REGISTRUM
        .item("compacting_plugin", properties -> new TerminalPluginItem(PluginKind.COMPACTING, properties))
        .properties(properties -> properties
            .component(ModComponents.FILTER_CONTENT, new FilterContent())
            .component(AddonDataComponents.COMPACTING_SETTINGS, CompactingSettings.DEFAULT)
            .component(AddonDataComponents.PLUGIN_SAMPLE, PluginSample.EMPTY))
        .lang("Terminal Compacting Plugin")
        .register();

    /** 流体接口插件：终端自带储液缓冲，与存储里的桶 / 瓶互换流体。 */
    public static final ItemEntry<TerminalPluginItem> FLUID_PLUGIN = REGISTRUM
        .item("fluid_plugin", properties -> new TerminalPluginItem(PluginKind.FLUID, properties))
        .properties(properties -> properties
            .component(ModComponents.FILTER_CONTENT, new FilterContent())
            .component(AddonDataComponents.FLUID_SETTINGS, FluidSettings.DEFAULT))
        .lang("Terminal Fluid Plugin")
        .register();

    /** 工具切换插件：手持工具快坏掉时，从存储换一把同种且耐久更好的。 */
    public static final ItemEntry<TerminalPluginItem> TOOL_SWAP_PLUGIN = REGISTRUM
        .item("tool_swap_plugin", properties -> new TerminalPluginItem(PluginKind.TOOL_SWAP, properties))
        .properties(properties -> properties
            .component(ModComponents.FILTER_CONTENT, new FilterContent())
            .component(AddonDataComponents.TOOL_SWAP_SETTINGS, ToolSwapSettings.DEFAULT))
        .lang("Terminal Tool Swap Plugin")
        .register();

    /** 生物捕捉插件：用存储里的空树脂块自动捕捉附近生物。 */
    public static final ItemEntry<TerminalPluginItem> MOB_CATCHER_PLUGIN = REGISTRUM
        .item("mob_catcher_plugin", properties -> new TerminalPluginItem(PluginKind.MOB_CATCHER, properties))
        .properties(properties -> properties.component(
            AddonDataComponents.MOB_CATCHER_SETTINGS,
            MobCatcherSettings.DEFAULT
        ))
        .lang("Terminal Mob Catcher Plugin")
        .register();

    /** 经验泵插件：玩家经验与存储里的经验宝石互转。 */
    public static final ItemEntry<TerminalPluginItem> XP_PUMP_PLUGIN = REGISTRUM
        .item("xp_pump_plugin", properties -> new TerminalPluginItem(PluginKind.XP_PUMP, properties))
        .properties(properties -> properties.component(
            AddonDataComponents.XP_PUMP_SETTINGS,
            XpPumpSettings.DEFAULT
        ))
        .lang("Terminal XP Pump Plugin")
        .register();

    /** 锻造插件：用存储里的模板 / 基底 / 附加物完成锻造台配方。 */
    public static final ItemEntry<TerminalPluginItem> SMITHING_PLUGIN = REGISTRUM
        .item("smithing_plugin", properties -> new TerminalPluginItem(PluginKind.SMITHING, properties))
        .properties(properties -> properties
            .component(ModComponents.FILTER_CONTENT, new FilterContent())
            .component(AddonDataComponents.SMITHING_SETTINGS, SmithingSettings.DEFAULT)
            .component(AddonDataComponents.PLUGIN_SAMPLE, PluginSample.EMPTY))
        .lang("Terminal Smithing Plugin")
        .register();

    /** 充能插件：接入电网，跑本体充能配方并给 FE 物品充电。 */
    public static final ItemEntry<TerminalPluginItem> CHARGING_PLUGIN = REGISTRUM
        .item("charging_plugin", properties -> new TerminalPluginItem(PluginKind.CHARGING, properties))
        .properties(properties -> properties
            .component(AddonDataComponents.CHARGING_SETTINGS, ChargingSettings.DEFAULT)
            .component(AddonDataComponents.PLUGIN_SAMPLE, PluginSample.EMPTY))
        .register();

    /** 铁砧加工插件：在界面里手动批量执行本体的铁砧加工方式。 */
    public static final ItemEntry<TerminalPluginItem> ANVIL_PROCESS_PLUGIN = REGISTRUM
        .item("anvil_process_plugin", properties -> new TerminalPluginItem(PluginKind.ANVIL_PROCESS, properties))
        .properties(properties -> properties
            .component(ModComponents.FILTER_CONTENT, new FilterContent())
            .component(AddonDataComponents.ANVIL_PROCESS_SETTINGS, AnvilProcessSettings.DEFAULT)
            .component(AddonDataComponents.PLUGIN_SAMPLE, PluginSample.EMPTY))
        .lang("Terminal Anvil Process Plugin")
        .register();

    public static void register() {
    }
}
