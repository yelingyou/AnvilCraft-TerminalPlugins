/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.plugin;

import dev.anvilcraft.addon.terminalplugins.plugin.impl.AlchemyPlugin;
import dev.anvilcraft.addon.terminalplugins.plugin.impl.AutoCookingPlugin;
import dev.anvilcraft.addon.terminalplugins.plugin.impl.CompactingPlugin;
import dev.anvilcraft.addon.terminalplugins.plugin.impl.FeedingPlugin;
import dev.anvilcraft.addon.terminalplugins.plugin.impl.FilterPlugin;
import dev.anvilcraft.addon.terminalplugins.plugin.impl.FluidPlugin;
import dev.anvilcraft.addon.terminalplugins.plugin.impl.MobCatcherPlugin;
import dev.anvilcraft.addon.terminalplugins.plugin.impl.SmithingPlugin;
import dev.anvilcraft.addon.terminalplugins.plugin.impl.ToolSwapPlugin;
import dev.anvilcraft.addon.terminalplugins.plugin.impl.XpPumpPlugin;
import dev.anvilcraft.addon.terminalplugins.plugin.impl.MagnetPlugin;
import dev.anvilcraft.addon.terminalplugins.plugin.impl.VoidPlugin;

/**
 * 内置插件的注册入口。新增插件时在这里登记行为实现即可。
 */
public final class TerminalPlugins {
    private TerminalPlugins() {
    }

    public static void register() {
        TerminalPluginRegistry.register(new FilterPlugin());
        TerminalPluginRegistry.register(new MagnetPlugin());
        TerminalPluginRegistry.register(new AutoCookingPlugin());
        TerminalPluginRegistry.register(new FeedingPlugin());
        TerminalPluginRegistry.register(new AlchemyPlugin());
        TerminalPluginRegistry.register(new VoidPlugin());
        TerminalPluginRegistry.register(new CompactingPlugin());
        TerminalPluginRegistry.register(new FluidPlugin());
        TerminalPluginRegistry.register(new ToolSwapPlugin());
        TerminalPluginRegistry.register(new MobCatcherPlugin());
        TerminalPluginRegistry.register(new XpPumpPlugin());
        TerminalPluginRegistry.register(new SmithingPlugin());
    }
}
