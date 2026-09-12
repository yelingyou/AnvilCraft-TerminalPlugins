/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for full license text.
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
 * 铁砧加工插件配置。
 *
 * <p>对应铁砧工艺手册里的「铁砧加工」：让铁砧落在特定方块上加工物品。
 * 插件把这件事搬进界面里**手动批量**做 —— 不做自动化，只有按下按钮才会执行。</p>
 *
 * @param process 加工方式
 * @param batch   一次最多加工多少次
 */
public record AnvilProcessSettings(AnvilProcess process, int batch) {
    public static final int[] BATCHES = {1, 8, 64};

    public static final AnvilProcessSettings DEFAULT = new AnvilProcessSettings(AnvilProcess.STAMPING, 1);

    public static final Codec<AnvilProcessSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        AnvilProcess.CODEC.optionalFieldOf("process", AnvilProcess.STAMPING).forGetter(AnvilProcessSettings::process),
        Codec.INT.optionalFieldOf("batch", 1).forGetter(AnvilProcessSettings::batch)
    ).apply(instance, AnvilProcessSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AnvilProcessSettings> STREAM_CODEC = StreamCodec.composite(
        AnvilProcess.STREAM_CODEC,
        AnvilProcessSettings::process,
        ByteBufCodecs.VAR_INT,
        AnvilProcessSettings::batch,
        AnvilProcessSettings::new
    );

    public AnvilProcessSettings nextProcess() {
        AnvilProcess[] values = AnvilProcess.values();
        return new AnvilProcessSettings(values[(this.process.ordinal() + 1) % values.length], this.batch);
    }

    public AnvilProcessSettings nextBatch() {
        for (int candidate : AnvilProcessSettings.BATCHES) {
            if (candidate > this.batch) {
                return new AnvilProcessSettings(this.process, candidate);
            }
        }
        return new AnvilProcessSettings(this.process, AnvilProcessSettings.BATCHES[0]);
    }

    public AnvilProcessSettings withBatch(int value) {
        return new AnvilProcessSettings(this.process, Math.clamp(value, 1, 64));
    }

    /**
     * 支持的铁砧加工方式。
     *
     * <p>只收录**纯物品进出**的那些：固液反应 / 快速烹饪需要炼药锅里的水，方块粉碎 / 压合 / 涂抹 / 压榨
     * 的作用对象是方块而不是物品，都不适合「存储物品 + 界面里点一下」的形式，因此不收录。</p>
     */
    public enum AnvilProcess implements StringRepresentable {
        /** 冲压：铁锭 → 压力板这类薄片。 */
        STAMPING,
        /** 粉碎：回收工具装备、头颅 → 骨粉等。 */
        CRUSH,
        /** 压缩：执行 2×2 / 3×3 合成（9 铁粒 → 铁锭）。 */
        COMPRESS,
        /** 分解：执行 1→n 合成（1 铁锭 → 9 铁粒）。 */
        UNPACK,
        /** 过筛：砂砾过筛之类，额外产出约一半原料。 */
        MESH,
        /** 超加热：加热器高温下的加工。 */
        SUPER_HEATING,
        /** 时移：腐化信标/时空类的转化。 */
        TIME_WARP,
        /** 质量注入：把物品注入另一个物品。 */
        ITEM_INJECT,
        /** 中子辐照。 */
        NEUTRON_IRRADIATION,
        ;

        public static final Codec<AnvilProcess> CODEC = StringRepresentable.fromEnum(AnvilProcess::values);
        public static final StreamCodec<ByteBuf, AnvilProcess> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}