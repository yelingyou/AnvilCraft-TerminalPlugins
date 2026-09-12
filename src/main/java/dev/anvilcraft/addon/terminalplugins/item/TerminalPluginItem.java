/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.item;

import dev.anvilcraft.addon.terminalplugins.component.AlchemySettings;
import dev.anvilcraft.addon.terminalplugins.component.AutoCookingSettings;
import dev.anvilcraft.addon.terminalplugins.component.CompactingSettings;
import dev.anvilcraft.addon.terminalplugins.component.VoidSettings;
import dev.anvilcraft.addon.terminalplugins.component.FeedingSettings;
import dev.anvilcraft.addon.terminalplugins.component.AnvilProcessSettings;
import dev.anvilcraft.addon.terminalplugins.component.ChargingSettings;
import dev.anvilcraft.addon.terminalplugins.component.FluidSettings;
import dev.anvilcraft.addon.terminalplugins.component.MobCatcherSettings;
import dev.anvilcraft.addon.terminalplugins.component.SmithingSettings;
import dev.anvilcraft.addon.terminalplugins.component.ToolSwapSettings;
import dev.anvilcraft.addon.terminalplugins.component.XpPumpSettings;
import dev.anvilcraft.addon.terminalplugins.component.MagnetSettings;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginKind;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 插件物品。行为由 {@link PluginKind} 决定，配置保存在物品自身的组件里。
 *
 * <p>配置方式：手持插件右键切换主档位、潜行右键切换副档位（聊天栏提示当前值）；
 * 更精细的配置（尤其是炼金插件的条目）在「插件面板」里调整 —— 面板可从终端界面或安装台打开。</p>
 */
public class TerminalPluginItem extends Item {
    private static final int[] MAGNET_RADII = {3, 5, 8, 12, 16};
    private static final int[] FEEDING_THRESHOLDS = {2, 4, 6, 8, 12, 16};
    private static final int[] ALCHEMY_RADII = {3, 5, 8, 12};

    private final PluginKind kind;

    public TerminalPluginItem(PluginKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public PluginKind kind() {
        return this.kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }
        Component feedback = TerminalPluginItem.cycleSetting(stack, player.isShiftKeyDown());
        player.displayClientMessage(
            Component.translatable(
                "message.anvilcraft_terminal_plugins.setting",
                this.kind.displayName(),
                feedback
            ),
            true
        );
        return InteractionResultHolder.success(stack);
    }

    /**
     * 切换档位并返回提示文本。安装台与插件面板共用同一套档位逻辑。
     *
     * @param stack     插件物品堆栈（会被就地修改）
     * @param secondary 是否为副档位（潜行右键）
     */
    public static Component cycleSetting(ItemStack stack, boolean secondary) {
        if (!(stack.getItem() instanceof TerminalPluginItem item)) {
            return Component.empty();
        }
        return switch (item.kind()) {
            case MAGNET -> TerminalPluginItem.cycleMagnet(stack, secondary);
            case AUTO_COOKING -> TerminalPluginItem.cycleCooking(stack, secondary);
            case FEEDING -> TerminalPluginItem.cycleFeeding(stack, secondary);
            case ALCHEMY -> TerminalPluginItem.cycleAlchemy(stack, secondary);
            case FILTER -> TerminalPluginItem.cycleFilter(stack, secondary);
            case VOID -> TerminalPluginItem.cycleVoid(stack, secondary);
            case COMPACTING -> TerminalPluginItem.cycleCompacting(stack, secondary);
            case FLUID -> TerminalPluginItem.cycleFluid(stack, secondary);
            case TOOL_SWAP -> TerminalPluginItem.cycleToolSwap(stack, secondary);
            case MOB_CATCHER -> TerminalPluginItem.cycleMobCatcher(stack, secondary);
            case XP_PUMP -> TerminalPluginItem.cycleXpPump(stack, secondary);
            case SMITHING -> TerminalPluginItem.cycleSmithing(stack, secondary);
            case CHARGING -> TerminalPluginItem.cycleCharging(stack, secondary);
            case ANVIL_PROCESS -> TerminalPluginItem.cycleAnvilProcess(stack, secondary);
        };
    }

