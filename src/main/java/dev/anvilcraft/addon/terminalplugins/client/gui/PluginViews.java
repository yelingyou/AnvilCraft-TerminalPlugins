package dev.anvilcraft.addon.terminalplugins.client.gui;

import dev.anvilcraft.addon.terminalplugins.component.AlchemySettings;
import dev.anvilcraft.addon.terminalplugins.component.AnvilRepairSettings;
import dev.anvilcraft.addon.terminalplugins.component.AutoCookingSettings;
import dev.anvilcraft.addon.terminalplugins.component.CompactingSettings;
import dev.anvilcraft.addon.terminalplugins.component.VoidSettings;
import dev.anvilcraft.addon.terminalplugins.component.FeedingSettings;
import dev.anvilcraft.addon.terminalplugins.component.FluidSettings;
import dev.anvilcraft.addon.terminalplugins.component.MagnetSettings;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.network.PluginActionPacket;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginKind;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

// 各插件的设置子页实现。新增插件时在这里加一个内部类即可。
public final class PluginViews {
    private PluginViews() {
    }

    public static PluginSettingsView of(PluginKind kind) {
        return switch (kind) {
            case FILTER -> new FilterView();
            case MAGNET -> new MagnetView();
            case AUTO_COOKING -> new CookingView();
            case FEEDING -> new FeedingView();
            case ALCHEMY -> new AlchemyView();
            case VOID -> new VoidView();
            case COMPACTING -> new CompactingView();
            case ANVIL_REPAIR -> new AnvilRepairView();
            case FLUID -> new FluidView();
        };
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    private static String on() {
        return PluginViews.tr("screen.anvilcraft_terminal_plugins.setting.on");
    }

    private static String off() {
        return PluginViews.tr("screen.anvilcraft_terminal_plugins.setting.off");
    }

    // 过滤：白/黑名单 + 是否比对组件 + 18 格过滤表
    private static final class FilterView implements PluginSettingsView {
        private static final int COLUMNS = 6;
        private static final int SLOTS = 18;

        @Override
        public int height(ItemStack plugin) {
            return 16 + 3 * 18;
        }

        @Override
        public void render(PluginSettingsView.Ctx ctx, GuiGraphics graphics, Minecraft minecraft, ItemStack plugin,
                           int pluginIndex, int x, int y, int width, int mouseX, int mouseY) {
            FilterContent content = plugin.getOrDefault(ModComponents.FILTER_CONTENT, new FilterContent());
            ctx.settingButton(graphics, minecraft, x, y, width, PluginViews.tr(
                content.blackList() ? "tooltip.anvilcraft_terminal_plugins.filter.mode.blacklist"
                    : "tooltip.anvilcraft_terminal_plugins.filter.mode.whitelist"),
                pluginIndex, -1, PluginActionPacket.CYCLE_PRIMARY, mouseX, mouseY,
                "screen.anvilcraft_terminal_plugins.panel.cycle_tip");
            ctx.settingButton(graphics, minecraft, x, y + 14, width, PluginViews.tr(
                "screen.anvilcraft_terminal_plugins.setting.components",
                content.includeComponents() ? PluginViews.on() : PluginViews.off()),
                pluginIndex, -1, PluginActionPacket.CYCLE_SECONDARY, mouseX, mouseY,
                "screen.anvilcraft_terminal_plugins.panel.cycle_tip");
            int gridY = y + 30;
            for (int slot = 0; slot < FilterView.SLOTS; slot++) {
                int column = slot % FilterView.COLUMNS;
                int row = slot / FilterView.COLUMNS;
                ItemStack shown = slot < content.list().size() ? content.list().get(slot) : ItemStack.EMPTY;
                ctx.ghostSlot(graphics, x + column * 18, gridY + row * 18, shown, pluginIndex, slot,
                    PluginActionPacket.SET_FILTER_SLOT, "screen.anvilcraft_terminal_plugins.panel.filter_slot_tip");
            }
        }
    }

    // 磁吸 / 拾取：半径（步进）+ 模式（循环）
    private static final class MagnetView implements PluginSettingsView {
        @Override
        public int height(ItemStack plugin) {
            return 28;
        }

        @Override
        public void render(PluginSettingsView.Ctx ctx, GuiGraphics graphics, Minecraft minecraft, ItemStack plugin,
                           int pluginIndex, int x, int y, int width, int mouseX, int mouseY) {
            MagnetSettings settings = plugin.getOrDefault(AddonDataComponents.MAGNET_SETTINGS, MagnetSettings.DEFAULT);
            ctx.smallButton(graphics, minecraft, x, y, 12, "-", pluginIndex, -1,
                PluginActionPacket.ADJUST_MAGNET_RANGE, mouseX, mouseY);
            ctx.pendingValue(-1.0F);
            ctx.settingButton(graphics, minecraft, x + 14, y, width - 28, PluginViews.tr(
                "screen.anvilcraft_terminal_plugins.setting.range", settings.range()),
                pluginIndex, -1, PluginActionPacket.CYCLE_PRIMARY, mouseX, mouseY,
                "screen.anvilcraft_terminal_plugins.panel.cycle_tip");
            ctx.smallButton(graphics, minecraft, x + width - 12, y, 12, "+", pluginIndex, -1,
                PluginActionPacket.ADJUST_MAGNET_RANGE, mouseX, mouseY);
            ctx.pendingValue(1.0F);
            ctx.settingButton(graphics, minecraft, x, y + 14, width, PluginViews.tr(
                "screen.anvilcraft_terminal_plugins.setting.mode", PluginViews.magnetMode(settings)),
                pluginIndex, -1, PluginActionPacket.CYCLE_SECONDARY, mouseX, mouseY,
                "screen.anvilcraft_terminal_plugins.panel.cycle_tip");
        }
    }

    // 自动熔炼 · 烹饪：配方（循环）+ 燃料（开关）
    private static final class CookingView implements PluginSettingsView {
        @Override
        public int height(ItemStack plugin) {
            return 28;
        }

        @Override
        public void render(PluginSettingsView.Ctx ctx, GuiGraphics graphics, Minecraft minecraft, ItemStack plugin,
                           int pluginIndex, int x, int y, int width, int mouseX, int mouseY) {
            AutoCookingSettings settings = plugin.getOrDefault(
                AddonDataComponents.COOKING_SETTINGS, AutoCookingSettings.DEFAULT);
            ctx.settingButton(graphics, minecraft, x, y, width, PluginViews.tr(
                "screen.anvilcraft_terminal_plugins.setting.recipe",
                PluginViews.tr("tooltip.anvilcraft_terminal_plugins.cooking.mode."
                    + settings.mode().getSerializedName())),
                pluginIndex, -1, PluginActionPacket.CYCLE_PRIMARY, mouseX, mouseY,
                "screen.anvilcraft_terminal_plugins.panel.cycle_tip");
            ctx.settingButton(graphics, minecraft, x, y + 14, width, PluginViews.tr(
                "screen.anvilcraft_terminal_plugins.setting.fuel", settings.consumeFuel()
                    ? PluginViews.on() : PluginViews.off()),
                pluginIndex, -1, PluginActionPacket.CYCLE_SECONDARY, mouseX, mouseY,
                "screen.anvilcraft_terminal_plugins.panel.cycle_tip");
        }
    }

    // 自动喂食：阈值（步进）+ 是否允许负面食物（开关）
    private static final class FeedingView implements PluginSettingsView {
        @Override
        public int height(ItemStack plugin) {
            return 28;
        }

        @Override
        public void render(PluginSettingsView.Ctx ctx, GuiGraphics graphics, Minecraft minecraft, ItemStack plugin,
                           int pluginIndex, int x, int y, int width, int mouseX, int mouseY) {
            FeedingSettings settings = plugin.getOrDefault(
                AddonDataComponents.FEEDING_SETTINGS, FeedingSettings.DEFAULT);
            ctx.smallButton(graphics, minecraft, x, y, 12, "-", pluginIndex, -1,
                PluginActionPacket.ADJUST_FEEDING_THRESHOLD, mouseX, mouseY);
            ctx.pendingValue(-1.0F);
            ctx.settingButton(graphics, minecraft, x + 14, y, width - 28, PluginViews.tr(
                "screen.anvilcraft_terminal_plugins.setting.threshold", settings.hungerThreshold()),
                pluginIndex, -1, PluginActionPacket.CYCLE_PRIMARY, mouseX, mouseY,
                "screen.anvilcraft_terminal_plugins.panel.cycle_tip");
            ctx.smallButton(graphics, minecraft, x + width - 12, y, 12, "+", pluginIndex, -1,
                PluginActionPacket.ADJUST_FEEDING_THRESHOLD, mouseX, mouseY);
            ctx.pendingValue(1.0F);
            ctx.settingButton(graphics, minecraft, x, y + 14, width, PluginViews.tr(
                "screen.anvilcraft_terminal_plugins.setting.harmful", settings.allowHarmful()
                    ? PluginViews.on() : PluginViews.off()),
                pluginIndex, -1, PluginActionPacket.CYCLE_SECONDARY, mouseX, mouseY,
                "screen.anvilcraft_terminal_plugins.panel.cycle_tip");
        }
    }

    // 炼金：作用范围 + 等级匹配 + 4 个条目（药水槽 / 条件 / 阈值）+ 作用半径
    private static final class AlchemyView implements PluginSettingsView {
        @Override
        public int height(ItemStack plugin) {
            return 28 + 11 + AlchemySettings.ENTRY_COUNT * 18 + 16;
        }

        @Override
        public void render(PluginSettingsView.Ctx ctx, GuiGraphics graphics, Minecraft minecraft, ItemStack plugin,
                           int pluginIndex, int x, int y, int width, int mouseX, int mouseY) {
            AlchemySettings settings = plugin.getOrDefault(
                AddonDataComponents.ALCHEMY_SETTINGS, AlchemySettings.DEFAULT);
            ctx.settingButton(graphics, minecraft, x, y, width, PluginViews.tr(
                "screen.anvilcraft_terminal_plugins.setting.nearby",
                PluginViews.tr("tooltip.anvilcraft_terminal_plugins.alchemy.nearby."
                    + settings.nearby().getSerializedName())),
                pluginIndex, -1, PluginActionPacket.CYCLE_ALCHEMY_NEARBY, mouseX, mouseY,
                "screen.anvilcraft_terminal_plugins.panel.cycle_tip");
            ctx.settingButton(graphics, minecraft, x, y + 14, width, PluginViews.tr(
                "screen.anvilcraft_terminal_plugins.setting.amplifier", settings.matchAmplifier()
                    ? PluginViews.on() : PluginViews.off()),
                pluginIndex, -1, PluginActionPacket.CYCLE_SECONDARY, mouseX, mouseY,
                "screen.anvilcraft_terminal_plugins.panel.cycle_tip");

            graphics.drawString(minecraft.font,
                Component.translatable("screen.anvilcraft_terminal_plugins.panel.alchemy"), x, y + 30,
                0xFF9A9AA4, false);
            List<AlchemySettings.AlchemyEntry> entries = settings.normalizedEntries();
            int rowY = y + 41;
            for (int index = 0; index < entries.size(); index++) {
                AlchemySettings.AlchemyEntry entry = entries.get(index);
                ctx.ghostSlot(graphics, x, rowY, entry.filter(), pluginIndex, index,
                    PluginActionPacket.SET_ALCHEMY_FILTER, "screen.anvilcraft_terminal_plugins.panel.filter_tip");
                String trigger = PluginViews.tr(
                    "tooltip.anvilcraft_terminal_plugins.alchemy.trigger." + entry.trigger().getSerializedName());
                ctx.settingButton(graphics, minecraft, x + 18, rowY + 2, width - 18 - 26, trigger,
                    pluginIndex, index, PluginActionPacket.CYCLE_ALCHEMY_TRIGGER, mouseX, mouseY,
                    "screen.anvilcraft_terminal_plugins.panel.trigger_tip");
                if (entry.trigger().usesValue()) {
                    ctx.smallButton(graphics, minecraft, x + width - 26, rowY + 2, 12, "-",
                        pluginIndex, index, PluginActionPacket.ADJUST_ALCHEMY_VALUE, mouseX, mouseY);
                    ctx.pendingValue(-0.05F);
                    ctx.smallButton(graphics, minecraft, x + width - 12, rowY + 2, 12, "+",
                        pluginIndex, index, PluginActionPacket.ADJUST_ALCHEMY_VALUE, mouseX, mouseY);
                    ctx.pendingValue(0.05F);
                }
                rowY += 18;
            }
            ctx.smallButton(graphics, minecraft, x, rowY + 1, 12, "-", pluginIndex, -1,
                PluginActionPacket.ADJUST_ALCHEMY_RADIUS, mouseX, mouseY);
            ctx.pendingValue(-1.0F);
            ctx.settingButton(graphics, minecraft, x + 14, rowY + 1, width - 28, PluginViews.tr(
                "screen.anvilcraft_terminal_plugins.setting.range", settings.radius()),
                pluginIndex, -1, PluginActionPacket.ADJUST_ALCHEMY_RADIUS, mouseX, mouseY,
                "screen.anvilcraft_terminal_plugins.panel.radius_tip");
            ctx.pendingValue(1.0F);
            ctx.smallButton(graphics, minecraft, x + width - 12, rowY + 1, 12, "+", pluginIndex, -1,
                PluginActionPacket.ADJUST_ALCHEMY_RADIUS, mouseX, mouseY);
            ctx.pendingValue(1.0F);
        }
    }

    // 共用的 6x3 过滤网格
    private static void drawFilterGrid(PluginSettingsView.Ctx ctx, GuiGraphics graphics, ItemStack plugin,
                                        int pluginIndex, int x, int y, int mouseX, int mouseY) {
        FilterContent content = plugin.getOrDefault(ModComponents.FILTER_CONTENT, new FilterContent());
        for (int slot = 0; slot < 18; slot++) {
            int column = slot % 6;
            int row = slot / 6;
            ItemStack shown = slot < content.list().size() ? content.list().get(slot) : ItemStack.EMPTY;
            ctx.ghostSlot(graphics, x + column * 18, y + row * 18, shown, pluginIndex, slot,
                PluginActionPacket.SET_FILTER_SLOT, "screen.anvilcraft_terminal_plugins.panel.filter_slot_tip");
        }
    }


    // 销毁：过滤表 + 保留组数
    private static final class VoidView implements PluginSettingsView {
        private static final int[] KEEP_STACKS = {0, 1, 2, 4, 8, 16, 64};

        @Override
        public int height(ItemStack plugin) {
            return 14 + 3 * 18;
        }

        @Override
        public void render(PluginSettingsView.Ctx ctx, GuiGraphics graphics, Minecraft minecraft, ItemStack plugin,
                           int pluginIndex, int x, int y, int width, int mouseX, int mouseY) {
            VoidSettings settings = plugin.getOrDefault(AddonDataComponents.VOID_SETTINGS, VoidSettings.DEFAULT);
            ctx.settingButton(graphics, minecraft, x, y, width, PluginViews.tr(
                "screen.anvilcraft_terminal_plugins.setting.keep_stacks", settings.keepStacks()),
                pluginIndex, -1, PluginActionPacket.CYCLE_PRIMARY, mouseX, mouseY,
                "screen.anvilcraft_terminal_plugins.panel.cycle_tip");
            PluginViews.drawFilterGrid(ctx, graphics, plugin, pluginIndex, x, y + 14, mouseX, mouseY);
        }
    }

    // 压缩：每次处理组数 + 过滤表
    private static final class CompactingView implements PluginSettingsView {
        @Override
        public int height(ItemStack plugin) {
            return 14 + 3 * 18;
        }

        @Override
        public void render(PluginSettingsView.Ctx ctx, GuiGraphics graphics, Minecraft minecraft, ItemStack plugin,
                           int pluginIndex, int x, int y, int width, int mouseX, int mouseY) {
            CompactingSettings settings = plugin.getOrDefault(
                AddonDataComponents.COMPACTING_SETTINGS, CompactingSettings.DEFAULT);
            ctx.settingButton(graphics, minecraft, x, y, width, PluginViews.tr(
                "screen.anvilcraft_terminal_plugins.setting.batch", settings.batch()),
                pluginIndex, -1, PluginActionPacket.CYCLE_PRIMARY, mouseX, mouseY,
                "screen.anvilcraft_terminal_plugins.panel.cycle_tip");
            PluginViews.drawFilterGrid(ctx, graphics, plugin, pluginIndex, x, y + 14, mouseX, mouseY);
        }
    }

    // 铁砧修复：每次修复点数 + 过滤表（材料）
    private static final class AnvilRepairView implements PluginSettingsView {
        @Override
        public int height(ItemStack plugin) {
            return 14 + 3 * 18;
        }

        @Override
        public void render(PluginSettingsView.Ctx ctx, GuiGraphics graphics, Minecraft minecraft, ItemStack plugin,
                           int pluginIndex, int x, int y, int width, int mouseX, int mouseY) {
            AnvilRepairSettings settings = plugin.getOrDefault(
                AddonDataComponents.ANVIL_REPAIR_SETTINGS, AnvilRepairSettings.DEFAULT);
            String label = settings.repairPerMaterial() == 0
                ? PluginViews.tr("screen.anvilcraft_terminal_plugins.setting.repair_quarter")
                : PluginViews.tr("screen.anvilcraft_terminal_plugins.setting.repair_amount", settings.repairPerMaterial());
            ctx.settingButton(graphics, minecraft, x, y, width, label,
                pluginIndex, -1, PluginActionPacket.CYCLE_PRIMARY, mouseX, mouseY,
                "screen.anvilcraft_terminal_plugins.panel.cycle_tip");
            PluginViews.drawFilterGrid(ctx, graphics, plugin, pluginIndex, x, y + 14, mouseX, mouseY);
        }
    }

    // 流体接口：工作模式 / 每周期批数 / 缓冲容量 + 过滤表（筛可用的容器）
    private static final class FluidView implements PluginSettingsView {
        @Override
        public int height(ItemStack plugin) {
            return 3 * 18 + 6 + 54;
        }

        @Override
        public void render(PluginSettingsView.Ctx ctx, GuiGraphics graphics, Minecraft minecraft, ItemStack plugin,
                           int pluginIndex, int x, int y, int width, int mouseX, int mouseY) {
            FluidSettings settings = plugin.getOrDefault(
                AddonDataComponents.FLUID_SETTINGS, FluidSettings.DEFAULT);
            ctx.settingButton(graphics, minecraft, x, y, width, PluginViews.tr(
                "screen.anvilcraft_terminal_plugins.setting.fluid_mode",
                PluginViews.tr("screen.anvilcraft_terminal_plugins.fluid_mode."
                    + settings.mode().getSerializedName())),
                pluginIndex, -1, PluginActionPacket.CYCLE_PRIMARY, mouseX, mouseY,
                "screen.anvilcraft_terminal_plugins.panel.cycle_tip");
            int rowY = y + 18;
            ctx.smallButton(graphics, minecraft, x, rowY, 12, "-", pluginIndex, -1,
                PluginActionPacket.ADJUST_FLUID_BATCH, mouseX, mouseY);
            ctx.pendingValue(-1.0F);
            ctx.settingButton(graphics, minecraft, x + 14, rowY, width - 28, PluginViews.tr(
                "screen.anvilcraft_terminal_plugins.setting.fluid_batch", settings.batch()),
                pluginIndex, -1, PluginActionPacket.ADJUST_FLUID_BATCH, mouseX, mouseY,
                "screen.anvilcraft_terminal_plugins.panel.cycle_tip");
            ctx.pendingValue(1.0F);
            ctx.smallButton(graphics, minecraft, x + width - 12, rowY, 12, "+", pluginIndex, -1,
                PluginActionPacket.ADJUST_FLUID_BATCH, mouseX, mouseY);
            ctx.pendingValue(1.0F);
            rowY += 18;
            ctx.smallButton(graphics, minecraft, x, rowY, 12, "-", pluginIndex, -1,
                PluginActionPacket.ADJUST_FLUID_CAPACITY, mouseX, mouseY);
            ctx.pendingValue(-1.0F);
            ctx.settingButton(graphics, minecraft, x + 14, rowY, width - 28, PluginViews.tr(
                "screen.anvilcraft_terminal_plugins.setting.fluid_capacity", settings.capacityBuckets()),
                pluginIndex, -1, PluginActionPacket.ADJUST_FLUID_CAPACITY, mouseX, mouseY,
                "screen.anvilcraft_terminal_plugins.panel.cycle_tip");
            ctx.pendingValue(1.0F);
            ctx.smallButton(graphics, minecraft, x + width - 12, rowY, 12, "+", pluginIndex, -1,
                PluginActionPacket.ADJUST_FLUID_CAPACITY, mouseX, mouseY);
            ctx.pendingValue(1.0F);
            PluginViews.drawFilterGrid(ctx, graphics, plugin, pluginIndex, x, y + 3 * 18 + 6, mouseX, mouseY);
        }
    }

    private static String magnetMode(MagnetSettings settings) {
        if (settings.magnetEnabled() && settings.pickupEnabled()) {
            return PluginViews.tr("tooltip.anvilcraft_terminal_plugins.magnet.mode.both");
        }
        if (settings.magnetEnabled()) {
            return PluginViews.tr("tooltip.anvilcraft_terminal_plugins.magnet.mode.magnet");
        }
        return PluginViews.tr("tooltip.anvilcraft_terminal_plugins.magnet.mode.pickup");
    }
}
