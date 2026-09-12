package dev.anvilcraft.addon.terminalplugins.client;

import dev.anvilcraft.addon.terminalplugins.component.AlchemySettings;
import dev.anvilcraft.addon.terminalplugins.component.AutoCookingSettings;
import dev.anvilcraft.addon.terminalplugins.component.FeedingSettings;
import dev.anvilcraft.addon.terminalplugins.component.MagnetSettings;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.item.TerminalPluginItem;
import dev.anvilcraft.addon.terminalplugins.network.PluginActionPacket;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginKind;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPluginManager;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

// 终端插件调节面板：叠加在存储界面之上，不关闭玩家已打开的界面。
// 位置默认贴屏幕左侧（JEI 的素材列表默认在右侧，避免重叠）；按住「插件」按钮可拖动整个面板，
// 位置记在 config/anvilcraft_terminal_plugins_panel.txt，下次进游戏仍然生效。
public class TerminalPluginPanel {
    private static final int BUTTON_WIDTH = 52;
    private static final int BUTTON_HEIGHT = 14;
    private static final int PANEL_WIDTH = 158;
    private static final int PANEL_HEIGHT = 246;
    private static final int DRAG_THRESHOLD = 3;
    private static final String POSITION_FILE = "anvilcraft_terminal_plugins_panel.txt";

    private static final int COLOR_PANEL = 0xF0181820;
    private static final int COLOR_BORDER = 0xFF6E6E78;
    private static final int COLOR_SLOT = 0xFF37373B;
    private static final int COLOR_TEXT = 0xFFE0E0E6;
    private static final int COLOR_DIM = 0xFF9A9AA4;
    private static final int COLOR_BUTTON = 0xFF2A2A33;
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
    private final List<Region> tipRegions = new ArrayList<>();
    private final List<String> tipTexts = new ArrayList<>();
    private boolean open = false;
    private int selected = 0;
    private Target target;

    private boolean positionLoaded = false;
    private int anchorX = 4;
    private int anchorY = 22;
    private boolean dragging = false;
    private boolean pendingToggle = false;
    private int dragStartMouseX;
    private int dragStartMouseY;
    private int dragStartAnchorX;
    private int dragStartAnchorY;

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

    // ---------- 位置与拖动 ----------

    private void ensurePosition(Minecraft minecraft) {
        if (this.positionLoaded) {
            return;
        }
        this.positionLoaded = true;
        Path path = TerminalPluginPanel.positionPath(minecraft);
        try {
            if (Files.exists(path)) {
                String[] parts = Files.readString(path).trim().split("\\s+");
                if (parts.length >= 2) {
                    this.anchorX = Integer.parseInt(parts[0]);
                    this.anchorY = Integer.parseInt(parts[1]);
                }
            }
        } catch (IOException | NumberFormatException ignored) {
            // 读不到就用默认位置
        }
        this.clampToScreen(minecraft);
    }

    private static Path positionPath(Minecraft minecraft) {
        return minecraft.gameDirectory.toPath().resolve("config").resolve(TerminalPluginPanel.POSITION_FILE);
    }

    private void savePosition(Minecraft minecraft) {
        try {
            Path path = TerminalPluginPanel.positionPath(minecraft);
            Files.createDirectories(path.getParent());
            Files.writeString(path, this.anchorX + " " + this.anchorY);
        } catch (IOException ignored) {
            // 存不下来也不影响本次游戏
        }
    }

    private void clampToScreen(Minecraft minecraft) {
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int totalHeight = TerminalPluginPanel.BUTTON_HEIGHT + 2 + TerminalPluginPanel.PANEL_HEIGHT;
        this.anchorX = Math.max(0, Math.min(this.anchorX, Math.max(0, screenWidth - TerminalPluginPanel.PANEL_WIDTH)));
        this.anchorY = Math.max(0, Math.min(this.anchorY, Math.max(0, screenHeight - totalHeight)));
    }

