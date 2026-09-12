package dev.anvilcraft.addon.terminalplugins.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

// 一键存入插件配置。过滤表沿用物品上的 filter_content 组件。
public record DepositSettings(boolean skipHotbar, boolean skipArmor) {
    public static final DepositSettings DEFAULT = new DepositSettings(false, true);

    public static final Codec<DepositSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("skip_hotbar", false).forGetter(DepositSettings::skipHotbar),
        Codec.BOOL.optionalFieldOf("skip_armor", true).forGetter(DepositSettings::skipArmor)
    ).apply(instance, DepositSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DepositSettings> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        DepositSettings::skipHotbar,
        ByteBufCodecs.BOOL,
        DepositSettings::skipArmor,
        DepositSettings::new
    );

    public DepositSettings withSkipHotbar(boolean value) {
        return new DepositSettings(value, this.skipArmor);
    }

    public DepositSettings withSkipArmor(boolean value) {
        return new DepositSettings(this.skipHotbar, value);
    }
}