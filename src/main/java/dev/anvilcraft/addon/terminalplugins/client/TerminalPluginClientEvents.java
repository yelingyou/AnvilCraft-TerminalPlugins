package dev.anvilcraft.addon.terminalplugins.client;

import dev.anvilcraft.addon.terminalplugins.AnvilCraftTerminalPlugins;
import dev.anvilcraft.addon.terminalplugins.client.screen.PluginStationScreen;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.Optional;
import java.util.function.Supplier;

// 客户端接入：把插件面板叠加画在铁砧工艺的存储界面（终端界面）与安装台界面之上。
// 用叠加层而不是新开 Screen，所以不会关闭玩家已经打开的终端界面，也不需要 mixin。
@EventBusSubscriber(modid = AnvilCraftTerminalPlugins.MOD_ID, value = Dist.CLIENT)
public class TerminalPluginClientEvents {
    private static TerminalPluginPanel panel;

    public static TerminalPluginPanel panel() {
        if (TerminalPluginClientEvents.panel == null) {
            TerminalPluginClientEvents.panel = new TerminalPluginPanel();
        }
        return TerminalPluginClientEvents.panel;
    }

    private static boolean isOurScreen(Object screen) {
        return screen instanceof StorageScreen || screen instanceof PluginStationScreen;
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (TerminalPluginClientEvents.isOurScreen(event.getScreen())) {
            TerminalPluginClientEvents.panel().render(
                event.getGuiGraphics(),
                Minecraft.getInstance(),
                (int) event.getMouseX(),
                (int) event.getMouseY()
            );
        } else if (TerminalPluginClientEvents.panel().isOpen()) {
            TerminalPluginClientEvents.panel().close();
        }
    }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!TerminalPluginClientEvents.isOurScreen(event.getScreen())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (TerminalPluginClientEvents.panel().mouseClicked(
            minecraft, event.getMouseX(), event.getMouseY(), event.getButton()
        )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!TerminalPluginClientEvents.isOurScreen(event.getScreen())) {
            return;
        }
        if (TerminalPluginClientEvents.panel().mouseDragged(
            Minecraft.getInstance(), event.getMouseX(), event.getMouseY()
        )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!TerminalPluginClientEvents.isOurScreen(event.getScreen())) {
            return;
        }
        if (TerminalPluginClientEvents.panel().mouseReleased(Minecraft.getInstance())) {
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

    // 服务端反馈（操作失败等）：显示在快捷栏上方
    public static void showFeedback(String translationKey) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable(translationKey), true);
        }
    }

    // 供安装台界面调用：针对安装台里的终端打开面板。
    public static void openForStation(Supplier<ItemStack> terminalSupplier, BlockPos pos) {
        TerminalPluginClientEvents.panel().open(
            new TerminalPluginPanel.Target(Optional.of(pos), -1, terminalSupplier)
        );
    }
}
