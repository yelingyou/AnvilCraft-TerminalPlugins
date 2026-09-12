/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for full license text.
 */
package dev.anvilcraft.addon.terminalplugins.client.screen;

import dev.anvilcraft.addon.terminalplugins.block.entity.PluginStationBlockEntity;
import dev.anvilcraft.addon.terminalplugins.client.TerminalPluginClientEvents;
import dev.anvilcraft.addon.terminalplugins.inventory.PluginStationMenu;
import dev.anvilcraft.addon.terminalplugins.network.StationActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * 插件安装台界面。
 *
 * <p>三块区域：左边 0 号槽放终端、下面 3×3 暂存槽放插件、右边三个按钮分别做
 * 「安装全部」「全部取下」「调节插件」。暂存槽**不会**自动安装，避免插件放进去就消失、
 * 玩家搞不清到底装上了没有。</p>
 */
public class PluginStationScreen extends AbstractContainerScreen<PluginStationMenu> {
    private static final int PANEL_COLOR = 0xFF1E1E22;
    private static final int BORDER_COLOR = 0xFF6E6E78;
    private static final int SLOT_BORDER = 0xFF8B8B8B;
    private static final int SLOT_COLOR = 0xFF37373B;
    private static final int GHOST_BORDER = 0xFF4E7A4E;
    private static final int GHOST_COLOR = 0xFF2A3329;
    private static final int TEXT_COLOR = 0xFFE0E0E6;
    private static final int DIM_COLOR = 0xFF9A9AA4;

    public PluginStationScreen(PluginStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 200;
        this.inventoryLabelY = 92;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(
            Component.translatable("screen.anvilcraft_terminal_plugins.install_all"),
            button -> PacketDistributor.sendToServer(new StationActionPacket(
                this.menu.getPos(),
                StationActionPacket.INSTALL_ALL
            ))
        ).bounds(this.leftPos + 98, this.topPos + 18, 70, 16).build());
        this.addRenderableWidget(Button.builder(
            Component.translatable("screen.anvilcraft_terminal_plugins.uninstall_all"),
            button -> PacketDistributor.sendToServer(new StationActionPacket(
                this.menu.getPos(),
                StationActionPacket.UNINSTALL_ALL
            ))
        ).bounds(this.leftPos + 98, this.topPos + 38, 70, 16).build());
        this.addRenderableWidget(Button.builder(
            Component.translatable("screen.anvilcraft_terminal_plugins.panel.open"),
            button -> TerminalPluginClientEvents.openForStation(this::terminalStack, this.menu.getPos())
        ).bounds(this.leftPos + 98, this.topPos + 58, 70, 16).build());
    }

    private ItemStack terminalStack() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return ItemStack.EMPTY;
        }
        if (minecraft.level.getBlockEntity(this.menu.getPos()) instanceof PluginStationBlockEntity station) {
            return station.getTerminal();
        }
        return ItemStack.EMPTY;
    }

    private List<ItemStack> installed() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return List.of();
        }
        if (minecraft.level.getBlockEntity(this.menu.getPos()) instanceof PluginStationBlockEntity station) {
            return station.getInstalledPlugins();
        }
        return List.of();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        graphics.fill(x - 1, y - 1, x + this.imageWidth + 1, y + this.imageHeight + 1, PluginStationScreen.BORDER_COLOR);
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, PluginStationScreen.PANEL_COLOR);
        for (Slot slot : this.menu.slots) {
            PluginStationScreen.drawSlot(graphics, x + slot.x - 1, y + slot.y - 1);
        }
        // 已安装插件的幽灵槽位（终端右侧一行，最多 6 个）
        List<ItemStack> plugins = this.installed();
        for (int index = 0; index < 6; index++) {
            PluginStationScreen.drawGhostSlot(graphics, x + 46 + index * 18, y + 19);
        }
        graphics.drawString(this.font, Component.translatable(
            "screen.anvilcraft_terminal_plugins.installed", plugins.size()), x + 46, y + 8,
            PluginStationScreen.DIM_COLOR, false);
        for (int index = 0; index < Math.min(plugins.size(), 6); index++) {
            graphics.renderItem(plugins.get(index), x + 47 + index * 18, y + 20);
        }
        graphics.drawString(this.font, Component.translatable(
            "screen.anvilcraft_terminal_plugins.staging"), x + 26, y + 40,
            PluginStationScreen.DIM_COLOR, false);
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, PluginStationScreen.SLOT_BORDER);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, PluginStationScreen.SLOT_COLOR);
    }

    private static void drawGhostSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, PluginStationScreen.GHOST_BORDER);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, PluginStationScreen.GHOST_COLOR);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(this.font, this.title, this.leftPos + 8, this.topPos + 6, PluginStationScreen.TEXT_COLOR, false);
        graphics.drawString(this.font, Component.translatable(
            "screen.anvilcraft_terminal_plugins.station.hint1"), this.leftPos + 98, this.topPos + 82,
            PluginStationScreen.DIM_COLOR, false);
        graphics.drawString(this.font, Component.translatable(
            "screen.anvilcraft_terminal_plugins.station.hint2"), this.leftPos + 98, this.topPos + 92,
            PluginStationScreen.DIM_COLOR, false);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}