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
import net.minecraft.client.gui.components.Tooltip;
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
 * <p>排版规则（避免不同 GUI Scale 下错乱）：</p>
 * <ul>
 *   <li>所有坐标都来自 {@link PluginStationMenu} 的布局常量，绘制与槽位共用同一套数字；</li>
 *   <li>每个网格上方留 10 像素给标题文字，玩家背包标题与暂存槽之间留 16 像素空白；</li>
 *   <li>长提示一律放进按钮 tooltip，不画在面板上，任何缩放比例都不会压到别的东西。</li>
 * </ul>
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
    /** 已安装插件最多显示几个图标。 */
    private static final int INSTALLED_ICONS = 6;

    public PluginStationScreen(PluginStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = PluginStationMenu.PANEL_WIDTH;
        this.imageHeight = PluginStationMenu.PANEL_HEIGHT;
        this.inventoryLabelY = PluginStationMenu.PLAYER_LABEL_Y;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos + PluginStationMenu.BUTTON_X;
        int y = this.topPos + PluginStationMenu.BUTTON_Y;
        int step = PluginStationMenu.BUTTON_HEIGHT + PluginStationMenu.BUTTON_GAP;
        this.addRenderableWidget(Button.builder(
            Component.translatable("screen.anvilcraft_terminal_plugins.install_all"),
            button -> PacketDistributor.sendToServer(new StationActionPacket(
                this.menu.getPos(),
                StationActionPacket.INSTALL_ALL
            ))
        ).bounds(x, y, PluginStationMenu.BUTTON_WIDTH, PluginStationMenu.BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable(
                "screen.anvilcraft_terminal_plugins.station.install_tip")))
            .build());
        this.addRenderableWidget(Button.builder(
            Component.translatable("screen.anvilcraft_terminal_plugins.uninstall_all"),
            button -> PacketDistributor.sendToServer(new StationActionPacket(
                this.menu.getPos(),
                StationActionPacket.UNINSTALL_ALL
            ))
        ).bounds(x, y + step, PluginStationMenu.BUTTON_WIDTH, PluginStationMenu.BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable(
                "screen.anvilcraft_terminal_plugins.station.uninstall_tip")))
            .build());
        this.addRenderableWidget(Button.builder(
            Component.translatable("screen.anvilcraft_terminal_plugins.panel.open"),
            button -> TerminalPluginClientEvents.openForStation(this::terminalStack, this.menu.getPos())
        ).bounds(x, y + step * 2, PluginStationMenu.BUTTON_WIDTH, PluginStationMenu.BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable(
                "screen.anvilcraft_terminal_plugins.station.panel_tip")))
            .build());
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
        // 已安装插件：终端右侧一行幽灵槽
        List<ItemStack> plugins = this.installed();
        for (int index = 0; index < PluginStationScreen.INSTALLED_ICONS; index++) {
            PluginStationScreen.drawGhostSlot(
                graphics,
                x + PluginStationMenu.TERMINAL_SLOT_X + 20 + index * 18,
                y + PluginStationMenu.TERMINAL_SLOT_Y
            );
        }
        for (int index = 0; index < Math.min(plugins.size(), PluginStationScreen.INSTALLED_ICONS); index++) {
            graphics.renderItem(
                plugins.get(index),
                x + PluginStationMenu.TERMINAL_SLOT_X + 21 + index * 18,
                y + PluginStationMenu.TERMINAL_SLOT_Y + 1
            );
        }
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
        int left = this.leftPos;
        int top = this.topPos;
        // 每个网格自己的标题：画在网格上方 10 像素处，网格与网格之间留足空白
        graphics.drawString(
            this.font,
            Component.translatable("screen.anvilcraft_terminal_plugins.station.terminal"),
            left + 8,
            top + PluginStationMenu.TERMINAL_SLOT_Y - 10,
            PluginStationScreen.DIM_COLOR,
            false
        );
        graphics.drawString(
            this.font,
            Component.translatable(
                "screen.anvilcraft_terminal_plugins.station.installed",
                this.installed().size(),
                PluginStationBlockEntity.PLUGIN_SLOTS
            ),
            left + PluginStationMenu.TERMINAL_SLOT_X + 19,
            top + PluginStationMenu.TERMINAL_SLOT_Y - 10,
            PluginStationScreen.DIM_COLOR,
            false
        );
        graphics.drawString(
            this.font,
            Component.translatable("screen.anvilcraft_terminal_plugins.station.staging_slots"),
            left + 8,
            top + PluginStationMenu.STAGING_Y - 10,
            PluginStationScreen.DIM_COLOR,
            false
        );
        // 标题与「物品」标签由原版 renderLabels 绘制（inventoryLabelY 已避开暂存槽）
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}