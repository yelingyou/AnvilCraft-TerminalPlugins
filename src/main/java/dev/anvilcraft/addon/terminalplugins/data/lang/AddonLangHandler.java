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
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.hint", "Left: terminal, right: plugins");
        AddonLangHandler.extra("message.anvilcraft_terminal_plugins.setting", "%s → %s");

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
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.panel.filter_tip", "Put a potion here: left click places the item in your cursor, right click clears");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.panel.trigger_tip", "Click to cycle the trigger condition");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.panel.radius_tip", "Click to enlarge the radius / right click to shrink it");
        AddonLangHandler.extra("tooltip.anvilcraft_terminal_plugins.magnet.mode.both", "Magnet + Pickup");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.installed", "Installed");
        AddonLangHandler.extra("screen.anvilcraft_terminal_plugins.staging", "Staging");
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