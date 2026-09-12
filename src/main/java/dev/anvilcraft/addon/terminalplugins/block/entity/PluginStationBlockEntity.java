/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.block.entity;

import dev.anvilcraft.addon.terminalplugins.component.InstalledPlugins;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.item.TerminalPluginItem;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPluginManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * 插件安装台的方块实体。
 *
 * <p>槽位布局：0 号槽放终端，1..PLUGIN_SLOTS 号槽是**暂存槽**。
 * 暂存槽里的插件不会自动安装：安装与取下都由界面上的按钮明确触发
 * （{@link #applyPlugins()} / {@link #uninstallAll()}），玩家能看清每一步发生了什么。</p>
 */
public class PluginStationBlockEntity extends BlockEntity {
    public static final int TERMINAL_SLOT = 0;
    public static final int PLUGIN_SLOTS_START = 1;
    public static final int PLUGIN_SLOTS = InstalledPlugins.MAX_SLOTS;
    public static final int SLOT_COUNT = PluginStationBlockEntity.PLUGIN_SLOTS_START + PluginStationBlockEntity.PLUGIN_SLOTS;

    /** 内部批量写槽位时置位，避免写一次槽位就同步一次方块与脏标记。 */
    private boolean applying = false;

    private final ItemStackHandler inventory = new ItemStackHandler(PluginStationBlockEntity.SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == PluginStationBlockEntity.TERMINAL_SLOT) {
                return PluginStationBlockEntity.isTerminal(stack);
            }
            return stack.getItem() instanceof TerminalPluginItem;
        }

        @Override
        protected void onContentsChanged(int slot) {
            PluginStationBlockEntity.this.setChanged();
            if (PluginStationBlockEntity.this.level != null && !PluginStationBlockEntity.this.level.isClientSide) {
                PluginStationBlockEntity.this.level.sendBlockUpdated(
                    PluginStationBlockEntity.this.getBlockPos(),
                    PluginStationBlockEntity.this.getBlockState(),
                    PluginStationBlockEntity.this.getBlockState(),
                    Block.UPDATE_ALL
                );
            }
        }
    };

    public PluginStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ItemStackHandler getInventory() {
        return this.inventory;
    }

    public static boolean isTerminal(ItemStack stack) {
        return TerminalPluginManager.isTerminal(stack);
    }

    /** 台面上的终端物品堆栈（可直接修改，改动会写回槽位）。 */
    public ItemStack getTerminal() {
        return this.inventory.getStackInSlot(PluginStationBlockEntity.TERMINAL_SLOT);
    }

    /** 台面上终端当前已安装的插件列表（供界面显示）。 */
    public List<ItemStack> getInstalledPlugins() {
        return TerminalPluginManager.installed(this.getTerminal());
    }
    /**
     * 把暂存槽里的插件装到终端上；超过上限的留在暂存槽里。
     *
     * <p>整个写入过程都置位 applying，避免写终端槽时又触发一次安装。</p>
     */
    public void applyPlugins() {
        ItemStack terminal = this.getTerminal();
        if (!PluginStationBlockEntity.isTerminal(terminal)) {
            return;
        }
        boolean changed = false;
        this.applying = true;
        try {
            for (int index = 0; index < PluginStationBlockEntity.PLUGIN_SLOTS; index++) {
                int slot = PluginStationBlockEntity.PLUGIN_SLOTS_START + index;
                ItemStack plugin = this.inventory.getStackInSlot(slot);
                if (plugin.isEmpty() || !(plugin.getItem() instanceof TerminalPluginItem)) {
                    continue;
                }
                if (!TerminalPluginManager.install(terminal, plugin)) {
                    // 达到上限：留在暂存槽，等玩家拆下别的插件
                    continue;
                }
                this.inventory.setStackInSlot(slot, ItemStack.EMPTY);
                changed = true;
            }
            if (changed) {
                this.inventory.setStackInSlot(PluginStationBlockEntity.TERMINAL_SLOT, terminal);
            }
        } finally {
            this.applying = false;
        }
        if (changed) {
            this.setChanged();
        }
    }

    /**
     * 把终端上已安装的插件全部拆回暂存槽（暂存槽不够时留在终端上）。
     *
     * <p>必须在整段逻辑（含最后写回终端槽）期间保持 applying，
     * 否则写槽会触发 applyPlugins() 把刚拆下的插件又装回去。</p>
     */
    public void uninstallAll() {
        ItemStack terminal = this.getTerminal();
        if (!PluginStationBlockEntity.isTerminal(terminal)) {
            return;
        }
        InstalledPlugins installed = terminal.getOrDefault(AddonDataComponents.INSTALLED_PLUGINS, InstalledPlugins.EMPTY);
        if (installed.isEmpty()) {
            return;
        }
        List<ItemStack> remaining = new ArrayList<>();
        this.applying = true;
        try {
            for (ItemStack plugin : installed.plugins()) {
                int free = this.findFreePluginSlot();
                if (free < 0) {
                    remaining.add(plugin);
                    continue;
                }
                this.inventory.setStackInSlot(free, plugin.copyWithCount(1));
            }
            terminal.set(AddonDataComponents.INSTALLED_PLUGINS, new InstalledPlugins(remaining));
            this.inventory.setStackInSlot(PluginStationBlockEntity.TERMINAL_SLOT, terminal);
        } finally {
            this.applying = false;
        }
        this.setChanged();
    }

    /** 面板 / 网络包修改完终端后的收尾：标记脏并同步。 */
    public void afterPluginChange() {
        this.applying = true;
        try {
            ItemStack terminal = this.getTerminal();
            if (!terminal.isEmpty()) {
                this.inventory.setStackInSlot(PluginStationBlockEntity.TERMINAL_SLOT, terminal);
            }
        } finally {
            this.applying = false;
        }
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(
                this.getBlockPos(),
                this.getBlockState(),
                this.getBlockState(),
                Block.UPDATE_ALL
            );
        }
    }

    private int findFreePluginSlot() {
        for (int index = 0; index < PluginStationBlockEntity.PLUGIN_SLOTS; index++) {
            int slot = PluginStationBlockEntity.PLUGIN_SLOTS_START + index;
            if (this.inventory.getStackInSlot(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", this.inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            this.applying = true;
            try {
                this.inventory.deserializeNBT(registries, tag.getCompound("inventory"));
            } finally {
                this.applying = false;
            }
        }
    }

    /**
     * 把仓库里的东西全部掉落在方块位置。
     *
     * <p><b>只能由 {@code PluginStationBlock#onRemove} 调用</b>：原版 {@code LevelChunk#clearAllBlockEntities()}
     * 在**区块卸载**时也会调用 {@code BlockEntity#setRemoved()}，之前把掉落写在 {@code setRemoved()} 里，
     * 玩家一走远台面上的终端和插件就会掉一地 —— 这是安装台「没法用」的直接原因。</p>
     */
    public void dropContents() {
        if (this.level != null && !this.level.isClientSide) {
            for (int slot = 0; slot < this.inventory.getSlots(); slot++) {
                ItemStack stack = this.inventory.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(
                        this.level,
                        this.getBlockPos().getX(),
                        this.getBlockPos().getY(),
                        this.getBlockPos().getZ(),
                        stack
                    );
                    this.inventory.setStackInSlot(slot, ItemStack.EMPTY);
                }
            }
        }
    }
}
