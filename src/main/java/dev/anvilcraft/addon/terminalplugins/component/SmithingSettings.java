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
 * 锻造插件配置。
 *
 * <p>本体存储界面的「合成窗口」只覆盖工作台与切石机（见本体手册 `004_block/003_crate.md`），
 * 锻造台是空白，所以这里走 {@code RecipeType.SMITHING}：模板 + 基底 + 附加物 → 成品，
 * 用于下界合金升级、盔甲纹饰这类配方。</p>
 *
 * @param batch 每周期最多完成的锻造次数
 */
public record SmithingSettings(int batch) {
    public static final SmithingSettings DEFAULT = new SmithingSettings(1);

    public static final Codec<SmithingSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf("batch", 1).forGetter(SmithingSettings::batch)
    ).apply(instance, SmithingSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SmithingSettings> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        SmithingSettings::batch,
        SmithingSettings::new
    );

    public SmithingSettings withBatch(int value) {
        return new SmithingSettings(Math.clamp(value, 1, 8));
    }
}