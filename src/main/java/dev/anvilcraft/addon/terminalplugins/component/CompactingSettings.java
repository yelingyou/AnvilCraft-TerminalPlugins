package dev.anvilcraft.addon.terminalplugins.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

// 压缩插件配置：每周期最多把 batch 组「9 个同类物品」压成 1 个（用原版 3x3 合成配方）。
public record CompactingSettings(int batch) {
    public static final CompactingSettings DEFAULT = new CompactingSettings(4);

    public static final Codec<CompactingSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf("batch", 4).forGetter(CompactingSettings::batch)
    ).apply(instance, CompactingSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompactingSettings> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        CompactingSettings::batch,
        CompactingSettings::new
    );

    public CompactingSettings withBatch(int value) {
        return new CompactingSettings(Math.clamp(value, 1, 16));
    }
}