/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.plugin;

import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.TerminalBlockRegistry;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.StorageRef;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.HyperdimensionStorage;
import dev.dubhe.anvilcraft.saved.storage.LargeCrateStorage;
import dev.dubhe.anvilcraft.saved.storage.ShulkerContainerStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * 从终端物品栈解析它当前连接的存储。
 *
 * <p>语义与铁砧工艺内置终端保持一致：</p>
 * <ul>
 *   <li>本地终端 → 玩家 32 格内最近的大型板条箱；</li>
 *   <li>潜影终端 → 身上最靠前的、已绑定存储 id 的潜影集装箱，其次 64 格内世界潜影集装箱；</li>
 *   <li>超维终端 → {@code TERMINAL_BINDING} 绑定的全局存储。</li>
 * </ul>
 */
public final class TerminalStorageResolver {
    public static final int LOCAL_TERMINAL_RANGE = 32;
    public static final int SHULKER_TERMINAL_RANGE = 64;

    private TerminalStorageResolver() {
    }

    public static TerminalStorage resolve(ServerPlayer player, ItemStack terminal) {
        List<BaseStorage<?>> storages = resolveAll(player, terminal);
        return storages.isEmpty() ? TerminalStorage.empty() : new TerminalStorage(List.copyOf(storages));
    }

    public static List<BaseStorage<?>> resolveAll(ServerPlayer player, ItemStack terminal) {
        if (terminal.is(ModItems.LOCAL_TERMINAL)) {
            return TerminalStorageResolver.nearestLargeCrate(player)
                .map(id -> List.<BaseStorage<?>>of(Storages.get().getOrCreate(id, LargeCrateStorage.class)))
                .orElseGet(List::of);
        }
        if (terminal.is(ModItems.SHULKER_TERMINAL)) {
            List<BaseStorage<?>> fromInventory = TerminalStorageResolver.frontmostShulkerStorages(player);
            if (!fromInventory.isEmpty()) {
                return fromInventory;
            }
            return TerminalStorageResolver.nearestShulkerContainer(player)
                .map(id -> List.<BaseStorage<?>>of(Storages.get().getOrCreate(id, ShulkerContainerStorage.class)))
                .orElseGet(List::of);
        }
        if (terminal.is(ModItems.HYPERDIMENSION_TERMINAL)) {
            UUID targetId = StorageServerStub.terminalTargetId(player, terminal);
            if (targetId == null) {
                return List.of();
            }
            return List.of(Storages.get().getOrCreate(targetId, HyperdimensionStorage.class));
        }
        return List.of();
    }

    private static Optional<UUID> nearestLargeCrate(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = TerminalBlockRegistry.nearestLargeCrate(
            level,
            player.getX(),
            player.getY(),
            player.getZ(),
            TerminalStorageResolver.LOCAL_TERMINAL_RANGE
        );
        return TerminalStorageResolver.storageIdAt(level, pos);
    }

    private static Optional<UUID> nearestShulkerContainer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = TerminalBlockRegistry.nearestShulkerContainer(
            level,
            player.getX(),
            player.getY(),
            player.getZ(),
            TerminalStorageResolver.SHULKER_TERMINAL_RANGE
        );
        return TerminalStorageResolver.storageIdAt(level, pos);
    }

    private static Optional<UUID> storageIdAt(ServerLevel level, @Nullable BlockPos pos) {
        if (pos == null) {
            return Optional.empty();
        }
        if (!(level.getBlockEntity(pos) instanceof StorageBlockEntity storage)) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.getId());
    }

    /** 玩家身上（含副手）所有已绑定存储 id 的潜影集装箱，按槽位顺序。 */
    private static List<BaseStorage<?>> frontmostShulkerStorages(ServerPlayer player) {
        List<BaseStorage<?>> result = new ArrayList<>();
        int size = player.getInventory().getContainerSize();
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            StorageRef ref = stack.get(ModComponents.STORAGE);
            if (ref == null || ref.id().isEmpty()) {
                continue;
            }
            if (!(stack.getItem() instanceof dev.dubhe.anvilcraft.block.item.ShulkerContainerBlockItem)) {
                continue;
            }
            result.add(Storages.get().getOrCreate(ref.id().get(), ShulkerContainerStorage.class));
        }
        return result;
    }
}
