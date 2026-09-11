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
 * 磁吸 / 拾取插件配置。
 *
 * @param range         工作半径（格）
 * @param magnetEnabled 是否启用「磁吸」（把附近掉落物拉向玩家）
 * @param pickupEnabled 是否启用「拾取」（直接收进终端连接的存储）
 * @param allowDrops    {@code false} 时只处理掉落物拾取，不处理经验球等其它实体
 */
public record MagnetSettings(int range, boolean magnetEnabled, boolean pickupEnabled, boolean allowDrops) {
    public static final MagnetSettings DEFAULT = new MagnetSettings(5, true, true, true);

    public static final Codec<MagnetSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf("range", 5).forGetter(MagnetSettings::range),
        Codec.BOOL.optionalFieldOf("magnet", true).forGetter(MagnetSettings::magnetEnabled),
        Codec.BOOL.optionalFieldOf("pickup", true).forGetter(MagnetSettings::pickupEnabled),
        Codec.BOOL.optionalFieldOf("allow_drops", true).forGetter(MagnetSettings::allowDrops)
    ).apply(instance, MagnetSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MagnetSettings> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        MagnetSettings::range,
        ByteBufCodecs.BOOL,
        MagnetSettings::magnetEnabled,
        ByteBufCodecs.BOOL,
        MagnetSettings::pickupEnabled,
        ByteBufCodecs.BOOL,
        MagnetSettings::allowDrops,
        MagnetSettings::new
    );

    public MagnetSettings withRange(int newRange) {
        return new MagnetSettings(Math.clamp(newRange, 1, 16), this.magnetEnabled, this.pickupEnabled, this.allowDrops);
    }

    public MagnetSettings withMagnet(boolean value) {
        return new MagnetSettings(this.range, value, this.pickupEnabled, this.allowDrops);
    }

    public MagnetSettings withPickup(boolean value) {
        return new MagnetSettings(this.range, this.magnetEnabled, value, this.allowDrops);
    }
}
