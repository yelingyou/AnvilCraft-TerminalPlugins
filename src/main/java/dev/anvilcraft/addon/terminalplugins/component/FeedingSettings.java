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
 * 自动喂食插件配置。
 *
 * @param hungerThreshold 饥饿值低于该值时自动进食
 * @param allowHarmful    是否允许使用带负面效果的食物（腐肉、河豚等）
 * @param keepSaturation  {@code true} 时只在饱和度也为空时进食
 */
public record FeedingSettings(int hungerThreshold, boolean allowHarmful, boolean keepSaturation) {
    public static final FeedingSettings DEFAULT = new FeedingSettings(6, false, false);

    public static final Codec<FeedingSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf("hunger_threshold", 6).forGetter(FeedingSettings::hungerThreshold),
        Codec.BOOL.optionalFieldOf("allow_harmful", false).forGetter(FeedingSettings::allowHarmful),
        Codec.BOOL.optionalFieldOf("keep_saturation", false).forGetter(FeedingSettings::keepSaturation)
    ).apply(instance, FeedingSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FeedingSettings> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        FeedingSettings::hungerThreshold,
        ByteBufCodecs.BOOL,
        FeedingSettings::allowHarmful,
        ByteBufCodecs.BOOL,
        FeedingSettings::keepSaturation,
        FeedingSettings::new
    );
}
