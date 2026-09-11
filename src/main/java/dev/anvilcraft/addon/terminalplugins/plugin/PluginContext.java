/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.plugin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 插件行为执行上下文。
 *
 * @param player        携带终端的玩家；仅用于过滤判定等无玩家场景时为 {@code null}
 * @param terminalStack 终端物品堆栈（含插件列表与绑定信息）
 * @param pluginStack   当前插件物品堆栈（含插件自身配置）
 * @param storage       终端当前连接的存储
 * @param gameTime      当前世界时间
 */
public record PluginContext(
    @Nullable ServerPlayer player,
    ItemStack terminalStack,
    ItemStack pluginStack,
    TerminalStorage storage,
    long gameTime
) {
    public boolean storageReachable() {
        return this.storage.isReachable();
    }

    /**
     * 本附属所有自动入库的统一入口：先经过已安装的过滤插件，再写入存储。
     *
     * @return 实际入库数量
     */
    public int insertIntoStorage(ItemStack stack) {
        if (stack.isEmpty() || !this.storageReachable()) {
            return 0;
        }
        if (!TerminalPluginRegistry.passesFilter(this.terminalStack, stack)) {
            return 0;
        }
        return this.storage.insert(stack);
    }
}
