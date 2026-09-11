/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.client;

import dev.anvilcraft.addon.terminalplugins.component.AlchemySettings;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.item.TerminalPluginItem;
import dev.anvilcraft.addon.terminalplugins.network.PluginActionPacket;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginKind;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPluginManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

// 终端插件调节面板：以叠加层形式画在存储界面之上，不会关闭玩家已打开的终端界面。
// 支持：查看已安装插件、单独开关、切换主/副档位、排序、卸下，以及炼金插件逐条配置。
public class TerminalPluginPanel {
    private static final int PANEL_WIDTH = 158;
    private static final int PANEL_HEIGHT = 190;
    private static final int COLOR_PANEL = 0xF0181820;
    private static final int COLOR_BORDER = 0xFF6E6E78;
    private static final int COLOR_SLOT = 0xFF37373B;
    private static final int COLOR_TEXT = 0xFFE0E0E6;
    private static final int COLOR_DIM = 0xFF9A9AA4;
    private static final int COLOR_BUTTON = 0xFF3C3C46;
    private static final int COLOR_BUTTON_HOVER = 0xFF565666;
    private static final int COLOR_SELECTED = 0xFF4A4A5C;

    private enum Action {
        CLOSE, SELECT, CYCLE_PRIMARY, CYCLE_SECONDARY, REMOVE, MOVE_UP, MOVE_DOWN, TOGGLE,
        SET_ALCHEMY_FILTER, CYCLE_ALCHEMY_TRIGGER, VALUE_UP, VALUE_DOWN, CYCLE_ALCHEMY_NEARBY, RADIUS_UP, RADIUS_DOWN
    }

