/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.init;

import dev.anvilcraft.addon.terminalplugins.block.entity.PluginStationBlockEntity;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntityEntry;

import static dev.anvilcraft.addon.terminalplugins.AnvilCraftTerminalPlugins.REGISTRUM;

public class AddonBlockEntities {
    public static final BlockEntityEntry<PluginStationBlockEntity> PLUGIN_STATION = REGISTRUM
        .blockEntity("plugin_station", PluginStationBlockEntity::new)
        .validBlocks(AddonBlocks.PLUGIN_STATION)
        .register();

    public static void register() {
    }
}
