/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * 自动熔炼 · 烹饪插件配置。
 *
 * @param mode          使用哪种原版烹饪配方类型
 * @param intervalTicks 每处理一批物品所需的间隔（tick）
 * @param consumeFuel   是否消耗燃料（煤炭等，按原版燃烧时间计算）
 */
public record AutoCookingSettings(AutoCookingSettings.CookingMode mode, int intervalTicks, boolean consumeFuel) {
    public static final AutoCookingSettings DEFAULT = new AutoCookingSettings(AutoCookingSettings.CookingMode.SMELTING, 40, true);

    public static final Codec<AutoCookingSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        AutoCookingSettings.CookingMode.CODEC.optionalFieldOf("mode", AutoCookingSettings.CookingMode.SMELTING)
            .forGetter(AutoCookingSettings::mode),
        Codec.INT.optionalFieldOf("interval", 40).forGetter(AutoCookingSettings::intervalTicks),
        Codec.BOOL.optionalFieldOf("consume_fuel", true).forGetter(AutoCookingSettings::consumeFuel)
    ).apply(instance, AutoCookingSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AutoCookingSettings> STREAM_CODEC = StreamCodec.composite(
        AutoCookingSettings.CookingMode.STREAM_CODEC,
        AutoCookingSettings::mode,
        ByteBufCodecs.VAR_INT,
        AutoCookingSettings::intervalTicks,
        ByteBufCodecs.BOOL,
        AutoCookingSettings::consumeFuel,
        AutoCookingSettings::new
    );

    public AutoCookingSettings nextMode() {
        AutoCookingSettings.CookingMode[] values = AutoCookingSettings.CookingMode.values();
        return new AutoCookingSettings(values[(this.mode.ordinal() + 1) % values.length], this.intervalTicks, this.consumeFuel);
    }

    public AutoCookingSettings withFuel(boolean value) {
        return new AutoCookingSettings(this.mode, this.intervalTicks, value);
    }

    public enum CookingMode implements StringRepresentable {
        SMELTING,
        BLASTING,
        SMOKING,
        CAMPFIRE;

        public static final Codec<CookingMode> CODEC = StringRepresentable.fromEnum(CookingMode::values);
        public static final StreamCodec<ByteBuf, CookingMode> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
