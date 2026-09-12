package dev.anvilcraft.addon.terminalplugins.client;

import dev.anvilcraft.addon.terminalplugins.client.gui.PluginSettingsView;
import dev.anvilcraft.addon.terminalplugins.client.gui.PluginViews;
import dev.anvilcraft.addon.terminalplugins.item.TerminalPluginItem;
import dev.anvilcraft.addon.terminalplugins.network.PluginActionPacket;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPluginManager;
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

// 终端插件面板：两级结构（插件列表 / 单个插件的设置子页），对齐精妙背包的升级标签。
// 叠加画在存储界面之上，不关闭玩家已打开的界面；按住「≡」按钮可拖动，位置会记住。
public class TerminalPluginPanel implements PluginSettingsView.Ctx {
    private static final int BUTTON_WIDTH = 52;
    private static final int BUTTON_HEIGHT = 14;
    private static final int PANEL_WIDTH = 158;
    private static final int DRAG_THRESHOLD = 3;
    private static final int VISIBLE_ROWS = 5;
    private static final String POSITION_FILE = "anvilcraft_terminal_plugins_panel.txt";

    // 本地动作（非网络动作，用负数与 PluginActionPacket 的动作区分）
    private static final int LOCAL_CLOSE = -1;
    private static final int LOCAL_SELECT = -2;
    private static final int LOCAL_SETTINGS = -3;
    private static final int LOCAL_BACK = -4;

    private static final int COLOR_PANEL = 0xF0181820;
    private static final int COLOR_BORDER = 0xFF6E6E78;
    private static final int COLOR_SLOT = 0xFF37373B;
    private static final int COLOR_TEXT = 0xFFE0E0E6;
    private static final int COLOR_DIM = 0xFF9A9AA4;
    private static final int COLOR_BUTTON = 0xFF2A2A33;
    private static final int COLOR_BUTTON_HOVER = 0xFF565666;
    private static final int COLOR_SELECTED = 0xFF4A4A5C;

