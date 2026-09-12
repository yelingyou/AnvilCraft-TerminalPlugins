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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 生物捕捉插件配置。
 *
 * <p>捕捉本身完全交给铁砧工艺的 {@code HasMobBlockItem#canMobBeSaved / saveMobInItem}，
 * 也就是说「体积限制、敌对生物需要虚弱」这些规则由本体判定，本附属只负责**自动化**：
 * 从存储取空树脂块 → 找最近的合法目标 → 存回装着生物的树脂块。</p>
 *
 * @param radius       搜索半径（格）
 * @param allowHostile 是否尝试捕捉敌对 / 中立生物（它们需要先被施加虚弱才能成功）
 */
public record MobCatcherSettings(int radius, boolean allowHostile) {
    public static final int[] RADII = {4, 8, 12, 16};

    public static final MobCatcherSettings DEFAULT = new MobCatcherSettings(8, false);

    public static final Codec<MobCatcherSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf("radius", 8).forGetter(MobCatcherSettings::radius),
        Codec.BOOL.optionalFieldOf("allow_hostile", false).forGetter(MobCatcherSettings::allowHostile)
    ).apply(instance, MobCatcherSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MobCatcherSettings> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        MobCatcherSettings::radius,
        ByteBufCodecs.BOOL,
        MobCatcherSettings::allowHostile,
        MobCatcherSettings::new
    );

    /** 主档位：半径 4 → 8 → 12 → 16 循环。 */
    public MobCatcherSettings nextRadius() {
        for (int candidate : MobCatcherSettings.RADII) {
            if (candidate > this.radius) {
                return new MobCatcherSettings(candidate, this.allowHostile);
            }
        }
        return new MobCatcherSettings(MobCatcherSettings.RADII[0], this.allowHostile);
    }

    public MobCatcherSettings withAllowHostile(boolean value) {
        return new MobCatcherSettings(this.radius, value);
    }
}