    private int left(Minecraft minecraft) {
        this.ensurePosition(minecraft);
        return this.anchorX;
    }

    private int top(Minecraft minecraft) {
        this.ensurePosition(minecraft);
        return this.anchorY;
    }

    private int panelTop(Minecraft minecraft) {
        return this.top(minecraft) + TerminalPluginPanel.BUTTON_HEIGHT + 2;
    }

    public boolean isOverToggleButton(Minecraft minecraft, double mouseX, double mouseY) {
        int x = this.left(minecraft);
        int y = this.top(minecraft);
        return mouseX >= x && mouseX < x + TerminalPluginPanel.BUTTON_WIDTH
               && mouseY >= y && mouseY < y + TerminalPluginPanel.BUTTON_HEIGHT;
    }

    public boolean isOverPanel(Minecraft minecraft, double mouseX, double mouseY) {
        if (!this.open) {
            return false;
        }
        int x = this.left(minecraft);
        int y = this.panelTop(minecraft);
        return mouseX >= x && mouseX < x + TerminalPluginPanel.PANEL_WIDTH
               && mouseY >= y && mouseY < y + TerminalPluginPanel.PANEL_HEIGHT;
    }

    // ---------- 鼠标 ----------

    public boolean mouseClicked(Minecraft minecraft, double mouseX, double mouseY, int button) {
        if (this.isOverToggleButton(minecraft, mouseX, mouseY)) {
            this.pendingToggle = true;
            this.dragging = false;
            this.dragStartMouseX = (int) mouseX;
            this.dragStartMouseY = (int) mouseY;
            this.dragStartAnchorX = this.left(minecraft);
            this.dragStartAnchorY = this.top(minecraft);
            return true;
        }
        for (Region region : this.regions) {
            if (region.contains(mouseX, mouseY)) {
                this.dispatch(region, button);
                return true;
            }
        }
        return this.isOverPanel(minecraft, mouseX, mouseY);
    }

    public boolean mouseDragged(Minecraft minecraft, double mouseX, double mouseY) {
        if (!this.pendingToggle && !this.dragging) {
            return false;
        }
        int dx = (int) mouseX - this.dragStartMouseX;
        int dy = (int) mouseY - this.dragStartMouseY;
        if (!this.dragging) {
            if (Math.abs(dx) < TerminalPluginPanel.DRAG_THRESHOLD
                && Math.abs(dy) < TerminalPluginPanel.DRAG_THRESHOLD) {
                return true;
            }
            this.dragging = true;
            this.pendingToggle = false;
        }
        this.anchorX = this.dragStartAnchorX + dx;
        this.anchorY = this.dragStartAnchorY + dy;
        this.clampToScreen(minecraft);
        return true;
    }

    public boolean mouseReleased(Minecraft minecraft) {
        boolean handled = this.pendingToggle || this.dragging;
        if (this.dragging) {
            this.savePosition(minecraft);
        } else if (this.pendingToggle) {
            this.toggle(this.heldTarget());
        }
        this.pendingToggle = false;
        this.dragging = false;
        return handled;
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

    private ItemStack terminal() {
        return this.target == null ? ItemStack.EMPTY : this.target.stackSupplier().get();
    }

    public static int findHeldTerminalSlot() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return Integer.MIN_VALUE;
        }
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

    // ---------- 绘制 ----------

