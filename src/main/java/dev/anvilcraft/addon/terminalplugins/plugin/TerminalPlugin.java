/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.plugin;

import net.minecraft.world.item.ItemStack;

/**
 * 终端插件的行为实现。
 *
 * <p>实现类由 {@link TerminalPluginRegistry} 按 {@link PluginKind} 注册；
 * 终端每个行为周期会遍历已安装插件并调用 {@link #onPlayerTick(PluginContext)}。</p>
 */
public interface TerminalPlugin {
    PluginKind kind();

    /** 行为触发周期（tick）。返回 20 表示每秒一次。 */
    default int intervalTicks() {
        return 10;
    }

    /**
     * 每 {@link #intervalTicks()} tick 调用一次（服务端）。
     */
    default void onPlayerTick(PluginContext context) {
    }

    /**
     * 过滤钩子：返回 {@code false} 时，本附属的自动入库行为不会放入该物品。
     *
     * @return 允许入库返回 {@code true}
     */
    default boolean allowsInsert(PluginContext context, ItemStack stack) {
        return true;
    }
}
