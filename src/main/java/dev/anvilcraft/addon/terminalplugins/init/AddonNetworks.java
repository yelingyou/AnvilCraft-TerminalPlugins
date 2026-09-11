/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.init;

import dev.anvilcraft.addon.terminalplugins.network.PluginActionPacket;
import dev.anvilcraft.addon.terminalplugins.network.UninstallPluginsPacket;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class AddonNetworks {
    public static void init(PayloadRegistrar registrar) {
        registrar.playToServer(
            UninstallPluginsPacket.TYPE,
            UninstallPluginsPacket.STREAM_CODEC,
            UninstallPluginsPacket.HANDLER
        );
        registrar.playToServer(
            PluginActionPacket.TYPE,
            PluginActionPacket.STREAM_CODEC,
            PluginActionPacket.HANDLER
        );
    }
}