    private static Component cycleMagnet(ItemStack stack, boolean secondary) {
        MagnetSettings settings = stack.getOrDefault(AddonDataComponents.MAGNET_SETTINGS, MagnetSettings.DEFAULT);
        if (secondary) {
            boolean magnet = settings.magnetEnabled();
            boolean pickup = settings.pickupEnabled();
            boolean nextMagnet;
            boolean nextPickup;
            if (magnet && pickup) {
                nextMagnet = true;
                nextPickup = false;
            } else if (magnet) {
                nextMagnet = false;
                nextPickup = true;
            } else {
                nextMagnet = true;
                nextPickup = true;
            }
            stack.set(
                AddonDataComponents.MAGNET_SETTINGS,
                new MagnetSettings(settings.range(), nextMagnet, nextPickup, settings.allowDrops())
            );
            return Component.translatable(
                "tooltip.anvilcraft_terminal_plugins.magnet.mode." + (nextMagnet ? "magnet" : "pickup")
            );
        }
        int next = TerminalPluginItem.nextInCycle(TerminalPluginItem.MAGNET_RADII, settings.range());
        stack.set(AddonDataComponents.MAGNET_SETTINGS, settings.withRange(next));
        return Component.translatable("tooltip.anvilcraft_terminal_plugins.magnet.range", next);
    }

    private static Component cycleCooking(ItemStack stack, boolean secondary) {
        AutoCookingSettings settings = stack.getOrDefault(
            AddonDataComponents.COOKING_SETTINGS,
            AutoCookingSettings.DEFAULT
        );
        if (secondary) {
            boolean fuel = !settings.consumeFuel();
            stack.set(AddonDataComponents.COOKING_SETTINGS, settings.withFuel(fuel));
            return Component.translatable(fuel
                ? "tooltip.anvilcraft_terminal_plugins.cooking.fuel.on"
                : "tooltip.anvilcraft_terminal_plugins.cooking.fuel.off");
        }
        AutoCookingSettings next = settings.nextMode();
        stack.set(AddonDataComponents.COOKING_SETTINGS, next);
        return Component.translatable(
            "tooltip.anvilcraft_terminal_plugins.cooking.mode",
            Component.translatable(
                "tooltip.anvilcraft_terminal_plugins.cooking.mode." + next.mode().getSerializedName()
            )
        );
    }

    private static Component cycleFeeding(ItemStack stack, boolean secondary) {
        FeedingSettings settings = stack.getOrDefault(
            AddonDataComponents.FEEDING_SETTINGS,
            FeedingSettings.DEFAULT
        );
        if (secondary) {
            boolean allowHarmful = !settings.allowHarmful();
            stack.set(
                AddonDataComponents.FEEDING_SETTINGS,
                new FeedingSettings(settings.hungerThreshold(), allowHarmful, settings.keepSaturation())
            );
            return Component.translatable(allowHarmful
                ? "tooltip.anvilcraft_terminal_plugins.feeding.harmful.on"
                : "tooltip.anvilcraft_terminal_plugins.feeding.harmful.off");
        }
        int next = TerminalPluginItem.nextInCycle(
            TerminalPluginItem.FEEDING_THRESHOLDS,
            settings.hungerThreshold()
        );
        stack.set(
            AddonDataComponents.FEEDING_SETTINGS,
            new FeedingSettings(next, settings.allowHarmful(), settings.keepSaturation())
        );
        return Component.translatable("tooltip.anvilcraft_terminal_plugins.feeding.threshold", next);
    }

    /** 炼金：主档位切换「照顾附近哪些实体」，副档位切换「是否匹配效果等级」。 */
    private static Component cycleAlchemy(ItemStack stack, boolean secondary) {
        AlchemySettings settings = stack.getOrDefault(AddonDataComponents.ALCHEMY_SETTINGS, AlchemySettings.DEFAULT);
        if (secondary) {
            AlchemySettings next = new AlchemySettings(
                settings.normalizedEntries(),
                settings.nearby(),
                settings.radius(),
                settings.interval(),
                !settings.matchAmplifier()
            );
            stack.set(AddonDataComponents.ALCHEMY_SETTINGS, next);
            return Component.translatable(next.matchAmplifier()
                ? "tooltip.anvilcraft_terminal_plugins.alchemy.amplifier.on"
                : "tooltip.anvilcraft_terminal_plugins.alchemy.amplifier.off");
        }
        AlchemySettings next = settings.withNearby(settings.nearby().next());
        stack.set(AddonDataComponents.ALCHEMY_SETTINGS, next);
        return Component.translatable(
            "tooltip.anvilcraft_terminal_plugins.alchemy.nearby",
            Component.translatable("tooltip.anvilcraft_terminal_plugins.alchemy.nearby." + next.nearby().getSerializedName())
        );
    }

