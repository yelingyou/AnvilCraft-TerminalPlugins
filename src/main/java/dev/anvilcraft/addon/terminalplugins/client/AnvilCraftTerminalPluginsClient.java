/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.client;

import dev.anvilcraft.addon.terminalplugins.AnvilCraftTerminalPlugins;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

// 客户端入口：注册插件面板的按键；界面装配在 TerminalPluginClientEvents 里。
@Mod(value = AnvilCraftTerminalPlugins.MOD_ID, dist = Dist.CLIENT)
public class AnvilCraftTerminalPluginsClient {
    public AnvilCraftTerminalPluginsClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(ModKeyMappings::register);
    }
}
