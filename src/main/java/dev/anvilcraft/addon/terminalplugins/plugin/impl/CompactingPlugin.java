package dev.anvilcraft.addon.terminalplugins.plugin.impl;

import dev.anvilcraft.addon.terminalplugins.component.CompactingSettings;
import dev.anvilcraft.addon.terminalplugins.component.PluginSample;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginContext;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginKind;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPlugin;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

// 压缩插件：存储里 9 个同类物品自动压成 1 个（用原版 3x3 合成配方，只处理过滤表里的物品）。
public class CompactingPlugin implements TerminalPlugin {
    private static final int REQUIRED = 9;

    @Override
    public PluginKind kind() {
        return PluginKind.COMPACTING;
    }

    @Override
    public int intervalTicks() {
        return 40;
    }

    @Override
    public void onPlayerTick(PluginContext context) {
        ServerPlayer player = context.player();
        if (player == null || !context.storageReachable()) {
            return;
        }
        FilterContent filter = context.pluginStack().get(ModComponents.FILTER_CONTENT);
        if (filter == null) {
            return;
        }
        CompactingSettings settings = context.pluginStack().getOrDefault(
            AddonDataComponents.COMPACTING_SETTINGS, CompactingSettings.DEFAULT);
        ServerLevel level = player.serverLevel();
        RecipeManager manager = level.getRecipeManager();

        PluginSample sample = context.pluginStack().getOrDefault(
            AddonDataComponents.PLUGIN_SAMPLE, PluginSample.EMPTY);
        int done = 0;
        for (ItemStack candidate : context.storage().types()) {
            if (done >= settings.batch()) {
                break;
            }
            if (!filter.filter(candidate)) {
                continue;
            }
            // 输入槽指定了就只压这一种
            if (sample.hasSample() && !ItemStack.isSameItemSameComponents(candidate, sample.sample())) {
                continue;
            }
            Predicate<ItemStack> sameType = stack -> ItemStack.isSameItemSameComponents(stack, candidate);
            if (context.storage().count(sameType) < CompactingPlugin.REQUIRED) {
                continue;
            }
            Optional<RecipeHolder<CraftingRecipe>> recipe = CompactingPlugin.findRecipe(manager, level, candidate);
            if (recipe.isEmpty()) {
                continue;
            }
            long taken = context.storage().extractExact(sameType, CompactingPlugin.REQUIRED);
            if (taken < CompactingPlugin.REQUIRED) {
                // 数量在扫描后被改动：把取出的还回去
                for (int index = 0; index < taken; index++) {
                    context.insertIntoStorage(candidate.copyWithCount(1));
                }
                continue;
            }
            ItemStack result = recipe.get().value().assemble(
                CompactingPlugin.input(candidate), level.registryAccess());
            if (result.isEmpty()) {
                for (int index = 0; index < CompactingPlugin.REQUIRED; index++) {
                    context.insertIntoStorage(candidate.copyWithCount(1));
                }
                continue;
            }
            context.insertIntoStorage(result);
            context.pluginStack().set(AddonDataComponents.PLUGIN_SAMPLE, sample.withOutput(result.copy()));
            done++;
        }
    }

    private static CraftingInput input(ItemStack stack) {
        List<ItemStack> list = new ArrayList<>();
        for (int index = 0; index < CompactingPlugin.REQUIRED; index++) {
            list.add(stack.copyWithCount(1));
        }
        return CraftingInput.of(3, 3, list);
    }

    private static Optional<RecipeHolder<CraftingRecipe>> findRecipe(RecipeManager manager, ServerLevel level, ItemStack stack) {
        return manager.getRecipeFor(RecipeType.CRAFTING, CompactingPlugin.input(stack), level);
    }
}