    private static final int[] VOID_KEEP_STACKS = {0, 1, 2, 4, 8, 16, 64};

    // 流体接口：主档位切工作模式，副档位切每周期批数
    private static Component cycleFluid(ItemStack stack, boolean secondary) {
        FluidSettings settings = stack.getOrDefault(
            AddonDataComponents.FLUID_SETTINGS, FluidSettings.DEFAULT);
        if (secondary) {
            FluidSettings next = settings.withBatch(FluidSettings.nextBatch(settings.batch()));
            stack.set(AddonDataComponents.FLUID_SETTINGS, next);
            return Component.translatable(
                "screen.anvilcraft_terminal_plugins.setting.fluid_batch", next.batch());
        }
        FluidSettings next = settings.nextMode();
        stack.set(AddonDataComponents.FLUID_SETTINGS, next);
        return Component.translatable(
            "screen.anvilcraft_terminal_plugins.setting.fluid_mode",
            Component.translatable("screen.anvilcraft_terminal_plugins.fluid_mode."
                + next.mode().getSerializedName()));
    }

    // 工具切换：主档位切耐久阈值，副档位切作用槽位
    private static Component cycleToolSwap(ItemStack stack, boolean secondary) {
        ToolSwapSettings settings = stack.getOrDefault(
            AddonDataComponents.TOOL_SWAP_SETTINGS, ToolSwapSettings.DEFAULT);
        if (secondary) {
            ToolSwapSettings next = settings.nextTarget();
            stack.set(AddonDataComponents.TOOL_SWAP_SETTINGS, next);
            return Component.translatable(
                "screen.anvilcraft_terminal_plugins.setting.tool_swap_target",
                Component.translatable("screen.anvilcraft_terminal_plugins.swap_target."
                    + next.target().getSerializedName()));
        }
        ToolSwapSettings next = settings.nextThreshold();
        stack.set(AddonDataComponents.TOOL_SWAP_SETTINGS, next);
        return Component.translatable(
            "screen.anvilcraft_terminal_plugins.setting.tool_swap_threshold", next.thresholdPercent());
    }

    // 生物捕捉：主档位切半径，副档位切是否连敌对生物一起抓
    private static Component cycleMobCatcher(ItemStack stack, boolean secondary) {
        MobCatcherSettings settings = stack.getOrDefault(
            AddonDataComponents.MOB_CATCHER_SETTINGS, MobCatcherSettings.DEFAULT);
        if (secondary) {
            MobCatcherSettings next = settings.withAllowHostile(!settings.allowHostile());
            stack.set(AddonDataComponents.MOB_CATCHER_SETTINGS, next);
            return Component.translatable(
                "screen.anvilcraft_terminal_plugins.setting.mob_catcher_hostile",
                Component.translatable(next.allowHostile()
                    ? "screen.anvilcraft_terminal_plugins.setting.on"
                    : "screen.anvilcraft_terminal_plugins.setting.off"));
        }
        MobCatcherSettings next = settings.nextRadius();
        stack.set(AddonDataComponents.MOB_CATCHER_SETTINGS, next);
        return Component.translatable(
            "screen.anvilcraft_terminal_plugins.setting.range", next.radius());
    }

    // 经验泵：主档位切模式，副档位切每周期宝石数
    private static Component cycleXpPump(ItemStack stack, boolean secondary) {
        XpPumpSettings settings = stack.getOrDefault(
            AddonDataComponents.XP_PUMP_SETTINGS, XpPumpSettings.DEFAULT);
        if (secondary) {
            XpPumpSettings next = settings.withGemsPerCycle(XpPumpSettings.nextBatch(settings.gemsPerCycle()));
            stack.set(AddonDataComponents.XP_PUMP_SETTINGS, next);
            return Component.translatable(
                "screen.anvilcraft_terminal_plugins.setting.xp_gems_per_cycle", next.gemsPerCycle());
        }
        XpPumpSettings next = settings.nextMode();
        stack.set(AddonDataComponents.XP_PUMP_SETTINGS, next);
        return Component.translatable(
            "screen.anvilcraft_terminal_plugins.setting.xp_mode",
            Component.translatable("screen.anvilcraft_terminal_plugins.xp_mode."
                + next.mode().getSerializedName()));
    }