    private record Region(int x, int y, int width, int height, Action action, int pluginIndex, int entryIndex) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
        }
    }

    // 面板目标：安装台里的终端，或玩家身上的某个终端（-1 主手 / -2 副手 / >=0 物品栏索引）
    public record Target(Optional<BlockPos> station, int containerSlot, Supplier<ItemStack> stackSupplier) {
    }

    private final List<Region> regions = new ArrayList<>();
    private boolean open = false;
    private int selected = 0;
    private Target target;

    public boolean isOpen() {
        return this.open;
    }

    public void close() {
        this.open = false;
    }

    public void open(Target newTarget) {
        this.target = newTarget;
        this.open = newTarget != null;
        this.selected = 0;
    }

    public void toggle(Target newTarget) {
        if (this.open && newTarget != null && this.target != null
            && this.target.station().equals(newTarget.station())
            && this.target.containerSlot() == newTarget.containerSlot()) {
            this.open = false;
            return;
        }
        this.open(newTarget);
    }

    private ItemStack terminal() {
        return this.target == null ? ItemStack.EMPTY : this.target.stackSupplier().get();
    }

    private int left(Minecraft minecraft) {
        return minecraft.getWindow().getGuiScaledWidth() - TerminalPluginPanel.PANEL_WIDTH - 6;
    }

    private int top() {
        return 18;
    }

    public static int buttonX(Minecraft minecraft) {
        return minecraft.getWindow().getGuiScaledWidth() - 58;
    }

    public static int buttonY() {
        return 4;
    }

    public static boolean isOverToggleButton(Minecraft minecraft, double mouseX, double mouseY) {
        return mouseX >= TerminalPluginPanel.buttonX(minecraft) && mouseX < TerminalPluginPanel.buttonX(minecraft) + 52
               && mouseY >= TerminalPluginPanel.buttonY() && mouseY < TerminalPluginPanel.buttonY() + 14;
    }

    public boolean isOverPanel(Minecraft minecraft, double mouseX, double mouseY) {
        if (!this.open) {
            return false;
        }
        int x = this.left(minecraft);
        int y = this.top();
        return mouseX >= x && mouseX < x + TerminalPluginPanel.PANEL_WIDTH
               && mouseY >= y && mouseY < y + TerminalPluginPanel.PANEL_HEIGHT;
    }

    public boolean mouseClicked(Minecraft minecraft, double mouseX, double mouseY, int button) {
        for (Region region : this.regions) {
            if (region.contains(mouseX, mouseY)) {
                this.dispatch(region, button);
                return true;
            }
        }
        return this.isOverPanel(minecraft, mouseX, mouseY);
    }

    private void dispatch(Region region, int button) {
        if (region.action() == Action.CLOSE) {
            this.close();
            return;
        }
        if (region.action() == Action.SELECT) {
            this.selected = region.pluginIndex();
            return;
        }
        if (this.target == null) {
            return;
        }
        ItemStack filter = ItemStack.EMPTY;
        if (region.action() == Action.SET_ALCHEMY_FILTER) {
            Minecraft minecraft = Minecraft.getInstance();
            ItemStack carried = minecraft.player == null ? ItemStack.EMPTY : minecraft.player.containerMenu.getCarried();
            filter = button == 1 || carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1);
        }
        int action = switch (region.action()) {
            case CYCLE_PRIMARY -> PluginActionPacket.CYCLE_PRIMARY;
            case CYCLE_SECONDARY -> PluginActionPacket.CYCLE_SECONDARY;
            case REMOVE -> PluginActionPacket.REMOVE;
            case MOVE_UP -> PluginActionPacket.MOVE_UP;
            case MOVE_DOWN -> PluginActionPacket.MOVE_DOWN;
            case TOGGLE -> PluginActionPacket.TOGGLE_ENABLED;
            case SET_ALCHEMY_FILTER -> PluginActionPacket.SET_ALCHEMY_FILTER;
            case CYCLE_ALCHEMY_TRIGGER -> PluginActionPacket.CYCLE_ALCHEMY_TRIGGER;
            case VALUE_UP, VALUE_DOWN -> PluginActionPacket.ADJUST_ALCHEMY_VALUE;
            case CYCLE_ALCHEMY_NEARBY -> PluginActionPacket.CYCLE_ALCHEMY_NEARBY;
            case RADIUS_UP, RADIUS_DOWN -> PluginActionPacket.ADJUST_ALCHEMY_RADIUS;
            default -> -1;
        };
        if (action < 0) {
            return;
        }
        float value = switch (region.action()) {
            case VALUE_UP -> 0.05F;
            case VALUE_DOWN -> -0.05F;
            case RADIUS_UP -> 1.0F;
            case RADIUS_DOWN -> -1.0F;
            default -> 0.0F;
        };
        PacketDistributor.sendToServer(new PluginActionPacket(
            this.target.station(), this.target.containerSlot(), action,
            region.pluginIndex(), region.entryIndex(), filter, value
        ));
    }

    public static int findHeldTerminalSlot() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return Integer.MIN_VALUE;
        }
        // 手持终端优先（即使还没装插件，也允许打开面板看空状态）
        if (TerminalPluginManager.isTerminal(minecraft.player.getMainHandItem())) {
            return -1;
        }
        if (TerminalPluginManager.isTerminal(minecraft.player.getOffhandItem())) {
            return -2;
        }
        Inventory inventory = minecraft.player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (TerminalPluginManager.isTerminal(inventory.getItem(slot))) {
                return slot;
            }
        }
        return Integer.MIN_VALUE;
    }

    public static ItemStack stackAt(int containerSlot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return ItemStack.EMPTY;
        }
        return switch (containerSlot) {
            case -1 -> minecraft.player.getMainHandItem();
            case -2 -> minecraft.player.getOffhandItem();
            default -> containerSlot >= 0 && containerSlot < minecraft.player.getInventory().getContainerSize()
                ? minecraft.player.getInventory().getItem(containerSlot)
                : ItemStack.EMPTY;
        };
    }

    public Target heldTarget() {
        int slot = TerminalPluginPanel.findHeldTerminalSlot();
        if (slot == Integer.MIN_VALUE) {
            return null;
        }
        return new Target(Optional.empty(), slot, () -> TerminalPluginPanel.stackAt(slot));
    }

    public Target stationTarget(BlockPos pos, Supplier<ItemStack> supplier) {
        return new Target(Optional.of(pos), -1, supplier);
    }

    // 画出面板与入口按钮的提示文字由调用方绘制；这里只画面板本体。
    public void render(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
        this.regions.clear();
        if (!this.open) {
            return;
        }
        int x = this.left(minecraft);
        int y = this.top();
        graphics.fill(x - 1, y - 1, x + TerminalPluginPanel.PANEL_WIDTH + 1, y + TerminalPluginPanel.PANEL_HEIGHT + 1, TerminalPluginPanel.COLOR_BORDER);
        graphics.fill(x, y, x + TerminalPluginPanel.PANEL_WIDTH, y + TerminalPluginPanel.PANEL_HEIGHT, TerminalPluginPanel.COLOR_PANEL);
        graphics.drawString(minecraft.font, Component.translatable("screen.anvilcraft_terminal_plugins.panel.title"), x + 4, y + 4, TerminalPluginPanel.COLOR_TEXT, false);
        graphics.drawString(minecraft.font, "x", x + TerminalPluginPanel.PANEL_WIDTH - 11, y + 5, TerminalPluginPanel.COLOR_DIM, false);
        this.regions.add(new Region(x + TerminalPluginPanel.PANEL_WIDTH - 14, y + 2, 12, 12, Action.CLOSE, -1, -1));

        ItemStack terminal = this.terminal();
        List<ItemStack> plugins = TerminalPluginManager.installed(terminal);
        if (plugins.isEmpty()) {
            graphics.drawString(minecraft.font, Component.translatable("screen.anvilcraft_terminal_plugins.panel.empty"), x + 4, y + 20, TerminalPluginPanel.COLOR_DIM, false);
            return;
        }
        this.selected = Math.clamp(this.selected, 0, plugins.size() - 1);

        int rowY = y + 18;
        for (int index = 0; index < plugins.size(); index++) {
            ItemStack plugin = plugins.get(index);
            boolean hovered = mouseX >= x + 2 && mouseX < x + TerminalPluginPanel.PANEL_WIDTH - 2
                              && mouseY >= rowY && mouseY < rowY + 14;
            if (index == this.selected) {
                graphics.fill(x + 2, rowY, x + TerminalPluginPanel.PANEL_WIDTH - 2, rowY + 14, TerminalPluginPanel.COLOR_SELECTED);
            } else if (hovered) {
                graphics.fill(x + 2, rowY, x + TerminalPluginPanel.PANEL_WIDTH - 2, rowY + 14, TerminalPluginPanel.COLOR_BUTTON_HOVER);
            }
            graphics.renderItem(plugin, x + 4, rowY - 1);
            boolean enabled = TerminalPluginManager.isEnabled(plugin);
            String name = plugin.getHoverName().getString();
            graphics.drawString(minecraft.font, name, x + 24, rowY + 3, enabled ? TerminalPluginPanel.COLOR_TEXT : TerminalPluginPanel.COLOR_DIM, false);
            if (!enabled) {
                graphics.drawString(minecraft.font, "off", x + TerminalPluginPanel.PANEL_WIDTH - 20, rowY + 3, TerminalPluginPanel.COLOR_DIM, false);
            }
            this.regions.add(new Region(x + 2, rowY, TerminalPluginPanel.PANEL_WIDTH - 4, 14, Action.SELECT, index, -1));
            rowY += 14;
        }

        ItemStack selectedPlugin = plugins.get(this.selected);
        int buttonY = rowY + 2;
        int buttonX = x + 3;
        buttonX = this.button(graphics, minecraft, buttonX, buttonY, 20, "screen.anvilcraft_terminal_plugins.panel.primary", Action.CYCLE_PRIMARY, this.selected, -1, mouseX, mouseY);
        buttonX = this.button(graphics, minecraft, buttonX, buttonY, 20, "screen.anvilcraft_terminal_plugins.panel.secondary", Action.CYCLE_SECONDARY, this.selected, -1, mouseX, mouseY);
        buttonX = this.button(graphics, minecraft, buttonX, buttonY, 14, "screen.anvilcraft_terminal_plugins.panel.up", Action.MOVE_UP, this.selected, -1, mouseX, mouseY);
        buttonX = this.button(graphics, minecraft, buttonX, buttonY, 14, "screen.anvilcraft_terminal_plugins.panel.down", Action.MOVE_DOWN, this.selected, -1, mouseX, mouseY);
        buttonX = this.button(graphics, minecraft, buttonX, buttonY, 22, TerminalPluginManager.isEnabled(selectedPlugin)
            ? "screen.anvilcraft_terminal_plugins.panel.disable"
            : "screen.anvilcraft_terminal_plugins.panel.enable", Action.TOGGLE, this.selected, -1, mouseX, mouseY);
        this.button(graphics, minecraft, buttonX, buttonY, 20, "screen.anvilcraft_terminal_plugins.panel.remove", Action.REMOVE, this.selected, -1, mouseX, mouseY);
        rowY = buttonY + 16;

        if (selectedPlugin.getItem() instanceof TerminalPluginItem item && item.kind() == PluginKind.ALCHEMY) {
            AlchemySettings settings = selectedPlugin.getOrDefault(AddonDataComponents.ALCHEMY_SETTINGS, AlchemySettings.DEFAULT);
            graphics.drawString(minecraft.font, Component.translatable("screen.anvilcraft_terminal_plugins.panel.alchemy"), x + 4, rowY + 1, TerminalPluginPanel.COLOR_DIM, false);
            rowY += 12;
            List<AlchemySettings.AlchemyEntry> entries = settings.normalizedEntries();
            for (int index = 0; index < entries.size(); index++) {
                AlchemySettings.AlchemyEntry entry = entries.get(index);
                graphics.fill(x + 4, rowY, x + 22, rowY + 18, TerminalPluginPanel.COLOR_SLOT);
                if (!entry.filter().isEmpty()) {
                    graphics.renderItem(entry.filter(), x + 5, rowY + 1);
                }
                this.regions.add(new Region(x + 4, rowY, 18, 18, Action.SET_ALCHEMY_FILTER, this.selected, index));
                String trigger = Component.translatable(
                    "tooltip.anvilcraft_terminal_plugins.alchemy.trigger." + entry.trigger().getSerializedName()
                ).getString();
                this.buttonText(graphics, minecraft, x + 24, rowY + 2, 66, trigger, Action.CYCLE_ALCHEMY_TRIGGER, this.selected, index, mouseX, mouseY);
                if (entry.trigger().usesValue()) {
                    this.buttonText(graphics, minecraft, x + 92, rowY + 2, 12, "-", Action.VALUE_DOWN, this.selected, index, mouseX, mouseY);
                    graphics.drawString(minecraft.font, String.format("%.2f", entry.value()), x + 106, rowY + 6, TerminalPluginPanel.COLOR_TEXT, false);
                    this.buttonText(graphics, minecraft, x + 134, rowY + 2, 12, "+", Action.VALUE_UP, this.selected, index, mouseX, mouseY);
                }
                rowY += 20;
            }
            String nearby = Component.translatable(
                "tooltip.anvilcraft_terminal_plugins.alchemy.nearby." + settings.nearby().getSerializedName()
            ).getString();
            this.buttonText(graphics, minecraft, x + 4, rowY, 70, nearby, Action.CYCLE_ALCHEMY_NEARBY, this.selected, -1, mouseX, mouseY);
            this.buttonText(graphics, minecraft, x + 78, rowY, 12, "-", Action.RADIUS_DOWN, this.selected, -1, mouseX, mouseY);
            graphics.drawString(minecraft.font, String.valueOf(settings.radius()), x + 94, rowY + 2, TerminalPluginPanel.COLOR_TEXT, false);
            this.buttonText(graphics, minecraft, x + 106, rowY, 12, "+", Action.RADIUS_UP, this.selected, -1, mouseX, mouseY);
        }
    }

    private int button(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, String translationKey,
                       Action action, int pluginIndex, int entryIndex, int mouseX, int mouseY) {
        return this.buttonText(graphics, minecraft, x, y, width, Component.translatable(translationKey).getString(),
            action, pluginIndex, entryIndex, mouseX, mouseY);
    }

    private int buttonText(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, String text,
                           Action action, int pluginIndex, int entryIndex, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 12;
        graphics.fill(x, y, x + width, y + 12, hovered ? TerminalPluginPanel.COLOR_BUTTON_HOVER : TerminalPluginPanel.COLOR_BUTTON);
        String label = minecraft.font.width(text) > width - 2 ? minecraft.font.plainSubstrByWidth(text, width - 2) : text;
        graphics.drawString(minecraft.font, label, x + (width - minecraft.font.width(label)) / 2, y + 2, TerminalPluginPanel.COLOR_TEXT, false);
        this.regions.add(new Region(x, y, width, 12, action, pluginIndex, entryIndex));
        return x + width + 2;
    }
}
