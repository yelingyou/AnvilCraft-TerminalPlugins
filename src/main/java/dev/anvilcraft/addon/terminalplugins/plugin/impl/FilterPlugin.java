/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.plugin.impl;

import dev.anvilcraft.addon.terminalplugins.plugin.PluginContext;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginKind;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPlugin;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.world.item.ItemStack;

/**
 * 过滤插件：复用铁砧工艺自身的过滤组件（{@link FilterContent}）语义——
 * 支持白名单 / 黑名单、是否比对组件、以及用命名牌写 {@code #tag} 做标签过滤。
 *
 * <p>作用范围：本附属所有的自动入库行为（磁吸拾取、自动烹饪产物、炼金产物）。
 * 玩家在终端界面手动存入的物品由铁砧工艺本体处理，不受本插件限制。</p>
 */
public class FilterPlugin implements TerminalPlugin {
    @Override
    public PluginKind kind() {
        return PluginKind.FILTER;
    }

    @Override
    public int intervalTicks() {
        return 20;
    }

    @Override
    public boolean allowsInsert(PluginContext context, ItemStack stack) {
        FilterContent content = context.pluginStack().get(ModComponents.FILTER_CONTENT);
        if (content == null) {
            return true;
        }
        return content.filter(stack);
    }
}
