/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for full license text.
 */
package dev.anvilcraft.addon.terminalplugins.inventory;

import dev.anvilcraft.addon.terminalplugins.block.entity.PluginStationBlockEntity;
import dev.anvilcraft.addon.terminalplugins.init.AddonMenus;
import dev.anvilcraft.addon.terminalplugins.item.TerminalPluginItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import javax.annotation.Nullable;

/**
 * 插件安装台界面。
 *
 * <p>布局常量全部集中在这里（{@link #LAYOUT_}），界面绘制直接读同一批常量，
 * 不会出现「槽位画在这里、可点击区域在那里」的错位。</p>
 */
public class PluginStationMenu extends AbstractContainerMenu {
    private static final int PLAYER_INVENTORY_ROWS = 3;
    private static final int PLAYER_COLUMNS = 9;

    /** 面板尺寸与关键 Y 坐标（单位：像素，和原版容器界面同一套坐标系）。 */
    public static final int PANEL_WIDTH = 176;
    public static final int PANEL_HEIGHT = 220;
    /** 玩家背包标题的 Y：下面留出 12 像素再放第一行槽位，不会被网格压住。 */
    public static final int PLAYER_LABEL_Y = 126;

    public static final int TERMINAL_SLOT_X = 26;
    public static final int TERMINAL_SLOT_Y = 28;
    public static final int STAGING_X = 26;
    public static final int STAGING_Y = 56;
    public static final int PLAYER_INV_Y = 138;
    public static final int HOTBAR_Y = 196;
    public static final int BUTTON_X = 98;
    public static final int BUTTON_Y = 56;
    public static final int BUTTON_WIDTH = 70;
    public static final int BUTTON_HEIGHT = 16;
    public static final int BUTTON_GAP = 4;

    private final BlockPos pos;
    private final IItemHandler stationInventory;
    private final ContainerLevelAccess access;

    // 服务端专用：由方块打开。
    public PluginStationMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(AddonMenus.PLUGIN_STATION.get(), containerId, playerInventory, pos);
    }

    // 网络构造：客户端读取方块坐标后解析对应方块实体。
    public static PluginStationMenu fromNetwork(
        @Nullable MenuType<?> menuType,
        int containerId,
        Inventory playerInventory,
        FriendlyByteBuf buf
    ) {
        return new PluginStationMenu(menuType, containerId, playerInventory, buf.readBlockPos());
    }

    private PluginStationMenu(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory, BlockPos pos) {
        this(menuType, containerId, playerInventory, PluginStationMenu.handlerAt(playerInventory, pos), pos);
    }

    private PluginStationMenu(
        @Nullable MenuType<?> menuType,
        int containerId,
        Inventory playerInventory,
        IItemHandler stationInventory,
        BlockPos pos
    ) {
        super(menuType, containerId);
        this.pos = pos;
        this.stationInventory = stationInventory;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);

        this.addSlot(new SlotItemHandler(
            stationInventory,
            PluginStationBlockEntity.TERMINAL_SLOT,
            PluginStationMenu.TERMINAL_SLOT_X,
            PluginStationMenu.TERMINAL_SLOT_Y
        ) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return PluginStationBlockEntity.isTerminal(stack);
            }
        });
        for (int index = 0; index < PluginStationBlockEntity.PLUGIN_SLOTS; index++) {
            int row = index / 3;
            int column = index % 3;
            this.addSlot(new SlotItemHandler(
                stationInventory,
                PluginStationBlockEntity.PLUGIN_SLOTS_START + index,
                PluginStationMenu.STAGING_X + column * 18,
                PluginStationMenu.STAGING_Y + row * 18
            ) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof TerminalPluginItem;
                }
            });
        }

        for (int row = 0; row < PluginStationMenu.PLAYER_INVENTORY_ROWS; row++) {
            for (int column = 0; column < PluginStationMenu.PLAYER_COLUMNS; column++) {
                this.addSlot(new Slot(
                    playerInventory,
                    column + row * PluginStationMenu.PLAYER_COLUMNS + PluginStationMenu.PLAYER_COLUMNS,
                    8 + column * 18,
                    PluginStationMenu.PLAYER_INV_Y + row * 18
                ));
            }
        }
        for (int column = 0; column < PluginStationMenu.PLAYER_COLUMNS; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, PluginStationMenu.HOTBAR_Y));
        }
    }

    private static IItemHandler handlerAt(Inventory playerInventory, BlockPos pos) {
        if (playerInventory.player.level().getBlockEntity(pos) instanceof PluginStationBlockEntity station) {
            return station.getInventory();
        }
        // 客户端区块未加载等情况下使用占位容器，保证槽位数量一致
        return new ItemStackHandler(PluginStationBlockEntity.SLOT_COUNT);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public IItemHandler getStationInventory() {
        return this.stationInventory;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        int stationSlots = PluginStationBlockEntity.SLOT_COUNT;
        if (index < stationSlots) {
            if (!this.moveItemStackTo(stack, stationSlots, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (PluginStationBlockEntity.isTerminal(stack)) {
            if (!this.moveItemStackTo(stack, PluginStationBlockEntity.TERMINAL_SLOT, PluginStationBlockEntity.TERMINAL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof TerminalPluginItem) {
            if (!this.moveItemStackTo(
                stack,
                PluginStationBlockEntity.PLUGIN_SLOTS_START,
                stationSlots,
                false
            )) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate(
            (level, blockPos) -> !level.getBlockState(blockPos).isAir()
                                && player.distanceToSqr(
                                    blockPos.getX() + 0.5,
                                    blockPos.getY() + 0.5,
                                    blockPos.getZ() + 0.5
                                ) <= 64.0,
            true
        );
    }
}