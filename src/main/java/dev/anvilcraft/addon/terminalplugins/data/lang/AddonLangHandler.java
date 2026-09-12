/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.data.lang;

import dev.anvilcraft.addon.terminalplugins.TerminalPluginsConfig;
import dev.anvilcraft.lib.v2.config.ConfigData;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

import java.util.LinkedHashMap;
import java.util.Map;

public class AddonLangHandler {
    /** 物品名 / 方块名 / 配置项由 Registrum 自动生成，这里只补交互文本与提示。 */
    private static final Map<String, String> EXTRA = new LinkedHashMap<>();

    static {
        AddonLangHandler.extra("container.anvilcraft_terminal_plugins.plugin_station", "Terminal Plugin Station");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.uninstall_all", "Uninstall All");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.install_all", "Install all");
AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.station.terminal", "Terminal");
AddonLangHandler.extra(
    "screen.anvilcraft_terminal_plugins.station.installed",
    "%s / %s plugins installed"
);
AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.station.staging_slots", "Staging");
AddonLangHandler.extra(
    "screen.anvilcraft_terminal_plugins.station.install_tip",
    "Install the staged plugins into the terminal (staging never installs by itself)"
);
AddonLangHandler.extra(
    "screen.anvilcraft_terminal_plugins.station.uninstall_tip",
    "Move every installed plugin back into the staging slots"
);
AddonLangHandler.extra(
    "screen.anvilcraft_terminal_plugins.station.panel_tip",
    "Open the plugin panel to inspect and configure installed plugins"
);
        AddonLangHandler.extra("message.anvilcraft_terminal_plugins.setting", "%s → %s");
        AddonLangHandler.extra("message.anvilcraft_terminal_plugins.no_terminal", "No terminal found on you");
        AddonLangHandler.extra("message.anvilcraft_terminal_plugins.stale_plugin", "That plugin is no longer installed");

