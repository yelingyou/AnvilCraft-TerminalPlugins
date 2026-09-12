/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for license text.
 */
package dev.anvilcraft.addon.terminalplugins.client.gui;

import dev.anvilcraft.addon.terminalplugins.component.AnvilProcessSettings;
import dev.anvilcraft.addon.terminalplugins.component.ChargingSettings;
import dev.anvilcraft.addon.terminalplugins.component.CompactingSettings;
import dev.anvilcraft.addon.terminalplugins.component.PluginSample;
import dev.anvilcraft.addon.terminalplugins.component.SmithingSettings;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.network.PluginActionPacket;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * 会执行配方的插件的「执行」子页。
 *
 * <p>执行相关的东西（加工方式、批量、输入槽、输出槽、开始加工）**不再放在设置里**：
 * 插件列表行上「设置」旁边多一个「执行」，两页各管各的 —— 设置只放配置项，执行只管加工。</p>
 *
 * <p>输入槽 = {@code PLUGIN_SAMPLE.sample}：左键放入光标上的物品、右键清空；留空表示不指定。
 * 输出槽 = {@code PLUGIN_SAMPLE.lastOutput}：服务端每次成功加工后写进来。</p>
 */
public final class PluginExecViews {
    private PluginExecViews() {
    }

    /** 没有「执行」页的插件返回 {@code null}。 */
    public static PluginSettingsView of(PluginKind kind) {
        return switch (kind) {
            case ANVIL_PROCESS -> new AnvilProcessExecView();
            case SMITHING -> new SmithingExecView();
            case COMPACTING -> new CompactingExecView();
            case CHARGING -> new ChargingExecView();
            default -> null;
        };
    }

    public static boolean has(PluginKind kind) {
        return PluginExecViews.of(kind) != null;
    }

    /**
     * 输入槽 + 输出槽。输入槽可点，输出槽只读。
     *
     * @return 画完这一行之后下一个控件应该用的 Y
     */
    static int slots(PluginSettingsView.Ctx ctx, GuiGraphics graphics, Minecraft minecraft, ItemStack plugin,
                     int pluginIndex, int x, int y) {
        PluginSample sample = plugin.getOrDefault(AddonDataComponents.PLUGIN_SAMPLE, PluginSample.EMPTY);
        graphics.drawString(minecraft.font, PluginExecViews.tr("screen.anvilcraft_terminal_plugins.exec.input"),
            x, y + 5, 0xFF9A9AA4, false);
        ctx.ghostSlot(graphics, x + 28, y, sample.sample(), pluginIndex, -1,
            PluginActionPacket.SET_SAMPLE_SLOT, "screen.anvilcraft_terminal_plugins.panel.input_slot_tip");
        graphics.drawString(minecraft.font, PluginExecViews.tr("screen.anvilcraft_terminal_plugins.exec.output"),
            x + 50, y + 5, 0xFF9A9AA4, false);
        graphics.fill(x + 78, y, x + 94, y + 16, 0xFF4E7A4E);
        graphics.fill(x + 79, y + 1, x + 93, y + 15, 0xFF2A3329);
        if (!sample.lastOutput().isEmpty()) {
            graphics.renderItem(sample.lastOutput(), x + 79, y + 1);
        }
        return y + 20;
    }

    private static String tr(String key, Object... args) {
        return net.minecraft.network.chat.Component.translatable(key, args).getString();
    }

    /** 铁砧加工：加工方式 / 批量 / 输入输出槽 / 开始加工。 */
    private static final class AnvilProcessExecView implements PluginSettingsView {
        @Override
        public int height(ItemStack plugin) {
            return 3 * 14 + 4 + 20 + 4;
        }

        @Override
        public void render(PluginSettingsView.Ctx ctx, GuiGraphics graphics, Minecraft minecraft, ItemStack plugin,
                           int pluginIndex, int x, int y, int width, int mouseX, int mouseY) {
            AnvilProcessSettings settings = plugin.getOrDefault(
                AddonDataComponents.ANVIL_PROCESS_SETTINGS, AnvilProcessSettings.DEFAULT);
            ctx.settingButton(graphics, minecraft, x, y, width, PluginExecViews.tr(
                "screen.anvilcraft_terminal_plugins.setting.process",
                PluginExecViews.tr("screen.anvilcraft_terminal_plugins.anvil_process."
                    + settings.process().getSerializedName())),
                pluginIndex, -1, PluginActionPacket.CYCLE_PRIMARY, mouseX, mouseY,
                "screen.anvilcraft_terminal_plugins.panel.process_tip");
            ctx.settingButton(graphics, minecraft, x, y + 14, width, PluginExecViews.tr(
                "screen.anvilcraft_terminal_plugins.setting.process_batch", settings.batch()),
                pluginIndex, -1, PluginActionPacket.CYCLE_SECONDARY, mouseX, mouseY,
                "screen.anvilcraft_terminal_plugins.panel.cycle_tip");
            int slotY = PluginExecViews.slots(ctx, graphics, minecraft, plugin, pluginIndex, x, y + 30);
            ctx.settingButton(graphics, minecraft, x, slotY, width, PluginExecViews.tr(
                "screen.anvilcraft_terminal_plugins.setting.process_now"),
                pluginIndex, -1, PluginActionPacket.ANVIL_PROCESS_NOW, mouseX, mouseY,
                "screen.anvilcraft_terminal_plugins.panel.process_now_tip");
        }
    }

