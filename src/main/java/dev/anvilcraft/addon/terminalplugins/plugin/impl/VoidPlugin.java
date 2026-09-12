package dev.anvilcraft.addon.terminalplugins.plugin.impl;

import dev.anvilcraft.addon.terminalplugins.component.VoidSettings;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginContext;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginKind;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPlugin;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import net.minecraft.world.item.ItemStack;

// 销毁：存储里匹配过滤表的物品只保留 keepStacks 组，多余部分直接销毁。
public class VoidPlugin implements TerminalPlugin {
    @Override
    public PluginKind kind() {
        return PluginKind.VOID;
    }

    @Override
    public int intervalTicks() {
        return 40;
    }

    @Override
    public void onPlayerTick(PluginContext context) {
        if (!context.storageReachable()) {
            return;
        }
        FilterContent filter = context.pluginStack().get(ModComponents.FILTER_CONTENT);
        if (filter == null) {
            return;
        }
        VoidSettings settings = context.pluginStack().getOrDefault(
            AddonDataComponents.VOID_SETTINGS, VoidSettings.DEFAULT);
        for (BaseStorage<?> storage : context.storage().storages()) {
            UnlimitedItemStacksResourceHandler handler = storage.getItems();
            for (int index = 0; index < handler.size(); index++) {
                UnlimitedItemStack unlimited = handler.getUnlimitedStackInSlot(index);
                if (unlimited.isEmpty()) {
                    continue;
                }
                ItemStack representative = unlimited.getStack();
                if (!filter.filter(representative)) {
                    continue;
                }
                long keep = (long) settings.keepStacks() * representative.getMaxStackSize();
                long excess = unlimited.getCount() - keep;
                if (excess > 0) {
                    handler.extractUnlimited(index, (int) Math.min(excess, Integer.MAX_VALUE), false);
                }
            }
        }
    }
}