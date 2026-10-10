/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for license text.
 */
package dev.anvilcraft.addon.terminalplugins.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 本模组所有界面的统一外观：深色金属底 + 黄铜描边 + 斜角槽位。
 *
 * <p>全部用矩形绘制，不依赖任何贴图，因此在任意 GUI 缩放比例下都清晰，
 * 也不会出现贴图拉伸导致的模糊。颜色以铁砧工艺的「铁灰 + 黄铜」为基调。</p>
 */
public final class PanelStyle {
    /** 外描边（最暗），用于框出面板轮廓。 */
    public static final int EDGE = 0xFF0B0C10;
    /** 面板底色（上浅下深，制造轻微渐变）。 */
    public static final int PANEL_TOP = 0xF01E2129;
    public static final int PANEL_BOTTOM = 0xF0161820;
    /** 内描边，比底色亮一档。 */
    public static final int BORDER = 0xFF3A4050;
    /** 黄铜强调色（标题、分隔线、主按钮）。 */
    public static final int ACCENT = 0xFFC8963C;
    public static final int ACCENT_LIGHT = 0xFFE9C778;
    /** 标题栏底色。 */
    public static final int HEADER = 0xFF262A36;

    public static final int TEXT = 0xFFE6E8EE;
    public static final int TEXT_DIM = 0xFF9AA0AE;
    public static final int TEXT_DISABLED = 0xFF6C7280;
    public static final int TEXT_ACCENT = 0xFFF0D79A;

    /** 槽位：内凹的斜角方格。 */
    public static final int SLOT_BG = 0xFF23262E;
    public static final int SLOT_SHADE = 0xFF0E1014;
    public static final int SLOT_LIGHT = 0xFF4A5162;
    public static final int SLOT_HOVER = 0xFF3C4658;
    /** 只读槽（输出）用绿色调，和可交互的输入槽区分。 */
    public static final int SLOT_READONLY = 0xFF23301F;
    public static final int SLOT_READONLY_EDGE = 0xFF5E8A54;

    /** 按钮三态。 */
    public static final int BUTTON_TOP = 0xFF31363F;
    public static final int BUTTON_BOTTOM = 0xFF252931;
    public static final int BUTTON_HOVER_TOP = 0xFF454C5A;
    public static final int BUTTON_HOVER_BOTTOM = 0xFF343A46;
    public static final int BUTTON_PRIMARY_TOP = 0xFF6E5620;
    public static final int BUTTON_PRIMARY_BOTTOM = 0xFF4E3D16;
    public static final int BUTTON_PRIMARY_HOVER_TOP = 0xFF8A6C2A;
    public static final int BUTTON_PRIMARY_HOVER_BOTTOM = 0xFF65501D;

    /** 列表行。 */
    public static final int ROW_HOVER = 0xFF272B35;
    public static final int ROW_SELECTED = 0xFF333A49;
    /** 开关状态点。 */
    public static final int DOT_ON = 0xFF7BC47F;
    public static final int DOT_OFF = 0xFF7A6A5A;

    private PanelStyle() {
    }

    /** 竖直渐变填充：逐行插值，避免出现明显的色带。 */
    public static void gradient(GuiGraphics graphics, int x, int y, int width, int height, int top, int bottom) {
        if (height <= 0 || width <= 0) {
            return;
        }
        for (int row = 0; row < height; row++) {
            float t = height == 1 ? 0.0F : (float) row / (float) (height - 1);
            graphics.fill(x, y + row, x + width, y + row + 1, PanelStyle.lerp(top, bottom, t));
        }
    }