    /** 锻造：批次 / 输入输出槽 / 开始锻造（模板不消耗，同皇家锻造台）。 */
    private static final class SmithingExecView implements PluginSettingsView {
        @Override
        public int height(ItemStack plugin) {
            return 2 * 14 + 4 + 20 + 4;
        }

        @Override
        public void render(PluginSettingsView.Ctx ctx, GuiGraphics graphics, Minecraft minecraft, ItemStack plugin,
                           int pluginIndex, int x, int y, int width, int mouseX, int mouseY) {
            SmithingSettings settings = plugin.getOrDefault(
                AddonDataComponents.SMITHING_SETTINGS, SmithingSettings.DEFAULT);
            ctx.settingButton(graphics, minecraft, x, y, width, PluginExecViews.tr(
                "screen.anvilcraft_terminal_plugins.setting.batch", settings.batch()),
                pluginIndex, -1, PluginActionPacket.CYCLE_PRIMARY, mouseX, mouseY,
                "screen.anvilcraft_terminal_plugins.panel.smithing_tip");
            int slotY = PluginExecViews.slots(ctx, graphics, minecraft, plugin, pluginIndex, x, y + 16);
            ctx.settingButton(graphics, minecraft, x, slotY, width, PluginExecViews.tr(
                "screen.anvilcraft_terminal_plugins.setting.smithing_now"),
                pluginIndex, -1, PluginActionPacket.SMITHING_NOW, mouseX, mouseY,
                "screen.anvilcraft_terminal_plugins.panel.smithing_now_tip");
        }
    }

    /** 压缩：自动执行，这里只放输入输出槽 + 提示。 */
    private static final class CompactingExecView implements PluginSettingsView {
        @Override
        public int height(ItemStack plugin) {
            return 20 + 14;
        }

        @Override
        public void render(PluginSettingsView.Ctx ctx, GuiGraphics graphics, Minecraft minecraft, ItemStack plugin,
                           int pluginIndex, int x, int y, int width, int mouseX, int mouseY) {
            CompactingSettings settings = plugin.getOrDefault(
                AddonDataComponents.COMPACTING_SETTINGS, CompactingSettings.DEFAULT);
            int slotY = PluginExecViews.slots(ctx, graphics, minecraft, plugin, pluginIndex, x, y);
            graphics.drawString(minecraft.font, PluginExecViews.tr(
                "screen.anvilcraft_terminal_plugins.exec.auto", settings.batch()),
                x, slotY + 2, 0xFF9A9AA4, false);
        }
    }

    /** 充能：自动执行，这里放输入输出槽 + 充能进度。 */
    private static final class ChargingExecView implements PluginSettingsView {
        @Override
        public int height(ItemStack plugin) {
            return 20 + 14;
        }

        @Override
        public void render(PluginSettingsView.Ctx ctx, GuiGraphics graphics, Minecraft minecraft, ItemStack plugin,
                           int pluginIndex, int x, int y, int width, int mouseX, int mouseY) {
            ChargingSettings settings = plugin.getOrDefault(
                AddonDataComponents.CHARGING_SETTINGS, ChargingSettings.DEFAULT);
            PluginExecViews.slots(ctx, graphics, minecraft, plugin, pluginIndex, x, y);
            String progress = settings.activeRecipe().isEmpty()
                ? PluginExecViews.tr("screen.anvilcraft_terminal_plugins.panel.charging_idle")
                : PluginExecViews.tr(
                    "screen.anvilcraft_terminal_plugins.panel.charging_progress",
                    settings.activeRecipe(),
                    settings.progress());
            graphics.drawString(minecraft.font, progress, x, y + 22, 0xFF9A9AA4, false);
        }
    }
}