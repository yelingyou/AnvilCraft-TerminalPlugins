/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.plugin.impl;

import dev.anvilcraft.addon.terminalplugins.component.ToolSwapSettings;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginContext;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginKind;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPlugin;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 工具切换插件：手持工具快坏掉时，从终端连接的存储里换一把同种、剩余耐久更多的工具。
 *
 * <p>与本体「智能补货」的区别：本体补货是「手持物品用完 → 补满一组」，对不可堆叠的工具无意义；
 * 本插件做的是**磨损前更换**，换的是同一个槽位里的单件工具。</p>
 *
 * <p>防刷要点：替换品的剩余耐久必须**严格大于**当前工具，因此同一把工具不会来回换；
 * 换下来的旧工具默认放回存储（可以作为铁砧修复插件的材料）。</p>
 */
public class ToolSwapPlugin implements TerminalPlugin {
    @Override
    public PluginKind kind() {
        return PluginKind.TOOL_SWAP;
    }

    @Override
    public int intervalTicks() {
        return 10;
    }

    @Override
    public void onPlayerTick(PluginContext context) {
        ServerPlayer player = context.player();
        if (player == null || !context.storageReachable()) {
            return;
        }
        ToolSwapSettings settings = context.pluginStack().getOrDefault(
            AddonDataComponents.TOOL_SWAP_SETTINGS, ToolSwapSettings.DEFAULT);
        FilterContent filter = context.pluginStack().get(ModComponents.FILTER_CONTENT);
        Inventory inventory = player.getInventory();

        if (settings.target() == ToolSwapSettings.SwapTarget.MAIN_HAND) {
            ToolSwapPlugin.trySwap(context, inventory, inventory.selected, settings, filter);
            return;
        }
        for (int slot = 0; slot < 9; slot++) {
            ToolSwapPlugin.trySwap(context, inventory, slot, settings, filter);
        }
    }

    /** 判断某个快捷栏槽位是否需要换工具，需要则换掉并返回 {@code true}。 */
    private static boolean trySwap(PluginContext context, Inventory inventory, int slot,
                                   ToolSwapSettings settings, FilterContent filter) {
        ServerPlayer player = context.player();
        if (player == null) {
            return false;
        }
        ItemStack current = inventory.getItem(slot);
        if (current.isEmpty() || !current.isDamageableItem() || current.getMaxDamage() <= 0) {
            return false;
        }
        if (!ToolSwapPlugin.matches(current, filter)) {
            return false;
        }
        int remaining = current.getMaxDamage() - current.getDamageValue();
        int threshold = Math.max(1, current.getMaxDamage() * settings.thresholdPercent() / 100);
        if (remaining > threshold) {
            return false;
        }

        ItemStack replacement = context.storage().extractFirst(candidate ->
            candidate.isDamageableItem()
                && candidate.getItem() == current.getItem()
                && candidate.getMaxDamage() - candidate.getDamageValue() > remaining
                && ToolSwapPlugin.matches(candidate, filter), 1);
        if (replacement.isEmpty()) {
            return false;
        }

        inventory.setItem(slot, replacement.copyWithCount(1));
        if (settings.returnWorn()) {
            context.insertIntoStorage(current);
        } else if (!inventory.add(current)) {
            player.drop(current, false);
        }
        inventory.setChanged();
        player.containerMenu.broadcastChanges();
        return true;
    }

    /**
     * 过滤表语义：留空 = 对所有可损坏物品生效；填了内容就只换表里的工具。
     *
     * <p>注意本体的过滤器在「白名单 + 空表」时对任何物品都返回 false（用来做白名单是安全的），
     * 而工具切换是增益型插件，留空时应当直接生效，所以这里特殊处理。</p>
     */
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