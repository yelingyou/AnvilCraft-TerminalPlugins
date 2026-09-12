package dev.anvilcraft.addon.terminalplugins.plugin.impl;

import dev.anvilcraft.addon.terminalplugins.component.AnvilRepairSettings;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginContext;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginKind;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPlugin;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.world.item.ItemStack;

// 铁砧修复插件：每次消耗 1 个过滤表里的材料，修复存储中一件受损装备的耐久。
public class AnvilRepairPlugin implements TerminalPlugin {
    @Override
    public PluginKind kind() {
        return PluginKind.ANVIL_REPAIR;
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
        ItemStack damaged = context.storage().extractFirst(
            stack -> stack.isDamageableItem() && stack.isDamaged(), 1);
        if (damaged.isEmpty()) {
            return;
        }
        ItemStack material = context.storage().extractFirst(filter::filter, 1);
        if (material.isEmpty()) {
            // 没有材料：把装备放回去
            context.insertIntoStorage(damaged);
            return;
        }
        AnvilRepairSettings settings = context.pluginStack().getOrDefault(
            AddonDataComponents.ANVIL_REPAIR_SETTINGS, AnvilRepairSettings.DEFAULT);
        int repair = settings.repairPerMaterial() > 0
            ? settings.repairPerMaterial()
            : Math.max(1, damaged.getMaxDamage() / 4);
        damaged.setDamageValue(Math.max(0, damaged.getDamageValue() - repair));
        context.insertIntoStorage(damaged);
    }
}