    private record Region(int x, int y, int width, int height, int action, int pluginIndex, int entryIndex) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
        }
    }

    // 面板目标：安装台里的终端，或玩家身上的某个终端（-1 主手 / -2 副手 / >=0 物品栏索引）
    public record Target(Optional<BlockPos> station, int containerSlot, Supplier<ItemStack> stackSupplier) {
    }

    private final List<Region> regions = new ArrayList<>();
    private final List<Float> regionValues = new ArrayList<>();
    private final List<Region> tipRegions = new ArrayList<>();
    private final List<String> tipTexts = new ArrayList<>();

    private boolean open = false;
    private boolean settingsOpen = false;
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
        this.settingsOpen = false;
    }

    public void open(Target newTarget) {
        this.target = newTarget;
        this.open = newTarget != null;
        this.settingsOpen = false;
        this.selected = 0;
    }

    public void toggle(Target newTarget) {
        if (this.open && newTarget != null && this.target != null
            && this.target.station().equals(newTarget.station())
            && this.target.containerSlot() == newTarget.containerSlot()) {
            this.close();
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
        this.anchorX = Math.max(0, Math.min(this.anchorX, Math.max(0, screenWidth - TerminalPluginPanel.PANEL_WIDTH)));
        this.anchorY = Math.max(0, Math.min(this.anchorY, Math.max(0, screenHeight - 40)));
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

    // 面板高度按内容自适应（对齐精妙背包按子控件外接矩形推算尺寸的思路）
    private int panelHeight(ItemStack terminal) {
        List<ItemStack> plugins = TerminalPluginManager.installed(terminal);
        if (!this.settingsOpen || plugins.isEmpty()) {
            int rows = Math.min(plugins.size(), TerminalPluginPanel.VISIBLE_ROWS);
            return 18 + Math.max(rows, 1) * 14 + 4 + 15 + 12;
        }
        ItemStack plugin = plugins.get(Math.clamp(this.selected, 0, plugins.size() - 1));
        int content = plugin.getItem() instanceof TerminalPluginItem item
            ? PluginViews.of(item.kind()).height(plugin)
            : 14;
        return 18 + content + 4 + 12;
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
               && mouseY >= y && mouseY < y + this.panelHeight(this.terminal());
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
        for (int index = 0; index < this.regions.size(); index++) {
            if (this.regions.get(index).contains(mouseX, mouseY)) {
                this.dispatch(index, button);
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

    private static boolean isAdjustAction(int action) {
        return action == PluginActionPacket.ADJUST_ALCHEMY_VALUE
               || action == PluginActionPacket.ADJUST_ALCHEMY_RADIUS
               || action == PluginActionPacket.ADJUST_MAGNET_RANGE
               || action == PluginActionPacket.ADJUST_FEEDING_THRESHOLD;
    }

    private void dispatch(int regionIndex, int button) {
        Region region = this.regions.get(regionIndex);
        switch (region.action()) {
            case LOCAL_CLOSE -> {
                this.close();
                return;
            }
            case LOCAL_SELECT -> {
                this.selected = region.pluginIndex();
                this.settingsOpen = false;
                return;
            }
            case LOCAL_SETTINGS -> {
                this.settingsOpen = true;
                return;
            }
            case LOCAL_BACK -> {
                this.settingsOpen = false;
                return;
            }
            default -> {
            }
        }
        if (this.target == null) {
            return;
        }
        int action = region.action();
        float value = regionIndex < this.regionValues.size() ? this.regionValues.get(regionIndex) : 0.0F;
        if (button == 1 && TerminalPluginPanel.isAdjustAction(action)) {
            value = -value;
        }
        ItemStack payload = ItemStack.EMPTY;
        if (action == PluginActionPacket.SET_FILTER_SLOT || action == PluginActionPacket.SET_ALCHEMY_FILTER) {
            Minecraft minecraft = Minecraft.getInstance();
            ItemStack carried = minecraft.player == null ? ItemStack.EMPTY : minecraft.player.containerMenu.getCarried();
            payload = button == 1 || carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1);
        }
        PacketDistributor.sendToServer(new PluginActionPacket(
            this.target.station(), this.target.containerSlot(), action,
            region.pluginIndex(), region.entryIndex(), payload, value
        ));
    }

    // ---------- PluginSettingsView.Ctx 实现 ----------

    @Override
    public void settingButton(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, String label,
                              int pluginIndex, int entryIndex, int action, int mouseX, int mouseY, String tipKey) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 12;
        graphics.fill(x, y, x + width, y + 12, hovered ? TerminalPluginPanel.COLOR_BUTTON_HOVER : TerminalPluginPanel.COLOR_BUTTON);
        String text = minecraft.font.width(label) > width - 6 ? minecraft.font.plainSubstrByWidth(label, width - 6) : label;
        graphics.drawString(minecraft.font, text, x + 3, y + 2, TerminalPluginPanel.COLOR_TEXT, false);
        this.addRegion(new Region(x, y, width, 12, action, pluginIndex, entryIndex), 0.0F, tipKey);
    }

    @Override
    public void smallButton(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, String label,
                            int pluginIndex, int entryIndex, int action, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 12;
        graphics.fill(x, y, x + width, y + 12, hovered ? TerminalPluginPanel.COLOR_BUTTON_HOVER : TerminalPluginPanel.COLOR_BUTTON);
        graphics.drawString(minecraft.font, label, x + (width - minecraft.font.width(label)) / 2, y + 2,
            TerminalPluginPanel.COLOR_TEXT, false);
        this.addRegion(new Region(x, y, width, 12, action, pluginIndex, entryIndex), 0.0F, null);
    }

    @Override
    public void ghostSlot(GuiGraphics graphics, int x, int y, ItemStack shown, int pluginIndex, int entryIndex,
                          int action, String tipKey) {
        graphics.fill(x, y, x + 16, y + 16, TerminalPluginPanel.COLOR_SLOT);
        if (!shown.isEmpty()) {
            graphics.renderItem(shown, x, y);
        }
        this.addRegion(new Region(x, y, 16, 16, action, pluginIndex, entryIndex), 0.0F, tipKey);
    }

    @Override
    public void pendingValue(float value) {
        if (!this.regionValues.isEmpty()) {
            this.regionValues.set(this.regionValues.size() - 1, value);
        }
    }

    private void addRegion(Region region, float value, String tipKey) {
        this.regions.add(region);
        this.regionValues.add(value);
        if (tipKey != null) {
            this.tipRegions.add(region);
            this.tipTexts.add(TerminalPluginPanel.tr(tipKey));
        }
    }

    // ---------- 工具 ----------

    private ItemStack terminal() {
        return this.target == null ? ItemStack.EMPTY : this.target.stackSupplier().get();
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
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
        this.regionValues.clear();
        this.tipRegions.clear();
        this.tipTexts.clear();
        this.renderToggleButton(graphics, minecraft, mouseX, mouseY);
        if (!this.open) {
            return;
        }
        ItemStack terminal = this.terminal();
        List<ItemStack> plugins = TerminalPluginManager.installed(terminal);
        int x = this.left(minecraft);
        int y = this.panelTop(minecraft);
        int height = this.panelHeight(terminal);
        graphics.fill(x - 1, y - 1, x + TerminalPluginPanel.PANEL_WIDTH + 1, y + height + 1, TerminalPluginPanel.COLOR_BORDER);
        graphics.fill(x, y, x + TerminalPluginPanel.PANEL_WIDTH, y + height, TerminalPluginPanel.COLOR_PANEL);
        graphics.drawString(minecraft.font, "x", x + TerminalPluginPanel.PANEL_WIDTH - 11, y + 5, TerminalPluginPanel.COLOR_DIM, false);
        this.addRegion(new Region(x + TerminalPluginPanel.PANEL_WIDTH - 14, y + 2, 12, 12, TerminalPluginPanel.LOCAL_CLOSE, -1, -1), 0.0F, null);

        if (plugins.isEmpty()) {
            graphics.drawString(minecraft.font,
                Component.translatable("screen.anvilcraft_terminal_plugins.panel.title"), x + 4, y + 5,
                TerminalPluginPanel.COLOR_TEXT, false);
            graphics.drawString(minecraft.font,
                Component.translatable("screen.anvilcraft_terminal_plugins.panel.empty"), x + 4, y + 20,
                TerminalPluginPanel.COLOR_DIM, false);
            this.renderFooter(graphics, minecraft, x, y + height);
            return;
        }
        this.selected = Math.clamp(this.selected, 0, plugins.size() - 1);
        ItemStack selectedPlugin = plugins.get(this.selected);

        if (this.settingsOpen) {
            // 设置子页：标题栏（返回 + 插件名）+ 插件自己的控件
            this.addRegion(new Region(x + 2, y + 2, 12, 12, TerminalPluginPanel.LOCAL_BACK, -1, -1), 0.0F,
                "screen.anvilcraft_terminal_plugins.panel.back_tip");
            graphics.drawString(minecraft.font, "<", x + 5, y + 5, TerminalPluginPanel.COLOR_TEXT, false);
            String name = minecraft.font.width(selectedPlugin.getHoverName()) > TerminalPluginPanel.PANEL_WIDTH - 24
                ? minecraft.font.plainSubstrByWidth(selectedPlugin.getHoverName().getString(), TerminalPluginPanel.PANEL_WIDTH - 24)
                : selectedPlugin.getHoverName().getString();
            graphics.renderItem(selectedPlugin, x + 16, y + 1);
            graphics.drawString(minecraft.font, name, x + 34, y + 5, TerminalPluginPanel.COLOR_TEXT, false);
            if (selectedPlugin.getItem() instanceof TerminalPluginItem item) {
                PluginViews.of(item.kind()).render(
                    this, graphics, minecraft, selectedPlugin, this.selected,
                    x + 4, y + 18, TerminalPluginPanel.PANEL_WIDTH - 8, mouseX, mouseY
                );
            }
            this.renderTips(graphics, minecraft, mouseX, mouseY);
            this.renderFooter(graphics, minecraft, x, y + height);
            return;
        }

        // 插件列表页
        graphics.drawString(minecraft.font,
            Component.translatable("screen.anvilcraft_terminal_plugins.panel.title"), x + 4, y + 5,
            TerminalPluginPanel.COLOR_TEXT, false);
        int rowY = y + 18;
        int rows = Math.min(plugins.size(), TerminalPluginPanel.VISIBLE_ROWS);
        for (int index = 0; index < rows; index++) {
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
            String label = minecraft.font.width(name) > TerminalPluginPanel.PANEL_WIDTH - 34
                ? minecraft.font.plainSubstrByWidth(name, TerminalPluginPanel.PANEL_WIDTH - 34)
                : name;
            graphics.drawString(minecraft.font, label, x + 24, rowY + 3,
                enabled ? TerminalPluginPanel.COLOR_TEXT : TerminalPluginPanel.COLOR_DIM, false);
            if (!enabled) {
                graphics.drawString(minecraft.font, "off", x + TerminalPluginPanel.PANEL_WIDTH - 20, rowY + 3,
                    TerminalPluginPanel.COLOR_DIM, false);
            }
            this.addRegion(new Region(x + 2, rowY, TerminalPluginPanel.PANEL_WIDTH - 4, 14, TerminalPluginPanel.LOCAL_SELECT, index, -1), 0.0F, null);
            rowY += 14;
        }
        if (plugins.size() > rows) {
            graphics.drawString(minecraft.font, "+" + (plugins.size() - rows),
                x + TerminalPluginPanel.PANEL_WIDTH - 16, rowY - 12, TerminalPluginPanel.COLOR_DIM, false);
        }

        int buttonY = rowY + 2;
        int buttonX = x + 3;
        buttonX = this.button(graphics, minecraft, buttonX, buttonY, 22,
            TerminalPluginManager.isEnabled(selectedPlugin)
                ? "screen.anvilcraft_terminal_plugins.panel.disable"
                : "screen.anvilcraft_terminal_plugins.panel.enable",
            PluginActionPacket.TOGGLE_ENABLED, this.selected, -1, mouseX, mouseY);
        buttonX = this.button(graphics, minecraft, buttonX, buttonY, 14,
            "screen.anvilcraft_terminal_plugins.panel.up", PluginActionPacket.MOVE_UP, this.selected, -1, mouseX, mouseY);
        buttonX = this.button(graphics, minecraft, buttonX, buttonY, 14,
            "screen.anvilcraft_terminal_plugins.panel.down", PluginActionPacket.MOVE_DOWN, this.selected, -1, mouseX, mouseY);
        buttonX = this.button(graphics, minecraft, buttonX, buttonY, 20,
            "screen.anvilcraft_terminal_plugins.panel.remove", PluginActionPacket.REMOVE, this.selected, -1, mouseX, mouseY);
        buttonX = this.button(graphics, minecraft, buttonX, buttonY, 24,
            "screen.anvilcraft_terminal_plugins.panel.settings", TerminalPluginPanel.LOCAL_SETTINGS, this.selected, -1, mouseX, mouseY);

        this.renderTips(graphics, minecraft, mouseX, mouseY);
        this.renderFooter(graphics, minecraft, x, y + height);
    }

    private int button(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, String translationKey,
                       int action, int pluginIndex, int entryIndex, int mouseX, int mouseY) {
        String label = TerminalPluginPanel.tr(translationKey);
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 12;
        graphics.fill(x, y, x + width, y + 12, hovered ? TerminalPluginPanel.COLOR_BUTTON_HOVER : TerminalPluginPanel.COLOR_BUTTON);
        graphics.drawString(minecraft.font, label, x + (width - minecraft.font.width(label)) / 2, y + 2,
            TerminalPluginPanel.COLOR_TEXT, false);
        this.addRegion(new Region(x, y, width, 12, action, pluginIndex, entryIndex), 0.0F, null);
        return x + width + 2;
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
        graphics.drawString(minecraft.font, this.open ? "v" : ">", x + TerminalPluginPanel.BUTTON_WIDTH - 8, y + 3,
            TerminalPluginPanel.COLOR_DIM, false);
    }

    private void renderFooter(GuiGraphics graphics, Minecraft minecraft, int x, int y) {
        graphics.drawString(minecraft.font,
            Component.translatable("screen.anvilcraft_terminal_plugins.panel.drag_hint"),
            x + 4, y - 11, TerminalPluginPanel.COLOR_DIM, false);
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
}