        AddonLangHandler.extra("plugin.anvilcraft_terminal_plugins.filter_plugin", "Filter Plugin");
        AddonLangHandler.extra(
            "plugin.anvilcraft_terminal_plugins.filter_plugin.desc",
            "Controls which items may enter the bound storage"
        );
        AddonLangHandler.extra("plugin.anvilcraft_terminal_plugins.magnet_plugin", "Magnet / Pickup Plugin");
        AddonLangHandler.extra(
            "plugin.anvilcraft_terminal_plugins.magnet_plugin.desc",
            "Pulls nearby drops in and stores them directly"
        );
        AddonLangHandler.extra("plugin.anvilcraft_terminal_plugins.auto_cooking_plugin", "Auto Cooking Plugin");
        AddonLangHandler.extra(
            "plugin.anvilcraft_terminal_plugins.auto_cooking_plugin.desc",
            "Smelts and cooks stored ingredients automatically"
        );
        AddonLangHandler.extra("plugin.anvilcraft_terminal_plugins.feeding_plugin", "Feeding Plugin");
        AddonLangHandler.extra(
            "plugin.anvilcraft_terminal_plugins.feeding_plugin.desc",
            "Feeds the owner from the bound storage when hungry"
        );
        AddonLangHandler.extra("plugin.anvilcraft_terminal_plugins.void_plugin", "Void Plugin");
        AddonLangHandler.extra(
            "plugin.anvilcraft_terminal_plugins.void_plugin.desc",
            "Destroys matching items in the storage beyond the kept amount"
        );
        AddonLangHandler.extra("plugin.anvilcraft_terminal_plugins.fluid_plugin", "Fluid Plugin");
        AddonLangHandler.extra(
            "plugin.anvilcraft_terminal_plugins.fluid_plugin.desc",
            "Moves fluids between containers in the storage and the terminal's built-in buffer"
        );
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.setting.fluid_mode", "Mode: %s");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.setting.fluid_batch", "Batch per cycle: %s");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.setting.fluid_capacity", "Buffer: %s buckets");
        AddonLangHandler.extra("plugin.anvilcraft_terminal_plugins.charging_plugin", "Charging Plugin");
        AddonLangHandler.extra(
            "plugin.anvilcraft_terminal_plugins.charging_plugin.desc",
            "Draws power from an AnvilCraft grid: runs charging recipes and charges FE items you carry"
        );
        AddonLangHandler.extra(
            "screen.anvilcraft_terminal_plugins.setting.charging_power",
            "Grid draw: %s kW"
        );
        AddonLangHandler.extra(
            "screen.anvilcraft_terminal_plugins.setting.charging_recipes",
            "Charging recipes: %s"
        );
        AddonLangHandler.extra(
            "screen.anvilcraft_terminal_plugins.setting.charging_items",
            "Charge FE items: %s"
        );
        AddonLangHandler.extra(
            "screen.anvilcraft_terminal_plugins.panel.charging_power_tip",
            "Click to change the grid draw; recipes needing more power will not run"
        );
        AddonLangHandler.extra(
            "screen.anvilcraft_terminal_plugins.panel.charging_recipes_tip",
            "On: run the mod's charging recipes with materials from the storage"
        );
        AddonLangHandler.extra(
            "screen.anvilcraft_terminal_plugins.panel.charging_items_tip",
            "On: convert grid power into FE and charge the FE items you carry"
        );
        AddonLangHandler.extra(
            "screen.anvilcraft_terminal_plugins.panel.charging_idle",
            "No charging recipe in progress"
        );
        AddonLangHandler.extra(
            "screen.anvilcraft_terminal_plugins.panel.charging_progress",
            "Charging %s (%s ticks)"
        );
        AddonLangHandler.extra("plugin.anvilcraft_terminal_plugins.anvil_process_plugin", "Anvil Process Plugin");
        AddonLangHandler.extra(
            "plugin.anvilcraft_terminal_plugins.anvil_process_plugin.desc",
            "Runs the mod's anvil processing methods by hand, in batches, from the panel"
        );
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.setting.process", "Process: %s");
        AddonLangHandler.extra(
            "screen.anvilcraft_terminal_plugins.setting.process_batch",
            "Runs this time: %s"
        );
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.setting.process_now", "Process now");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.anvil_process.stamping", "Stamping");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.anvil_process.crush", "Crushing");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.anvil_process.compress", "Compressing");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.anvil_process.unpack", "Unpacking");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.anvil_process.mesh", "Sifting");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.anvil_process.super_heating", "Super heating");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.anvil_process.time_warp", "Time warp");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.anvil_process.item_inject", "Mass inject");
        AddonLangHandler.extra(
            "screen.anvilcraft_terminal_plugins.anvil_process.neutron_irradiation",
            "Neutron irradiation"
        );
        AddonLangHandler.extra(
            "screen.anvilcraft_terminal_plugins.panel.process_tip",
            "Click to change the anvil processing method"
        );
        AddonLangHandler.extra(
            "screen.anvilcraft_terminal_plugins.panel.process_now_tip",
            "Only runs when clicked: uses materials from the storage"
        );
        AddonLangHandler.extra(
            "message.anvilcraft_terminal_plugins.nothing_to_process",
            "Nothing could be processed (check the filter table and the process)"
        );
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.fluid_mode.off", "Off");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.fluid_mode.fill_buffer", "Fill buffer");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.fluid_mode.empty_buffer", "Fill containers");
        AddonLangHandler.extra("plugin.anvilcraft_terminal_plugins.tool_swap_plugin", "Tool Swap Plugin");
        AddonLangHandler.extra(
            "plugin.anvilcraft_terminal_plugins.tool_swap_plugin.desc",
            "Swaps in a less worn copy of the held tool taken from the storage"
        );
        AddonLangHandler.extra(
            "screen.anvilcraft_terminal_plugins.setting.tool_swap_threshold",
            "Swap below %s%% durability"
        );
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.setting.tool_swap_target", "Slots: %s");
        AddonLangHandler.extra(
            "screen.anvilcraft_terminal_plugins.setting.tool_swap_return_worn",
            "Store worn tool: %s"
        );
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.swap_target.main_hand", "Main hand only");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.swap_target.all_hotbar", "Whole hotbar");
        AddonLangHandler.extra("plugin.anvilcraft_terminal_plugins.mob_catcher_plugin", "Mob Catcher Plugin");
        AddonLangHandler.extra(
            "plugin.anvilcraft_terminal_plugins.mob_catcher_plugin.desc",
            "Catches nearby mobs into empty resin blocks taken from the storage"
        );
        AddonLangHandler.extra(
            "screen.anvilcraft_terminal_plugins.setting.mob_catcher_hostile",
            "Hostile and neutral mobs: %s"
        );
        AddonLangHandler.extra(
            "screen.anvilcraft_terminal_plugins.panel.mob_catcher_tip",
            "On: also try hostile/neutral mobs, which still need Weakness to be caught"
        );
        AddonLangHandler.extra("plugin.anvilcraft_terminal_plugins.xp_pump_plugin", "XP Pump Plugin");
        AddonLangHandler.extra(
            "plugin.anvilcraft_terminal_plugins.xp_pump_plugin.desc",
            "Moves experience between you and exp gems kept in the storage"
        );
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.setting.xp_mode", "Mode: %s");
        AddonLangHandler.extra(
            "screen.anvilcraft_terminal_plugins.setting.xp_store_level",
            "Store above level %s"
        );
        AddonLangHandler.extra(
            "screen.anvilcraft_terminal_plugins.setting.xp_keep_level",
            "Withdraw below level %s"
        );
        AddonLangHandler.extra(
            "screen.anvilcraft_terminal_plugins.setting.xp_gems_per_cycle",
            "Gems per cycle: %s"
        );
        AddonLangHandler.extra("plugin.anvilcraft_terminal_plugins.smithing_plugin", "Smithing Plugin");
        AddonLangHandler.extra(
            "plugin.anvilcraft_terminal_plugins.smithing_plugin.desc",
            "Runs smithing table recipes (netherite upgrades, armour trims) with items from the storage"
        );
        AddonLangHandler.extra(
            "screen.anvilcraft_terminal_plugins.panel.smithing_tip",
            "Click to change smithing per cycle; only items in the filter table may be used as a base"
        );
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.xp_mode.off", "Off");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.xp_mode.store", "Store");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.xp_mode.withdraw", "Withdraw");
        AddonLangHandler.extra(
            "screen.anvilcraft_terminal_plugins.panel.xp_store_tip",
            "Above this level, spare experience becomes exp gems in the storage"
        );
        AddonLangHandler.extra(
            "screen.anvilcraft_terminal_plugins.panel.xp_keep_tip",
            "Below this level, exp gems from the storage become experience again"
        );
        AddonLangHandler.extra(
            "screen.anvilcraft_terminal_plugins.panel.tool_swap_return_tip",
            "On: worn tools go back into the storage; Off: they stay in your inventory"
        );
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.setting.keep_stacks", "Keep %s stacks");
        AddonLangHandler.extra("plugin.anvilcraft_terminal_plugins.compacting_plugin", "Compacting Plugin");
        AddonLangHandler.extra(
            "plugin.anvilcraft_terminal_plugins.compacting_plugin.desc",
            "Compacts 9 matching items into 1 using vanilla crafting recipes"
        );
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.setting.batch", "Per cycle: %s");
        AddonLangHandler.extra("plugin.anvilcraft_terminal_plugins.alchemy_plugin", "Alchemy Plugin");
        AddonLangHandler.extra(
            "plugin.anvilcraft_terminal_plugins.alchemy_plugin.desc",
            "Uses stored potions automatically when a condition is met"
        );

        AddonLangHandler.extra(
            "tooltip.anvilcraft_terminal_plugins.plugin.install_hint",
            "Install it on a terminal at the Plugin Station"
        );
        AddonLangHandler.extra(
            "tooltip.anvilcraft_terminal_plugins.plugin.configure_hint",
            "Right click to cycle the main setting, sneak + right click for the secondary"
        );
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.filter.mode", "Mode: %s");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.filter.mode.whitelist", "Whitelist");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.filter.mode.blacklist", "Blacklist");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.filter.components.on", "Match components: ON");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.filter.components.off", "Match components: OFF");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.magnet.range", "Radius: %s blocks");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.magnet.mode.magnet", "Magnet only");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.magnet.mode.pickup", "Pickup only");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.cooking.mode", "Recipe: %s");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.cooking.mode.smelting", "Smelting");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.cooking.mode.blasting", "Blasting");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.cooking.mode.smoking", "Smoking");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.cooking.mode.campfire", "Campfire");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.cooking.fuel.on", "Fuel required: ON");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.cooking.fuel.off", "Fuel required: OFF");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.feeding.threshold", "Eats below %s hunger");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.feeding.harmful.on", "Harmful food: ALLOWED");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.feeding.harmful.off", "Harmful food: BLOCKED");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.alchemy.entries", "Alchemy entries: %s / %s");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.alchemy.nearby", "Nearby targets: %s");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.alchemy.nearby.none", "Self only");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.alchemy.nearby.players", "Self + players");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.alchemy.nearby.mobs", "Self + mobs");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.alchemy.nearby.all", "Self + all");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.alchemy.amplifier.on", "Match effect amplifier: ON");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.alchemy.amplifier.off", "Match effect amplifier: OFF");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.alchemy.trigger.never", "Never");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.alchemy.trigger.always", "Always");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.alchemy.trigger.under_water", "Underwater");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.alchemy.trigger.on_fire", "On fire");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.alchemy.trigger.falling", "Falling");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.alchemy.trigger.sprinting", "Sprinting");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.alchemy.trigger.hurt", "Hurt below");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.alchemy.trigger.negative_effect", "Has debuff");

        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.panel.title", "Terminal Plugins");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.panel.button", "Plugins");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.panel.open", "Adjust Plugins");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.panel.empty", "No plugins installed");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.panel.primary", "Main");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.panel.secondary", "Alt");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.panel.up", "^");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.panel.down", "v");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.panel.enable", "Enable");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.panel.disable", "Disable");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.panel.remove", "Take out");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.panel.alchemy", "Alchemy entries (click slot with potion in cursor)");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.panel.drag_hint", "Drag the Plugins button to move this panel");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.setting.on", "ON");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.setting.off", "OFF");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.setting.range", "Radius %s");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.setting.recipe", "Recipe %s");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.setting.fuel", "Fuel %s");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.setting.threshold", "Hunger %s");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.setting.harmful", "Bad food %s");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.setting.mode", "Mode %s");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.setting.components", "Components %s");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.setting.nearby", "Targets %s");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.setting.amplifier", "Amplifier %s");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.panel.cycle_tip", "Click to cycle this setting");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.panel.settings", "Set");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.panel.back_tip", "Back to the plugin list");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.panel.filter_slot_tip", "Left click puts the item in your cursor, right click clears");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.panel.filter_tip", "Put a potion here: left click places the item in your cursor, right click clears");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.panel.trigger_tip", "Click to cycle the trigger condition");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.panel.radius_tip", "Click to enlarge the radius / right click to shrink it");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.magnet.mode.both", "Magnet + Pickup");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.installed", "%s plugins installed");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.staging", "Staging (click Install all)");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.plugin.panel_hint", "Press K in the terminal screen to adjust plugins");
        AddonLangHandler.extra("key.categories.anvilcraft_terminal_plugins", "AnvilCraft Terminal Plugins");
        AddonLangHandler.extra("key.anvilcraft_terminal_plugins.open_panel", "Open plugin panel");
    }

    private static void extra(String key, String value) {
        AddonLangHandler.EXTRA.put(key, value);
    }

    /**
     * 语言文件初始化：配置项说明 + 自动生成名称之外的手写文本。
     *
     * @param provider 提供器
     */
    public static void init(RegistrumLangProvider provider) {
        ConfigData.readConfigClass(provider, TerminalPluginsConfig.class);
        AddonLangHandler.EXTRA.forEach(provider::add);
    }
}