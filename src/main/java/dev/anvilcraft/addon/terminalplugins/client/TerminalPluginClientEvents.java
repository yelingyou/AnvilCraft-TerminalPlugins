/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.client;

import dev.anvilcraft.addon.terminalplugins.AnvilCraftTerminalPlugins;
import dev.anvilcraft.addon.terminalplugins.client.screen.PluginStationScreen;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPluginManager;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.Optional;
import java.util.function.Supplier;

// 客户端接入：在铁砧工艺的存储界面（终端界面）内画「插件」入口按钮与插件调节面板。
// 面板用叠加层实现，因此不会关闭玩家已经打开的终端界面。
@EventBusSubscriber(modid = AnvilCraftTerminalPlugins.MOD_ID, value = Dist.CLIENT)
public class TerminalPluginClientEvents {
    private static TerminalPluginPanel panel;

    public static TerminalPluginPanel panel() {
        if (TerminalPluginClientEvents.panel == null) {
            TerminalPluginClientEvents.panel = new TerminalPluginPanel();
        }
        return TerminalPluginClientEvents.panel;
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        int mouseX = (int) event.getMouseX();
        int mouseY = (int) event.getMouseY();
        if (event.getScreen() instanceof StorageScreen) {
            TerminalPluginClientEvents.drawToggleButton(graphics, minecraft, mouseX, mouseY);
            TerminalPluginClientEvents.panel().render(graphics, minecraft, mouseX, mouseY);
        } else if (event.getScreen() instanceof PluginStationScreen) {
            TerminalPluginClientEvents.panel().render(graphics, minecraft, mouseX, mouseY);
        } else if (TerminalPluginClientEvents.panel().isOpen()) {
            TerminalPluginClientEvents.panel().close();
        }
    }

    private static void drawToggleButton(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
        if (minecraft.player == null) {
            return;
        }
        int slot = TerminalPluginPanel.findHeldTerminalSlot();
        boolean hasTerminal = slot != Integer.MIN_VALUE;
        int count = hasTerminal ? TerminalPluginManager.installed(TerminalPluginPanel.stackAt(slot)).size() : 0;
        int x = TerminalPluginPanel.buttonX(minecraft);
        int y = TerminalPluginPanel.buttonY();
        boolean hovered = TerminalPluginPanel.isOverToggleButton(minecraft, mouseX, mouseY);
        graphics.fill(x, y, x + 52, y + 14, hovered ? 0xFF565666 : 0xFF2A2A33);
        String label = Component.translatable("screen.anvilcraft_terminal_plugins.panel.button").getString() + " " + count;
        graphics.drawString(minecraft.font, label, x + 4, y + 3, hasTerminal ? 0xFFE0E0E6 : 0xFF808088, false);
    }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        if (event.getScreen() instanceof StorageScreen
            && TerminalPluginPanel.isOverToggleButton(minecraft, mouseX, mouseY)) {
            TerminalPluginClientEvents.panel().toggle(TerminalPluginClientEvents.panel().heldTarget());
            event.setCanceled(true);
            return;
        }
        if (TerminalPluginClientEvents.panel().mouseClicked(minecraft, mouseX, mouseY, event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!ModKeyMappings.OPEN_PLUGIN_PANEL.matches(event.getKeyCode(), event.getScanCode())) {
            return;
        }
        if (event.getScreen() instanceof StorageScreen) {
            TerminalPluginClientEvents.panel().toggle(TerminalPluginClientEvents.panel().heldTarget());
            event.setCanceled(true);
        } else if (event.getScreen() instanceof PluginStationScreen) {
            TerminalPluginClientEvents.panel().close();
            event.setCanceled(true);
        }
    }

    // 供安装台界面复用：直接针对安装台里的终端打开面板。
    public static void openForStation(Supplier<ItemStack> terminalSupplier, BlockPos pos) {
        TerminalPluginClientEvents.panel().open(
            new TerminalPluginPanel.Target(Optional.of(pos), -1, terminalSupplier)
        );
    }
}