    public void render(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
        this.regions.clear();
        this.tipRegions.clear();
        this.tipTexts.clear();
        this.renderToggleButton(graphics, minecraft, mouseX, mouseY);
        if (!this.open) {
            return;
        }
        int x = this.left(minecraft);
        int y = this.panelTop(minecraft);
        graphics.fill(x - 1, y - 1, x + TerminalPluginPanel.PANEL_WIDTH + 1, y + TerminalPluginPanel.PANEL_HEIGHT + 1, TerminalPluginPanel.COLOR_BORDER);
        graphics.fill(x, y, x + TerminalPluginPanel.PANEL_WIDTH, y + TerminalPluginPanel.PANEL_HEIGHT, TerminalPluginPanel.COLOR_PANEL);
        graphics.drawString(minecraft.font, Component.translatable("screen.anvilcraft_terminal_plugins.panel.title"), x + 4, y + 4, TerminalPluginPanel.COLOR_TEXT, false);
        graphics.drawString(minecraft.font, "x", x + TerminalPluginPanel.PANEL_WIDTH - 11, y + 5, TerminalPluginPanel.COLOR_DIM, false);
        this.regions.add(new Region(x + TerminalPluginPanel.PANEL_WIDTH - 14, y + 2, 12, 12, Action.CLOSE, -1, -1));

        ItemStack terminal = this.terminal();
        List<ItemStack> plugins = TerminalPluginManager.installed(terminal);
        if (plugins.isEmpty()) {
            graphics.drawString(minecraft.font, Component.translatable("screen.anvilcraft_terminal_plugins.panel.empty"), x + 4, y + 20, TerminalPluginPanel.COLOR_DIM, false);
            this.renderFooter(graphics, minecraft, x, y);
            return;
        }
        this.selected = Math.clamp(this.selected, 0, plugins.size() - 1);

        int rowY = y + 18;
        int visibleRows = Math.min(plugins.size(), 5);
        for (int index = 0; index < visibleRows; index++) {
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
        if (plugins.size() > visibleRows) {
            graphics.drawString(minecraft.font, "+" + (plugins.size() - visibleRows), x + TerminalPluginPanel.PANEL_WIDTH - 16, rowY - 12, TerminalPluginPanel.COLOR_DIM, false);
        }

        ItemStack selectedPlugin = plugins.get(this.selected);
        int buttonY = rowY + 2;
        int buttonX = x + 3;
        // 第一行：开关 / 排序 / 拆下
        buttonX = this.button(graphics, minecraft, buttonX, buttonY, 22, TerminalPluginManager.isEnabled(selectedPlugin)
            ? "screen.anvilcraft_terminal_plugins.panel.disable"
            : "screen.anvilcraft_terminal_plugins.panel.enable", Action.TOGGLE, this.selected, -1, mouseX, mouseY);
        buttonX = this.button(graphics, minecraft, buttonX, buttonY, 14, "screen.anvilcraft_terminal_plugins.panel.up", Action.MOVE_UP, this.selected, -1, mouseX, mouseY);
        buttonX = this.button(graphics, minecraft, buttonX, buttonY, 14, "screen.anvilcraft_terminal_plugins.panel.down", Action.MOVE_DOWN, this.selected, -1, mouseX, mouseY);
        this.button(graphics, minecraft, buttonX, buttonY, 20, "screen.anvilcraft_terminal_plugins.panel.remove", Action.REMOVE, this.selected, -1, mouseX, mouseY);
        rowY = buttonY + 15;

        // 与精妙背包的升级标签一致：每个设置是一个「显示当前值」的按钮，点一下循环切换，悬停显示说明
        String primaryLabel = this.primaryLabel(selectedPlugin);
        String secondaryLabel = this.secondaryLabel(selectedPlugin);
        String cycleTip = "screen.anvilcraft_terminal_plugins.panel.cycle_tip";
        if (primaryLabel != null) {
            this.settingButton(graphics, minecraft, x + 3, rowY, TerminalPluginPanel.PANEL_WIDTH - 6, primaryLabel,
                Action.CYCLE_PRIMARY, this.selected, mouseX, mouseY, cycleTip);
            rowY += 14;
        }
        if (secondaryLabel != null) {
            this.settingButton(graphics, minecraft, x + 3, rowY, TerminalPluginPanel.PANEL_WIDTH - 6, secondaryLabel,
                Action.CYCLE_SECONDARY, this.selected, mouseX, mouseY, cycleTip);
            rowY += 14;
        }

        if (selectedPlugin.getItem() instanceof TerminalPluginItem item && item.kind() == PluginKind.ALCHEMY) {
            AlchemySettings settings = selectedPlugin.getOrDefault(AddonDataComponents.ALCHEMY_SETTINGS, AlchemySettings.DEFAULT);
            graphics.drawString(minecraft.font, Component.translatable("screen.anvilcraft_terminal_plugins.panel.alchemy"), x + 4, rowY + 1, TerminalPluginPanel.COLOR_DIM, false);
            rowY += 11;
            List<AlchemySettings.AlchemyEntry> entries = settings.normalizedEntries();
            for (int index = 0; index < entries.size(); index++) {
                AlchemySettings.AlchemyEntry entry = entries.get(index);
                graphics.fill(x + 4, rowY, x + 20, rowY + 16, TerminalPluginPanel.COLOR_SLOT);
                if (!entry.filter().isEmpty()) {
                    graphics.renderItem(entry.filter(), x + 4, rowY);
                }
                this.regionWithTip(new Region(x + 4, rowY, 16, 16, Action.SET_ALCHEMY_FILTER, this.selected, index),
                    "screen.anvilcraft_terminal_plugins.panel.filter_tip");
                String trigger = TerminalPluginPanel.tr(
                    "tooltip.anvilcraft_terminal_plugins.alchemy.trigger." + entry.trigger().getSerializedName()
                );
                this.drawButton(graphics, minecraft, x + 22, rowY + 2, 62, trigger, Action.CYCLE_ALCHEMY_TRIGGER,
                    this.selected, index, mouseX, mouseY, "screen.anvilcraft_terminal_plugins.panel.trigger_tip");
                if (entry.trigger().usesValue()) {
                    this.drawButton(graphics, minecraft, x + 86, rowY + 2, 12, "-", Action.VALUE_DOWN, this.selected, index, mouseX, mouseY, null);
                    graphics.drawString(minecraft.font, String.format("%.2f", entry.value()), x + 100, rowY + 4, TerminalPluginPanel.COLOR_TEXT, false);
                    this.drawButton(graphics, minecraft, x + 130, rowY + 2, 12, "+", Action.VALUE_UP, this.selected, index, mouseX, mouseY, null);
                }
                rowY += 18;
            }
            // 作用半径（作用范围本身由上面的「作用范围」设置按钮切换）
            this.drawButton(graphics, minecraft, x + 4, rowY + 1, 62, TerminalPluginPanel.tr(
                "screen.anvilcraft_terminal_plugins.setting.range", settings.radius()
            ), Action.RADIUS_UP, this.selected, -1, mouseX, mouseY, "screen.anvilcraft_terminal_plugins.panel.radius_tip");
            this.drawButton(graphics, minecraft, x + 68, rowY + 1, 12, "-", Action.RADIUS_DOWN, this.selected, -1, mouseX, mouseY, null);
        }
        this.renderTips(graphics, minecraft, mouseX, mouseY);
        this.renderFooter(graphics, minecraft, x, y);
    }

    private void renderToggleButton(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
        int x = this.left(minecraft);
        int y = this.top(minecraft);
        int slot = TerminalPluginPanel.findHeldTerminalSlot();
        ItemStack terminal = this.target != null ? this.terminal()
            : (slot == Integer.MIN_VALUE ? ItemStack.EMPTY : TerminalPluginPanel.stackAt(slot));
        boolean hasTerminal = !terminal.isEmpty();
        int count = TerminalPluginManager.installed(terminal).size();
        boolean hovered = this.isOverToggleButton(minecraft, mouseX, mouseY) || this.dragging;
        graphics.fill(x, y, x + TerminalPluginPanel.BUTTON_WIDTH, y + TerminalPluginPanel.BUTTON_HEIGHT,
            hovered ? TerminalPluginPanel.COLOR_BUTTON_HOVER : TerminalPluginPanel.COLOR_BUTTON);
        graphics.drawString(minecraft.font, "==", x + 3, y + 3, TerminalPluginPanel.COLOR_DIM, false);
        String label = Component.translatable("screen.anvilcraft_terminal_plugins.panel.button").getString() + " " + count;
        graphics.drawString(minecraft.font, label, x + 16, y + 3,
            hasTerminal ? TerminalPluginPanel.COLOR_TEXT : TerminalPluginPanel.COLOR_DIM, false);
        graphics.drawString(minecraft.font, this.open ? "v" : ">", x + TerminalPluginPanel.BUTTON_WIDTH - 8, y + 3, TerminalPluginPanel.COLOR_DIM, false);
    }

    private void renderFooter(GuiGraphics graphics, Minecraft minecraft, int x, int y) {
        graphics.drawString(
            minecraft.font,
            Component.translatable("screen.anvilcraft_terminal_plugins.panel.drag_hint"),
            x + 4,
            y + TerminalPluginPanel.PANEL_HEIGHT - 11,
            TerminalPluginPanel.COLOR_DIM,
            false
        );
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

    // ---------- 精妙背包风格的设置控件：按钮上直接显示当前值 ----------

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    private void regionWithTip(Region region, String tipKey) {
        this.regions.add(region);
        if (tipKey != null) {
            this.addTip(region, TerminalPluginPanel.tr(tipKey));
        }
    }

    private void addTip(Region region, String text) {
        this.tipRegions.add(region);
        this.tipTexts.add(text);
    }

    private void drawButton(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, String text,
                            Action action, int pluginIndex, int entryIndex, int mouseX, int mouseY, String tipKey) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 12;
        graphics.fill(x, y, x + width, y + 12, hovered ? TerminalPluginPanel.COLOR_BUTTON_HOVER : TerminalPluginPanel.COLOR_BUTTON);
        String label = minecraft.font.width(text) > width - 2 ? minecraft.font.plainSubstrByWidth(text, width - 2) : text;
        graphics.drawString(minecraft.font, label, x + (width - minecraft.font.width(label)) / 2, y + 2,
            TerminalPluginPanel.COLOR_TEXT, false);
        this.regionWithTip(new Region(x, y, width, 12, action, pluginIndex, entryIndex), tipKey);
    }

    private void settingButton(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, String text,
                               Action action, int pluginIndex, int mouseX, int mouseY, String tipKey) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 12;
        graphics.fill(x, y, x + width, y + 12, hovered ? TerminalPluginPanel.COLOR_BUTTON_HOVER : TerminalPluginPanel.COLOR_BUTTON);
        String label = minecraft.font.width(text) > width - 4 ? minecraft.font.plainSubstrByWidth(text, width - 4) : text;
        graphics.drawString(minecraft.font, label, x + 3, y + 2, TerminalPluginPanel.COLOR_TEXT, false);
        this.regionWithTip(new Region(x, y, width, 12, action, pluginIndex, -1), tipKey);
    }

