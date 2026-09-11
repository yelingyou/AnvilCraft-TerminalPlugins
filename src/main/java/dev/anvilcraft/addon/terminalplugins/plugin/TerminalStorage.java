/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.plugin;

import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Predicate;

/**
 * 终端所连接存储的只读视图 + 常用读写操作封装。
 *
 * <p>铁砧工艺的存储是「按类型计数、数量无限」的稀疏模型（{@link UnlimitedItemStacksResourceHandler}），
 * 因此这里统一按「物品类型槽位」遍历，写入走 {@code insertItem(stack, false)}，
 * 取出走 {@code extractUnlimited(index, amount, false)}。</p>
 */
public final class TerminalStorage {
    private final List<BaseStorage<?>> storages;

    public TerminalStorage(List<BaseStorage<?>> storages) {
        this.storages = storages;
    }

    public static TerminalStorage empty() {
        return new TerminalStorage(List.of());
    }

    public boolean isReachable() {
        return !this.storages.isEmpty();
    }

    public List<BaseStorage<?>> storages() {
        return this.storages;
    }

    /** 把所有物品插入存储，返回实际插入数量。 */
    public int insert(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        int remaining = stack.getCount();
        for (BaseStorage<?> storage : this.storages) {
            UnlimitedItemStacksResourceHandler handler = storage.getItems();
            ItemStack left = handler.insertItem(stack.copyWithCount(remaining), false);
            int inserted = remaining - left.getCount();
            remaining = left.getCount();
            if (remaining <= 0) {
                return stack.getCount();
            }
        }
        return stack.getCount() - remaining;
    }

    /** 统计所有匹配物品的总数量（只读，不受单格堆叠上限影响）。 */
    public long count(Predicate<ItemStack> matcher) {
        long total = 0;
        for (BaseStorage<?> storage : this.storages) {
            UnlimitedItemStacksResourceHandler handler = storage.getItems();
            for (int index = 0; index < handler.size(); index++) {
                UnlimitedItemStack unlimited = handler.getUnlimitedStackInSlot(index);
                if (unlimited.isEmpty()) {
                    continue;
                }
                ItemStack representative = unlimited.getStack();
                if (matcher.test(representative)) {
                    total += unlimited.getCount();
                }
            }
        }
        return total;
    }

    /**
     * 找到第一个匹配的物品类型并取出最多 {@code amount} 个。
     *
     * @return 取出的物品堆（可能为空），数量不超过该物品的单组上限
     */
    public ItemStack extractFirst(Predicate<ItemStack> matcher, int amount) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        for (BaseStorage<?> storage : this.storages) {
            UnlimitedItemStacksResourceHandler handler = storage.getItems();
            for (int index = 0; index < handler.size(); index++) {
                UnlimitedItemStack unlimited = handler.getUnlimitedStackInSlot(index);
                if (unlimited.isEmpty()) {
                    continue;
                }
                ItemStack representative = unlimited.getStack();
                if (!matcher.test(representative)) {
                    continue;
                }
                int take = (int) Math.min(Math.min(amount, representative.getMaxStackSize()), unlimited.getCount());
                if (take <= 0) {
                    continue;
                }
                UnlimitedItemStack extracted = handler.extractUnlimited(index, take, false);
                if (!extracted.isEmpty()) {
                    return extracted.toStack();
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /** 取出指定类型的精确数量（允许超过单组上限），用于批量烹饪等场景。 */
    public long extractExact(Predicate<ItemStack> matcher, long amount) {
        if (amount <= 0) {
            return 0;
        }
        long taken = 0;
        for (BaseStorage<?> storage : this.storages) {
            UnlimitedItemStacksResourceHandler handler = storage.getItems();
            for (int index = 0; index < handler.size() && taken < amount; index++) {
                UnlimitedItemStack unlimited = handler.getUnlimitedStackInSlot(index);
                if (unlimited.isEmpty() || !matcher.test(unlimited.getStack())) {
                    continue;
                }
                int take = (int) Math.min(amount - taken, unlimited.getCount());
                if (take <= 0) {
                    continue;
                }
                UnlimitedItemStack extracted = handler.extractUnlimited(index, take, false);
                taken += extracted.getCount();
            }
        }
        return taken;
    }

    /** 存储中所有不同的物品类型（每个类型返回一个数量为 1 的展示栈，只读）。 */
    public List<ItemStack> types() {
        List<ItemStack> result = new java.util.ArrayList<>();
        for (BaseStorage<?> storage : this.storages) {
            UnlimitedItemStacksResourceHandler handler = storage.getItems();
            for (int index = 0; index < handler.size(); index++) {
                UnlimitedItemStack unlimited = handler.getUnlimitedStackInSlot(index);
                if (unlimited.isEmpty()) {
                    continue;
                }
                ItemStack representative = unlimited.getStack().copyWithCount(1);
                boolean duplicated = result.stream().anyMatch(existing -> ItemStack.isSameItemSameComponents(existing, representative));
                if (!duplicated) {
                    result.add(representative);
                }
            }
        }
        return result;
    }

    /** 存储中第一个非空物品的只读展示栈（用于调试/提示）。 */
    public ItemStack peekFirst() {
        for (BaseStorage<?> storage : this.storages) {
            UnlimitedItemStacksResourceHandler handler = storage.getItems();
            for (int index = 0; index < handler.size(); index++) {
                UnlimitedItemStack unlimited = handler.getUnlimitedStackInSlot(index);
                if (!unlimited.isEmpty()) {
                    return unlimited.getStack();
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