    /** 面板：圆角外框 + 渐变底 + 内描边。参数是最外侧可见范围。 */
    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        // 外框（四角各留 1 像素做出圆角观感）
        graphics.fill(x + 1, y, x + width - 1, y + 1, PanelStyle.EDGE);
        graphics.fill(x + 1, y + height - 1, x + width - 1, y + height, PanelStyle.EDGE);
        graphics.fill(x, y + 1, x + 1, y + height - 1, PanelStyle.EDGE);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, PanelStyle.EDGE);
        // 底 + 内描边
        PanelStyle.gradient(graphics, x + 1, y + 1, width - 2, height - 2, PanelStyle.PANEL_TOP, PanelStyle.PANEL_BOTTOM);
        graphics.fill(x + 2, y + 1, x + width - 2, y + 2, PanelStyle.BORDER);
        graphics.fill(x + 2, y + height - 2, x + width - 2, y + height - 1, PanelStyle.BORDER);
        graphics.fill(x + 1, y + 2, x + 2, y + height - 2, PanelStyle.BORDER);
        graphics.fill(x + width - 2, y + 2, x + width - 1, y + height - 2, PanelStyle.BORDER);
    }

    /** 标题栏：浅色底 + 底部黄铜细线。 */
    public static void header(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x + 2, y + 1, x + width - 2, y + height, PanelStyle.HEADER);
        graphics.fill(x + 3, y + height, x + width - 3, y + height + 1, PanelStyle.ACCENT);
    }

    /** 内容区分隔线。 */
    public static void separator(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, 0x60C8963C);
    }

    /** 可交互槽位（斜角内凹）。 */
    public static void slot(GuiGraphics graphics, int x, int y, int size, boolean hovered) {
        graphics.fill(x, y, x + size, y + size, PanelStyle.SLOT_SHADE);
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, hovered ? PanelStyle.SLOT_HOVER : PanelStyle.SLOT_BG);
        graphics.fill(x, y, x + size - 1, y + 1, PanelStyle.SLOT_SHADE);
        graphics.fill(x, y, x + 1, y + size - 1, PanelStyle.SLOT_SHADE);
        graphics.fill(x + size - 1, y + 1, x + size, y + size, PanelStyle.SLOT_LIGHT);
        graphics.fill(x + 1, y + size - 1, x + size, y + size, PanelStyle.SLOT_LIGHT);
    }

    /** 只读槽位（输出）：绿色描边，表示只能看。 */
    public static void readOnlySlot(GuiGraphics graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, PanelStyle.SLOT_READONLY_EDGE);
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, PanelStyle.SLOT_READONLY);
    }

    /** 列表行底：选中时左侧带一条黄铜竖条。 */
    public static void row(GuiGraphics graphics, int x, int y, int width, int height, boolean selected, boolean hovered) {
        if (selected) {
            graphics.fill(x, y, x + width, y + height, PanelStyle.ROW_SELECTED);
            graphics.fill(x, y, x + 2, y + height, PanelStyle.ACCENT);
        } else if (hovered) {
            graphics.fill(x, y, x + width, y + height, PanelStyle.ROW_HOVER);
        }
    }

    /** 普通按钮 / 主按钮。{@code primary} 为真时使用黄铜底色（用于「开始加工」这类主要动作）。 */
    public static void button(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                              String label, boolean hovered, boolean primary, boolean centerText) {
        int top;
        int bottom;
        if (primary) {
            top = hovered ? PanelStyle.BUTTON_PRIMARY_HOVER_TOP : PanelStyle.BUTTON_PRIMARY_TOP;
            bottom = hovered ? PanelStyle.BUTTON_PRIMARY_HOVER_BOTTOM : PanelStyle.BUTTON_PRIMARY_BOTTOM;
        } else {
            top = hovered ? PanelStyle.BUTTON_HOVER_TOP : PanelStyle.BUTTON_TOP;
            bottom = hovered ? PanelStyle.BUTTON_HOVER_BOTTOM : PanelStyle.BUTTON_BOTTOM;
        }
        graphics.fill(x, y, x + width, y + height, PanelStyle.EDGE);
        PanelStyle.gradient(graphics, x + 1, y + 1, Math.max(0, width - 2), Math.max(0, height - 2), top, bottom);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, hovered ? PanelStyle.BORDER : 0x40FFFFFF);
        graphics.fill(x + 1, y, x + width - 1, y + 1, PanelStyle.EDGE);
        graphics.fill(x + 1, y + height - 1, x + width - 1, y + height, PanelStyle.EDGE);
        int textColor = primary ? PanelStyle.TEXT_ACCENT : PanelStyle.TEXT;
        String shown = label;
        int maxWidth = width - 6;
        if (font.width(shown) > maxWidth) {
            shown = font.plainSubstrByWidth(shown, maxWidth);
        }
        int textX = centerText ? x + (width - font.width(shown)) / 2 : x + 3;
        graphics.drawString(font, shown, textX, y + (height - 8) / 2, textColor, false);
    }

    /** 状态点：物品名前的小圆点，表示插件是否启用。 */
    public static void dot(GuiGraphics graphics, int x, int y, boolean on) {
        graphics.fill(x + 1, y, x + 2, y + 1, on ? PanelStyle.DOT_ON : PanelStyle.DOT_OFF);
        graphics.fill(x, y + 1, x + 3, y + 2, on ? PanelStyle.DOT_ON : PanelStyle.DOT_OFF);
        graphics.fill(x + 1, y + 2, x + 2, y + 3, on ? PanelStyle.DOT_ON : PanelStyle.DOT_OFF);
    }

    /**
     * 几何箭头：上移 / 下移按钮用。
     *
     * <p>不画「↑」「↓」字形：这两个符号来自字体回退，不同字体包与缩放下的
     * 字形宽度、基线都不一样，会出现「文字对不上按钮」的错位。三角形由矩形堆出来，
     * 在任何环境下位置都一样。</p>
     */
    public static void arrow(GuiGraphics graphics, int x, int y, boolean up, boolean hovered) {
        int color = hovered ? PanelStyle.ACCENT_LIGHT : PanelStyle.TEXT;
        for (int row = 0; row < 5; row++) {
            // 上箭头：第一行只有中间一像素（尖端朝上），最后一行最宽；下箭头反过来
            int half = up ? row + 1 : 5 - row;
            graphics.fill(x + 4 - half, y + row, x + 4 + half, y + row + 1, color);
        }
    }

    /** 返回箭头：指向左边的三角形。 */
    public static void backArrow(GuiGraphics graphics, int x, int y, boolean hovered) {
        int color = hovered ? PanelStyle.ACCENT_LIGHT : PanelStyle.TEXT;
        for (int col = 0; col < 5; col++) {
            // 尖端在左：第一列只有中间一像素，向右逐渐变高
            int half = col + 1;
            graphics.fill(x + col, y + 5 - half, x + col + 1, y + 5 + half, color);
        }
    }

    /** 拖动把手：三道横线。 */
    public static void grip(GuiGraphics graphics, int x, int y, int width, boolean hovered) {
        int color = hovered ? PanelStyle.ACCENT_LIGHT : PanelStyle.TEXT_DIM;
        for (int row = 0; row < 3; row++) {
            graphics.fill(x, y + row * 3, x + width, y + row * 3 + 1, color);
        }
    }

    /** 关闭按钮：悬停时红底 + 叉。 */
    public static void closeButton(GuiGraphics graphics, int x, int y, boolean hovered) {
        if (hovered) {
            graphics.fill(x, y, x + 10, y + 10, 0xFF7E3B36);
        }
        int color = hovered ? 0xFFFFFFFF : PanelStyle.TEXT_DIM;
        for (int i = 0; i < 6; i++) {
            graphics.fill(x + 2 + i, y + 2 + i, x + 3 + i, y + 3 + i, color);
            graphics.fill(x + 7 - i, y + 2 + i, x + 8 - i, y + 3 + i, color);
        }
    }

    private static int lerp(int from, int to, float t) {
        int a = (from >>> 24) & 0xFF;
        int r = (from >> 16) & 0xFF;
        int g = (from >> 8) & 0xFF;
        int b = from & 0xFF;
        int a2 = (to >>> 24) & 0xFF;
        int r2 = (to >> 16) & 0xFF;
        int g2 = (to >> 8) & 0xFF;
        int b2 = to & 0xFF;
        int ar = Math.round(a + (a2 - a) * t);
        int rr = Math.round(r + (r2 - r) * t);
        int gr = Math.round(g + (g2 - g) * t);
        int br = Math.round(b + (b2 - b) * t);
        return (ar << 24) | (rr << 16) | (gr << 8) | br;
    }
}