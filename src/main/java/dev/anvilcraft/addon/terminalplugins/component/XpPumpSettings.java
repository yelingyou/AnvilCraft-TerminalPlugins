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
 * 经验泵插件配置。
 *
 * <p>经验的存储形态选的是铁砧工艺本体的 **经验宝石**（`anvilcraft:exp_gem`）：
 * 1 颗 = 50 点玩家经验，正好是「按类型计数」的物品存储能装的东西，
 * 因此不需要给终端引入流体 / 能量那样的第二套存储模型。</p>
 *
 * @param mode         工作模式：关闭 / 存入 / 取出
 * @param storeLevel   等级达到该值以上才把多余经验换成宝石存进存储
 * @param keepLevel    等级低于该值以下时从存储取宝石换回经验
 * @param gemsPerCycle 每周期最多处理的宝石数
 */
public record XpPumpSettings(XpPumpMode mode, int storeLevel, int keepLevel, int gemsPerCycle) {
    /** 一颗经验宝石 = 50 点经验值（本体文档：1000mB 经验流体 = 1 经验宝石 = 50 玩家经验值）。 */
    public static final int XP_PER_GEM = 50;

    public static final int[] BATCHES = {1, 2, 4, 8};

    public static final XpPumpSettings DEFAULT = new XpPumpSettings(XpPumpMode.OFF, 30, 5, 1);

    public static final Codec<XpPumpSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        XpPumpMode.CODEC.optionalFieldOf("mode", XpPumpMode.OFF).forGetter(XpPumpSettings::mode),
        Codec.INT.optionalFieldOf("store_level", 30).forGetter(XpPumpSettings::storeLevel),
        Codec.INT.optionalFieldOf("keep_level", 5).forGetter(XpPumpSettings::keepLevel),
        Codec.INT.optionalFieldOf("gems_per_cycle", 1).forGetter(XpPumpSettings::gemsPerCycle)
    ).apply(instance, XpPumpSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, XpPumpSettings> STREAM_CODEC = StreamCodec.composite(
        XpPumpMode.STREAM_CODEC,
        XpPumpSettings::mode,
        ByteBufCodecs.VAR_INT,
        XpPumpSettings::storeLevel,
        ByteBufCodecs.VAR_INT,
        XpPumpSettings::keepLevel,
        ByteBufCodecs.VAR_INT,
        XpPumpSettings::gemsPerCycle,
        XpPumpSettings::new
    );

    /** 每周期宝石数档位：1 / 2 / 4 / 8 循环。 */
    public static int nextBatch(int current) {
        for (int batch : XpPumpSettings.BATCHES) {
            if (batch > current) {
                return batch;
            }
        }
        return XpPumpSettings.BATCHES[0];
    }

    public XpPumpSettings nextMode() {
        XpPumpMode[] values = XpPumpMode.values();
        return new XpPumpSettings(
            values[(this.mode.ordinal() + 1) % values.length],
            this.storeLevel,
            this.keepLevel,
            this.gemsPerCycle
        );
    }

    /** 存入阈值越高越保守；同时保证它始终高于「取出保留等级」，两档不会打架。 */
    public XpPumpSettings withStoreLevel(int value) {
        int clamped = Math.clamp(value, 1, 218);
        return new XpPumpSettings(this.mode, clamped, Math.min(this.keepLevel, clamped - 1), this.gemsPerCycle);
    }

    public XpPumpSettings withKeepLevel(int value) {
        return new XpPumpSettings(this.mode, this.storeLevel, Math.clamp(value, 0, this.storeLevel - 1), this.gemsPerCycle);
    }

    public XpPumpSettings withGemsPerCycle(int value) {
        return new XpPumpSettings(this.mode, this.storeLevel, this.keepLevel, Math.clamp(value, 1, 8));
    }

    /** OFF = 停用；STORE = 多余经验换成宝石存进存储；WITHDRAW = 存储里的宝石换回经验。 */
    public enum XpPumpMode implements StringRepresentable {
        OFF,
        STORE,
        WITHDRAW,
        ;

        public static final Codec<XpPumpMode> CODEC = StringRepresentable.fromEnum(XpPumpMode::values);
        public static final StreamCodec<ByteBuf, XpPumpMode> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}