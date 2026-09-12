package dev.anvilcraft.addon.terminalplugins.plugin.impl;

import dev.anvilcraft.addon.terminalplugins.component.DepositSettings;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.network.PluginActionPacket;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginContext;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginKind;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPlugin;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

// 一键存入：把玩家背包里匹配过滤表的物品一次性存进终端连接的存储。
// 由面板上的「立即存入」按钮触发（不占用右键，因为右键是打开终端界面）。
public class DepositPlugin implements TerminalPlugin {
    @Override
    public PluginKind kind() {
        return PluginKind.DEPOSIT;
    }

    @Override
    public int intervalTicks() {
        return 200;
    }

    @Override
    public boolean onAction(PluginContext context, int action) {
        if (action != PluginActionPacket.DEPOSIT_NOW || context.player() == null) {
            return false;
        }
        return DepositPlugin.depositNow(context);
    }

    // 返回是否真的存进去了东西
    private static boolean depositNow(PluginContext context) {
        ServerPlayer player = context.player();
        if (player == null || !context.storageReachable()) {
            return false;
        }
        DepositSettings settings = context.pluginStack().getOrDefault(
            AddonDataComponents.DEPOSIT_SETTINGS, DepositSettings.DEFAULT);
        FilterContent filter = context.pluginStack().get(ModComponents.FILTER_CONTENT);
        Inventory inventory = player.getInventory();
        ItemStack terminal = context.terminalStack();
        boolean moved = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || stack == terminal) {
                continue;
            }
            if (settings.skipHotbar() && slot < 9) {
                continue;
            }
            if (settings.skipArmor() && slot >= 36) {
                continue;
            }
            if (filter != null && !filter.filter(stack)) {
                continue;
            }
            int inserted = context.insertIntoStorage(stack);
            if (inserted <= 0) {
                continue;
            }
            stack.shrink(inserted);
            if (stack.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
            moved = true;
        }
        if (moved) {
            inventory.setChanged();
            player.containerMenu.broadcastChanges();
        }
        return moved;
    }
}