    // 锻造：切换每周期锻造次数
    private static Component cycleSmithing(ItemStack stack, boolean secondary) {
        SmithingSettings settings = stack.getOrDefault(
            AddonDataComponents.SMITHING_SETTINGS, SmithingSettings.DEFAULT);
        int next = TerminalPluginItem.nextInCycle(TerminalPluginItem.COMPACTING_BATCHES, settings.batch());
        stack.set(AddonDataComponents.SMITHING_SETTINGS, settings.withBatch(next));
        return Component.translatable("screen.anvilcraft_terminal_plugins.setting.batch", next);
    }

    // 充能：主档位切申请功率，副档位切「是否跑充能配方」
    private static Component cycleCharging(ItemStack stack, boolean secondary) {
        ChargingSettings settings = stack.getOrDefault(
            AddonDataComponents.CHARGING_SETTINGS, ChargingSettings.DEFAULT);
        if (secondary) {
            ChargingSettings next = settings.withRunRecipes(!settings.runRecipes());
            stack.set(AddonDataComponents.CHARGING_SETTINGS, next);
            return Component.translatable(
                "screen.anvilcraft_terminal_plugins.setting.charging_recipes",
                Component.translatable(next.runRecipes()
                    ? "screen.anvilcraft_terminal_plugins.setting.on"
                    : "screen.anvilcraft_terminal_plugins.setting.off"));
        }
        ChargingSettings next = settings.nextPower();
        stack.set(AddonDataComponents.CHARGING_SETTINGS, next);
        return Component.translatable(
            "screen.anvilcraft_terminal_plugins.setting.charging_power", next.powerKw());
    }

    // 铁砧加工：主档位切加工方式，副档位切批量
    private static Component cycleAnvilProcess(ItemStack stack, boolean secondary) {
        AnvilProcessSettings settings = stack.getOrDefault(
            AddonDataComponents.ANVIL_PROCESS_SETTINGS, AnvilProcessSettings.DEFAULT);
        if (secondary) {
            AnvilProcessSettings next = settings.nextBatch();
            stack.set(AddonDataComponents.ANVIL_PROCESS_SETTINGS, next);
            return Component.translatable(
                "screen.anvilcraft_terminal_plugins.setting.process_batch", next.batch());
        }
        AnvilProcessSettings next = settings.nextProcess();
        stack.set(AddonDataComponents.ANVIL_PROCESS_SETTINGS, next);
        return Component.translatable(
            "screen.anvilcraft_terminal_plugins.setting.process",
            Component.translatable("screen.anvilcraft_terminal_plugins.anvil_process."
                + next.process().getSerializedName()));
    }

    // 销毁：切换保留组数
    private static Component cycleVoid(ItemStack stack, boolean secondary) {
        VoidSettings settings = stack.getOrDefault(AddonDataComponents.VOID_SETTINGS, VoidSettings.DEFAULT);
        int next = TerminalPluginItem.nextInCycle(TerminalPluginItem.VOID_KEEP_STACKS, settings.keepStacks());
        stack.set(AddonDataComponents.VOID_SETTINGS, settings.withKeepStacks(next));
        return Component.translatable("screen.anvilcraft_terminal_plugins.setting.keep_stacks", next);
    }

    private static final int[] COMPACTING_BATCHES = {1, 2, 4, 8, 16};

    // 压缩：切换每次处理组数
    private static Component cycleCompacting(ItemStack stack, boolean secondary) {
        CompactingSettings settings = stack.getOrDefault(
            AddonDataComponents.COMPACTING_SETTINGS, CompactingSettings.DEFAULT);
        int next = TerminalPluginItem.nextInCycle(TerminalPluginItem.COMPACTING_BATCHES, settings.batch());
        stack.set(AddonDataComponents.COMPACTING_SETTINGS, settings.withBatch(next));
        return Component.translatable("screen.anvilcraft_terminal_plugins.setting.batch", next);
    }


