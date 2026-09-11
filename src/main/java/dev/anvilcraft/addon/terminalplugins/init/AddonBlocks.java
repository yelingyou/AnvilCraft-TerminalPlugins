/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.init;

import dev.anvilcraft.addon.terminalplugins.block.PluginStationBlock;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import net.minecraft.world.level.block.Blocks;

import static dev.anvilcraft.addon.terminalplugins.AnvilCraftTerminalPlugins.REGISTRUM;

public class AddonBlocks {
    static {
        REGISTRUM.defaultCreativeTab(AddonItemGroups.TERMINAL_PLUGINS.getKey());
    }

    /** 插件安装台：把插件安装到终端上 / 从终端上取下插件。 */
    public static final BlockEntry<PluginStationBlock> PLUGIN_STATION = REGISTRUM
        .block("plugin_station", PluginStationBlock::new)
        .initialProperties(() -> Blocks.IRON_BLOCK)
        .properties(properties -> properties
            .noOcclusion()
            .isValidSpawn((state, level, pos, type) -> false)
            .requiresCorrectToolForDrops())
        .lang("Terminal Plugin Station")
        .simpleItem()
        .register();

    /** 触发本类静态初始化，完成方块注册。 */
    public static void register() {
    }
}
