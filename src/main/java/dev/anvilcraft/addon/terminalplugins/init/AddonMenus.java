/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.init;

import dev.anvilcraft.addon.terminalplugins.client.screen.PluginStationScreen;
import dev.anvilcraft.addon.terminalplugins.inventory.PluginStationMenu;
import dev.anvilcraft.lib.v2.registrum.util.entry.MenuEntry;

import static dev.anvilcraft.addon.terminalplugins.AnvilCraftTerminalPlugins.REGISTRUM;

public class AddonMenus {
    public static final MenuEntry<PluginStationMenu> PLUGIN_STATION = REGISTRUM
        .menu(
            "plugin_station",
            (menuType, containerId, inventory, buf) -> PluginStationMenu.fromNetwork(menuType, containerId, inventory, buf),
            () -> PluginStationScreen::new)
        .register();

    public static void register() {
    }
}
