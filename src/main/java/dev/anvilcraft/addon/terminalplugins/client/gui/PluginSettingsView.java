package dev.anvilcraft.addon.terminalplugins.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

// 单个插件的设置子页视图：对齐精妙背包的「每个升级一个设置标签页」。
// 新增插件只需要实现一个视图类，面板本身不再为每种插件写 if-else。
public interface PluginSettingsView {
    // 子页需要的高度（不含标题栏与底部提示）
    int height(ItemStack plugin);

    // 画自己的设置控件（坐标系为屏幕绝对坐标）
    void render(
        Ctx ctx,
        GuiGraphics graphics,
        Minecraft minecraft,
        ItemStack plugin,
        int pluginIndex,
        int x,
        int y,
        int width,
        int mouseX,
        int mouseY
    );

    // 返回 true 表示已消费该次点击
    default boolean mouseClicked(Ctx ctx, int mouseX, int mouseY, int button) {
        return false;
    }

    // 面板提供给视图的能力
    interface Ctx {
        // 画一个显示当前值的按钮，并登记为可点击区域
        void settingButton(
            GuiGraphics graphics,
            Minecraft minecraft,
            int x,
            int y,
            int width,
            String label,
            int pluginIndex,
            int entryIndex,
            int action,
            int mouseX,
            int mouseY,
            String tipKey
        );

        // 画一个小按钮（- / + / 开关一类）
        void smallButton(
            GuiGraphics graphics,
            Minecraft minecraft,
            int x,
            int y,
            int width,
            String label,
            int pluginIndex,
            int entryIndex,
            int action,
            int mouseX,
            int mouseY
        );

        // 画一个幽灵槽（过滤槽 / 药水槽：左键放入光标物品、右键清空）
        void ghostSlot(
            GuiGraphics graphics,
            int x,
            int y,
            ItemStack shown,
            int pluginIndex,
            int entryIndex,
            int action,
            String tipKey
        );

        // 记录一次点击时要附带的数值（步进控件用）
        void pendingValue(float value);
    }
}
