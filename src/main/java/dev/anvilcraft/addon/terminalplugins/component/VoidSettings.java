package dev.anvilcraft.addon.terminalplugins.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

// 销毁插件配置：存储中匹配过滤表的物品只保留 keepStacks 组，多余部分销毁。
public record VoidSettings(int keepStacks) {
    public static final VoidSettings DEFAULT = new VoidSettings(0);

    public static final Codec<VoidSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf("keep_stacks", 0).forGetter(VoidSettings::keepStacks)
    ).apply(instance, VoidSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, VoidSettings> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        VoidSettings::keepStacks,
        VoidSettings::new
    );

    public VoidSettings withKeepStacks(int value) {
        return new VoidSettings(Math.clamp(value, 0, 64));
    }
}