/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.plugin.impl;

import dev.anvilcraft.addon.terminalplugins.component.FluidSettings;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginContext;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginKind;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPlugin;
import dev.dubhe.anvilcraft.api.fluid.FluidHandlerWrapper;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * 流体接口插件：终端自带储液缓冲，流体始终以「容器物品」的形式参与物品存储。
 *
 * <p>两种模式都复用铁砧工艺自己的 {@link FluidHandlerWrapper}（它已经处理了桶、玻璃瓶、
 * 水瓶、蜜瓶、不祥之瓶等特例），因此这里只需要负责「从存储取容器 / 把容器放回存储」。</p>
 *
 * <p>模拟（simulate）调用一律作用在 {@code copy()} 上，失败时把取出的原容器原样放回，
 * 避免出现「容器被消耗但流体没动」的丢物品 bug。</p>
 */
public class FluidPlugin implements TerminalPlugin {
    @Override
    public PluginKind kind() {
        return PluginKind.FLUID;
    }

    @Override
    public int intervalTicks() {
        return 20;
    }

    @Override
    public void onPlayerTick(PluginContext context) {
        if (context.player() == null || !context.storageReachable()) {
            return;
        }
        FluidSettings settings = context.pluginStack().getOrDefault(
            AddonDataComponents.FLUID_SETTINGS, FluidSettings.DEFAULT);
        if (settings.mode() == FluidSettings.FluidMode.OFF) {
            return;
        }
        FilterContent filter = context.pluginStack().get(ModComponents.FILTER_CONTENT);
        PluginFluidHandler handler = new PluginFluidHandler(context.pluginStack(), settings.capacityMb());
        FluidHandlerWrapper wrapper = new FluidHandlerWrapper(handler);
        boolean filling = settings.mode() == FluidSettings.FluidMode.FILL_BUFFER;

        for (int index = 0; index < settings.batch(); index++) {
            boolean moved = filling
                ? FluidPlugin.fillBuffer(context, wrapper, handler, filter)
                : FluidPlugin.emptyBuffer(context, wrapper, handler, filter);
            if (!moved) {
                return;
            }
        }
    }

    /** 抽进缓冲：取一个能倒出流体的容器，成功则把空容器放回存储。 */
    private static boolean fillBuffer(PluginContext context, FluidHandlerWrapper wrapper,
                                      PluginFluidHandler handler, FilterContent filter) {
        int before = handler.getFluid().getAmount();
        Predicate<ItemStack> candidate = stack -> FluidPlugin.matches(stack, filter)
                                                 && !stack.isEmpty()
                                                 && wrapper.fillFromItem(stack.copyWithCount(1), true) != null;
        ItemStack container = context.storage().extractFirst(candidate, 1);
        if (container.isEmpty()) {
            return false;
        }
        ItemStack emptied = wrapper.fillFromItem(container.copy());
        if (emptied == null || emptied.isEmpty() || handler.getFluid().getAmount() <= before) {
            context.insertIntoStorage(container);
            return false;
        }
        context.insertIntoStorage(emptied);
        return true;
    }

    /** 灌进容器：取一个空容器，成功则把装满的容器放回存储。 */
    private static boolean emptyBuffer(PluginContext context, FluidHandlerWrapper wrapper,
                                       PluginFluidHandler handler, FilterContent filter) {
        int before = handler.getFluid().getAmount();
        Predicate<ItemStack> candidate = stack -> FluidPlugin.matches(stack, filter)
                                                 && !stack.isEmpty()
                                                 && wrapper.drainToItem(stack.copyWithCount(1), true) != null;
        ItemStack empty = context.storage().extractFirst(candidate, 1);
        if (empty.isEmpty()) {
            return false;
        }
        ItemStack filled = wrapper.drainToItem(empty.copy());
        if (filled == null || filled.isEmpty() || handler.getFluid().getAmount() >= before) {
            context.insertIntoStorage(empty);
            return false;
        }
        context.insertIntoStorage(filled);
        return true;
    }

    /** 过滤表留空 = 所有容器都允许；填了内容就只处理表里的容器。 */
    private static boolean matches(ItemStack stack, FilterContent filter) {
        if (filter == null) {
            return true;
        }
        if (filter.list().stream().allMatch(ItemStack::isEmpty)) {
            return true;
        }
        return filter.filter(stack);
    }
}