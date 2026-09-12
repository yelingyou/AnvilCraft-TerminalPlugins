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
 * 工具切换插件配置。
 *
 * <p>语义说明（刻意与本体「智能补货」区分）：本体的补货只在**手持物品用完时补满一组**，
 * 对不可堆叠的工具没有意义；本插件做的是**磨损预防**：手持工具的剩余耐久低于阈值时，
 * 从存储里换一把**剩余耐久更多**的同种工具到同一个槽位，旧工具放回存储留作修复材料。</p>
 *
 * @param thresholdPercent 剩余耐久百分比低于该值时触发（5 / 10 / 25 / 50）
 * @param target           作用槽位：仅主手 / 整个快捷栏
 * @param returnWorn       换下来的旧工具是否放回存储（否则留在背包，背包也满则掉在地上）
 */
public record ToolSwapSettings(int thresholdPercent, SwapTarget target, boolean returnWorn) {
    public static final int[] THRESHOLDS = {5, 10, 25, 50};

    public static final ToolSwapSettings DEFAULT = new ToolSwapSettings(10, SwapTarget.MAIN_HAND, true);

    public static final Codec<ToolSwapSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf("threshold_percent", 10).forGetter(ToolSwapSettings::thresholdPercent),
        SwapTarget.CODEC.optionalFieldOf("target", SwapTarget.MAIN_HAND).forGetter(ToolSwapSettings::target),
        Codec.BOOL.optionalFieldOf("return_worn", true).forGetter(ToolSwapSettings::returnWorn)
    ).apply(instance, ToolSwapSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToolSwapSettings> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        ToolSwapSettings::thresholdPercent,
        SwapTarget.STREAM_CODEC,
        ToolSwapSettings::target,
        ByteBufCodecs.BOOL,
        ToolSwapSettings::returnWorn,
        ToolSwapSettings::new
    );

    /** 主档位：阈值 5% → 10% → 25% → 50% 循环。 */
    public ToolSwapSettings nextThreshold() {
        for (int threshold : ToolSwapSettings.THRESHOLDS) {
            if (threshold > this.thresholdPercent) {
                return new ToolSwapSettings(threshold, this.target, this.returnWorn);
            }
        }
        return new ToolSwapSettings(ToolSwapSettings.THRESHOLDS[0], this.target, this.returnWorn);
    }

    public ToolSwapSettings nextTarget() {
        SwapTarget[] values = SwapTarget.values();
        return new ToolSwapSettings(
            this.thresholdPercent,
            values[(this.target.ordinal() + 1) % values.length],
            this.returnWorn
        );
    }

    public ToolSwapSettings withReturnWorn(boolean value) {
        return new ToolSwapSettings(this.thresholdPercent, this.target, value);
    }

    /** 作用范围：只换主手，还是快捷栏 9 格都维护。 */
    public enum SwapTarget implements StringRepresentable {
        MAIN_HAND,
        ALL_HOTBAR,
        ;

        public static final Codec<SwapTarget> CODEC = StringRepresentable.fromEnum(SwapTarget::values);
        public static final StreamCodec<ByteBuf, SwapTarget> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}