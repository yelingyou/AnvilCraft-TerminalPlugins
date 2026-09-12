/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.init;

import dev.anvilcraft.addon.terminalplugins.AnvilCraftTerminalPlugins;
import dev.anvilcraft.addon.terminalplugins.component.AlchemySettings;
import dev.anvilcraft.addon.terminalplugins.component.AutoCookingSettings;
import dev.anvilcraft.addon.terminalplugins.component.CompactingSettings;
import dev.anvilcraft.addon.terminalplugins.component.AnvilProcessSettings;
import dev.anvilcraft.addon.terminalplugins.component.ChargingSettings;
import dev.anvilcraft.addon.terminalplugins.component.FluidSettings;
import dev.anvilcraft.addon.terminalplugins.component.MobCatcherSettings;
import dev.anvilcraft.addon.terminalplugins.component.SmithingSettings;
import dev.anvilcraft.addon.terminalplugins.component.ToolSwapSettings;
import dev.anvilcraft.addon.terminalplugins.component.XpPumpSettings;
import dev.anvilcraft.addon.terminalplugins.component.VoidSettings;
import dev.anvilcraft.addon.terminalplugins.component.FeedingSettings;
import dev.anvilcraft.addon.terminalplugins.component.InstalledPlugins;
import dev.anvilcraft.addon.terminalplugins.component.MagnetSettings;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

public class AddonDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DR = DeferredRegister.create(
        Registries.DATA_COMPONENT_TYPE,
        AnvilCraftTerminalPlugins.MOD_ID
    );

    /** 终端物品上已安装的插件列表。 */
    public static final DataComponentType<InstalledPlugins> INSTALLED_PLUGINS = AddonDataComponents.register(
        "installed_plugins",
        b -> b.persistent(InstalledPlugins.CODEC).networkSynchronized(InstalledPlugins.STREAM_CODEC)
    );

    /** 插件自身的启用开关（面板里可以单独关掉某个插件）。 */
    public static final DataComponentType<Boolean> PLUGIN_ENABLED = AddonDataComponents.register(
        "plugin_enabled",
        b -> b.persistent(com.mojang.serialization.Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
    );

    public static final DataComponentType<MagnetSettings> MAGNET_SETTINGS = AddonDataComponents.register(
        "magnet_settings",
        b -> b.persistent(MagnetSettings.CODEC).networkSynchronized(MagnetSettings.STREAM_CODEC)
    );

    public static final DataComponentType<AutoCookingSettings> COOKING_SETTINGS = AddonDataComponents.register(
        "cooking_settings",
        b -> b.persistent(AutoCookingSettings.CODEC).networkSynchronized(AutoCookingSettings.STREAM_CODEC)
    );

    public static final DataComponentType<FeedingSettings> FEEDING_SETTINGS = AddonDataComponents.register(
        "feeding_settings",
        b -> b.persistent(FeedingSettings.CODEC).networkSynchronized(FeedingSettings.STREAM_CODEC)
    );

    /** 销毁插件。 */
    public static final DataComponentType<VoidSettings> VOID_SETTINGS = AddonDataComponents.register(
        "void_settings",
        b -> b.persistent(VoidSettings.CODEC).networkSynchronized(VoidSettings.STREAM_CODEC)
    );

    /** 压缩插件。 */
    public static final DataComponentType<CompactingSettings> COMPACTING_SETTINGS = AddonDataComponents.register(
        "compacting_settings",
        b -> b.persistent(CompactingSettings.CODEC).networkSynchronized(CompactingSettings.STREAM_CODEC)
    );

    /** 流体接口插件配置。 */
    public static final DataComponentType<FluidSettings> FLUID_SETTINGS = AddonDataComponents.register(
        "fluid_settings",
        b -> b.persistent(FluidSettings.CODEC).networkSynchronized(FluidSettings.STREAM_CODEC)
    );

    /** 流体接口插件的储液缓冲（流体跟随插件物品，拆下即带走）。 */
    public static final DataComponentType<net.neoforged.neoforge.fluids.FluidStack> FLUID_BUFFER =
        AddonDataComponents.register(
            "fluid_buffer",
            b -> b.persistent(net.neoforged.neoforge.fluids.FluidStack.CODEC)
                .networkSynchronized(net.neoforged.neoforge.fluids.FluidStack.STREAM_CODEC)
        );

    /** 工具切换插件配置。 */
    public static final DataComponentType<ToolSwapSettings> TOOL_SWAP_SETTINGS = AddonDataComponents.register(
        "tool_swap_settings",
        b -> b.persistent(ToolSwapSettings.CODEC).networkSynchronized(ToolSwapSettings.STREAM_CODEC)
    );

    /** 生物捕捉插件配置。 */
    public static final DataComponentType<MobCatcherSettings> MOB_CATCHER_SETTINGS =
        AddonDataComponents.register(
            "mob_catcher_settings",
            b -> b.persistent(MobCatcherSettings.CODEC).networkSynchronized(MobCatcherSettings.STREAM_CODEC)
        );

    /** 经验泵插件配置。 */
    public static final DataComponentType<XpPumpSettings> XP_PUMP_SETTINGS = AddonDataComponents.register(
        "xp_pump_settings",
        b -> b.persistent(XpPumpSettings.CODEC).networkSynchronized(XpPumpSettings.STREAM_CODEC)
    );

    /** 锻造插件配置。 */
    public static final DataComponentType<SmithingSettings> SMITHING_SETTINGS = AddonDataComponents.register(
        "smithing_settings",
        b -> b.persistent(SmithingSettings.CODEC).networkSynchronized(SmithingSettings.STREAM_CODEC)
    );

    /** 充能插件配置（含充能配方进度）。 */
    public static final DataComponentType<ChargingSettings> CHARGING_SETTINGS = AddonDataComponents.register(
        "charging_settings",
        b -> b.persistent(ChargingSettings.CODEC).networkSynchronized(ChargingSettings.STREAM_CODEC)
    );

    /** 铁砧加工插件配置。 */
    public static final DataComponentType<AnvilProcessSettings> ANVIL_PROCESS_SETTINGS =
        AddonDataComponents.register(
            "anvil_process_settings",
            b -> b.persistent(AnvilProcessSettings.CODEC).networkSynchronized(AnvilProcessSettings.STREAM_CODEC)
        );

    /** 炼金插件：条件自动用药（对齐精妙背包炼金升级语义）。 */
    public static final DataComponentType<AlchemySettings> ALCHEMY_SETTINGS = AddonDataComponents.register(
        "alchemy_settings",
        b -> b.persistent(AlchemySettings.CODEC).networkSynchronized(AlchemySettings.STREAM_CODEC)
    );

    private static <T> DataComponentType<T> register(String name, Consumer<DataComponentType.Builder<T>> customizer) {
        DataComponentType.Builder<T> builder = DataComponentType.builder();
        customizer.accept(builder);
        DataComponentType<T> type = builder.build();
        AddonDataComponents.DR.register(name, () -> type);
        return type;
    }

    public static void register(IEventBus modEventBus) {
        AddonDataComponents.DR.register(modEventBus);
    }
}
