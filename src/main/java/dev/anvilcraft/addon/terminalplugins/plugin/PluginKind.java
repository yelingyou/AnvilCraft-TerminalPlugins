/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.plugin;

import dev.anvilcraft.addon.terminalplugins.AnvilCraftTerminalPlugins;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 终端插件种类。每种插件对应一枚可安装的物品。
 */
public enum PluginKind {
    /** 过滤：决定哪些物品允许进入终端连接的存储。 */
    FILTER("filter_plugin"),
    /** 磁吸 / 拾取：把附近的掉落物直接收进存储。 */
    MAGNET("magnet_plugin"),
    /** 自动熔炼 · 烹饪：把存储中的原料按原版烹饪配方自动加工。 */
    AUTO_COOKING("auto_cooking_plugin"),
    /** 自动喂食：饥饿时自动从存储取食物进食。 */
    FEEDING("feeding_plugin"),
    /** 炼金：在存储内自动酿造药水。 */
    ALCHEMY("alchemy_plugin"),
    /** 销毁：存储中匹配过滤的物品只保留指定组数，多余部分销毁。 */
    VOID("void_plugin"),
    /** 压缩：把存储里的 9 个同类物品自动压成 1 个。 */
    COMPACTING("compacting_plugin"),
    /** 流体接口：终端自带储液缓冲，与存储里的容器互换流体。 */
    FLUID("fluid_plugin"),
    /** 工具切换：手持工具快坏掉时，自动从存储换一把更好的同种工具。 */
    TOOL_SWAP("tool_swap_plugin"),
    /** 生物捕捉：把存储里的空树脂块变成装着生物的树脂块。 */
    MOB_CATCHER("mob_catcher_plugin"),
    /** 经验泵：玩家经验与存储里的经验宝石互转。 */
    XP_PUMP("xp_pump_plugin"),
    /** 锻造：用存储里的模板 / 基底 / 附加物完成锻造台配方（本体合成窗口没有锻造台）。 */
    SMITHING("smithing_plugin"),
    ;

    private final String id;

    PluginKind(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public ResourceLocation resourceLocation() {
        return AnvilCraftTerminalPlugins.of(this.id);
    }

    public String descriptionId() {
        return "plugin." + AnvilCraftTerminalPlugins.MOD_ID + "." + this.id;
    }

    public Component displayName() {
        return Component.translatable(this.descriptionId());
    }
}