    private void renderTips(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
        if (this.isOverToggleButton(minecraft, mouseX, mouseY)) {
            graphics.renderTooltip(minecraft.font,
                Component.translatable("screen.anvilcraft_terminal_plugins.panel.drag_hint"), mouseX, mouseY);
            return;
        }
        for (int index = 0; index < this.tipRegions.size(); index++) {
            if (this.tipRegions.get(index).contains(mouseX, mouseY)) {
                graphics.renderTooltip(minecraft.font, Component.literal(this.tipTexts.get(index)), mouseX, mouseY);
                return;
            }
        }
    }

    // 主档位按钮的文字：直接显示当前值（对齐精妙背包的升级设置按钮）
    private String primaryLabel(ItemStack plugin) {
        if (!(plugin.getItem() instanceof TerminalPluginItem item)) {
            return null;
        }
        return switch (item.kind()) {
            case FILTER -> {
                FilterContent content = plugin.getOrDefault(ModComponents.FILTER_CONTENT, new FilterContent());
                yield TerminalPluginPanel.tr(content.blackList()
                    ? "tooltip.anvilcraft_terminal_plugins.filter.mode.blacklist"
                    : "tooltip.anvilcraft_terminal_plugins.filter.mode.whitelist");
            }
            case MAGNET -> TerminalPluginPanel.tr(
                "screen.anvilcraft_terminal_plugins.setting.range",
                plugin.getOrDefault(AddonDataComponents.MAGNET_SETTINGS, MagnetSettings.DEFAULT).range()
            );
            case AUTO_COOKING -> TerminalPluginPanel.tr(
                "screen.anvilcraft_terminal_plugins.setting.recipe",
                TerminalPluginPanel.tr("tooltip.anvilcraft_terminal_plugins.cooking.mode."
                    + plugin.getOrDefault(AddonDataComponents.COOKING_SETTINGS, AutoCookingSettings.DEFAULT)
                        .mode().getSerializedName())
            );
            case FEEDING -> TerminalPluginPanel.tr(
                "screen.anvilcraft_terminal_plugins.setting.threshold",
                plugin.getOrDefault(AddonDataComponents.FEEDING_SETTINGS, FeedingSettings.DEFAULT).hungerThreshold()
            );
            case ALCHEMY -> TerminalPluginPanel.tr(
                "screen.anvilcraft_terminal_plugins.setting.nearby",
                TerminalPluginPanel.tr("tooltip.anvilcraft_terminal_plugins.alchemy.nearby."
                    + plugin.getOrDefault(AddonDataComponents.ALCHEMY_SETTINGS, AlchemySettings.DEFAULT)
                        .nearby().getSerializedName())
            );
        };
    }

