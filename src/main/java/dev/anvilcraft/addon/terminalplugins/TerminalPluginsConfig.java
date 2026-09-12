/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins;

import dev.anvilcraft.lib.v2.config.BoundedDiscrete;
import dev.anvilcraft.lib.v2.config.Comment;
import dev.anvilcraft.lib.v2.config.Config;

@Config(name = AnvilCraftTerminalPlugins.MOD_ID)
public class TerminalPluginsConfig {
    @Comment("终端最多可安装的插件数量 / Max plugins installable on a single terminal")
    @BoundedDiscrete(max = 9, min = 1)
    public int maxPluginsPerTerminal = 5;

    @Comment("插件行为检测间隔（tick） / Tick interval between plugin effect scans")
    @BoundedDiscrete(max = 100, min = 1)
    public int pluginTickInterval = 10;

    @Comment("磁吸/拾取插件默认工作半径 / Default radius of the magnet & pickup plugin")
    @BoundedDiscrete(max = 16, min = 1)
    public int magnetDefaultRange = 5;

    @Comment("在日志里打印插件派发信息，排查插件不生效时打开 / Log plugin dispatch details (for troubleshooting)")
    public boolean debugLogging = false;

    @Comment("启用本模组物品的铁砧工艺化配方（冲压 / 充能 / 时移）/ Enable AnvilCraft-flavoured recipes for this mod's items")
    public boolean enableAnvilCraftRecipes = true;

    @Comment("自动烹饪插件每次处理的最大物品数 / Max items handled per auto-cooking operation")
    @BoundedDiscrete(max = 64, min = 1)
    public int autoCookingBatchSize = 8;
}