    private static Component cycleFilter(ItemStack stack, boolean secondary) {
        FilterContent content = stack.getOrDefault(ModComponents.FILTER_CONTENT, new FilterContent());
        if (secondary) {
            FilterContent next = content.setIncludeComponents(!content.includeComponents());
            stack.set(ModComponents.FILTER_CONTENT, next);
            return Component.translatable(next.includeComponents()
                ? "tooltip.anvilcraft_terminal_plugins.filter.components.on"
                : "tooltip.anvilcraft_terminal_plugins.filter.components.off");
        }
        FilterContent next = content.setBlackList(!content.blackList());
        stack.set(ModComponents.FILTER_CONTENT, next);
        return Component.translatable(next.blackList()
            ? "tooltip.anvilcraft_terminal_plugins.filter.mode.blacklist"
            : "tooltip.anvilcraft_terminal_plugins.filter.mode.whitelist");
    }

    /** 在循环档位表里取"当前值的下一个"（当前值不在表中时取第一个比它大的，否则回到第一项）。 */
    private static int nextInCycle(int[] values, int current) {
        for (int value : values) {
            if (value > current) {
                return value;
            }
        }
        return values[0];
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(this.kind.descriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        switch (this.kind) {
            case FILTER -> {
                FilterContent content = stack.get(ModComponents.FILTER_CONTENT);
                if (content != null) {
                    tooltip.add(Component.translatable(
                        "tooltip.anvilcraft_terminal_plugins.filter.mode",
                        Component.translatable(content.blackList()
                            ? "tooltip.anvilcraft_terminal_plugins.filter.mode.blacklist"
                            : "tooltip.anvilcraft_terminal_plugins.filter.mode.whitelist")
                    ).withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            case MAGNET -> {
                MagnetSettings settings = stack.getOrDefault(AddonDataComponents.MAGNET_SETTINGS, MagnetSettings.DEFAULT);
                tooltip.add(Component.translatable(
                    "tooltip.anvilcraft_terminal_plugins.magnet.range", settings.range()
                ).withStyle(ChatFormatting.DARK_GRAY));
            }
            case AUTO_COOKING -> {
                AutoCookingSettings settings = stack.getOrDefault(
                    AddonDataComponents.COOKING_SETTINGS,
                    AutoCookingSettings.DEFAULT
                );
                tooltip.add(Component.translatable(
                    "tooltip.anvilcraft_terminal_plugins.cooking.mode",
                    Component.translatable(
                        "tooltip.anvilcraft_terminal_plugins.cooking.mode." + settings.mode().getSerializedName()
                    )
                ).withStyle(ChatFormatting.DARK_GRAY));
            }
            case FEEDING -> {
                FeedingSettings settings = stack.getOrDefault(
                    AddonDataComponents.FEEDING_SETTINGS,
                    FeedingSettings.DEFAULT
                );
                tooltip.add(Component.translatable(
                    "tooltip.anvilcraft_terminal_plugins.feeding.threshold", settings.hungerThreshold()
                ).withStyle(ChatFormatting.DARK_GRAY));
            }
            case ALCHEMY -> {
                AlchemySettings settings = stack.getOrDefault(
                    AddonDataComponents.ALCHEMY_SETTINGS,
                    AlchemySettings.DEFAULT
                );
                int active = 0;
                for (AlchemySettings.AlchemyEntry entry : settings.normalizedEntries()) {
                    if (!entry.isEmpty()) {
                        active++;
                    }
                }
                tooltip.add(Component.translatable(
                    "tooltip.anvilcraft_terminal_plugins.alchemy.entries",
                    active,
                    AlchemySettings.ENTRY_COUNT
                ).withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.translatable(
                    "tooltip.anvilcraft_terminal_plugins.alchemy.nearby",
                    Component.translatable(
                        "tooltip.anvilcraft_terminal_plugins.alchemy.nearby." + settings.nearby().getSerializedName()
                    )
                ).withStyle(ChatFormatting.DARK_GRAY));
            }
            default -> {
            }
        }
        tooltip.add(Component.translatable("tooltip.anvilcraft_terminal_plugins.plugin.install_hint")
            .withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("tooltip.anvilcraft_terminal_plugins.plugin.configure_hint")
            .withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("tooltip.anvilcraft_terminal_plugins.plugin.panel_hint")
            .withStyle(ChatFormatting.DARK_GREEN));
    }
}