    // 副档位按钮的文字
    private String secondaryLabel(ItemStack plugin) {
        if (!(plugin.getItem() instanceof TerminalPluginItem item)) {
            return null;
        }
        String on = TerminalPluginPanel.tr("screen.anvilcraft_terminal_plugins.setting.on");
        String off = TerminalPluginPanel.tr("screen.anvilcraft_terminal_plugins.setting.off");
        return switch (item.kind()) {
            case FILTER -> TerminalPluginPanel.tr(
                "screen.anvilcraft_terminal_plugins.setting.components",
                plugin.getOrDefault(ModComponents.FILTER_CONTENT, new FilterContent()).includeComponents() ? on : off
            );
            case MAGNET -> {
                MagnetSettings settings = plugin.getOrDefault(AddonDataComponents.MAGNET_SETTINGS, MagnetSettings.DEFAULT);
                String mode;
                if (settings.magnetEnabled() && settings.pickupEnabled()) {
                    mode = TerminalPluginPanel.tr("tooltip.anvilcraft_terminal_plugins.magnet.mode.both");
                } else if (settings.magnetEnabled()) {
                    mode = TerminalPluginPanel.tr("tooltip.anvilcraft_terminal_plugins.magnet.mode.magnet");
                } else {
                    mode = TerminalPluginPanel.tr("tooltip.anvilcraft_terminal_plugins.magnet.mode.pickup");
                }
                yield TerminalPluginPanel.tr("screen.anvilcraft_terminal_plugins.setting.mode", mode);
            }
            case AUTO_COOKING -> TerminalPluginPanel.tr(
                "screen.anvilcraft_terminal_plugins.setting.fuel",
                plugin.getOrDefault(AddonDataComponents.COOKING_SETTINGS, AutoCookingSettings.DEFAULT).consumeFuel()
                    ? on : off
            );
            case FEEDING -> TerminalPluginPanel.tr(
                "screen.anvilcraft_terminal_plugins.setting.harmful",
                plugin.getOrDefault(AddonDataComponents.FEEDING_SETTINGS, FeedingSettings.DEFAULT).allowHarmful()
                    ? on : off
            );
            case ALCHEMY -> TerminalPluginPanel.tr(
                "screen.anvilcraft_terminal_plugins.setting.amplifier",
                plugin.getOrDefault(AddonDataComponents.ALCHEMY_SETTINGS, AlchemySettings.DEFAULT).matchAmplifier()
                    ? on : off
            );
        };
    }
}
