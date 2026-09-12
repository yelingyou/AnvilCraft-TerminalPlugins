package dev.anvilcraft.addon.terminalplugins.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

// 铁砧修复插件配置：每次消耗 1 个材料修复 repairPerMaterial 点耐久（0 表示按最大耐久的 1/4 计算）。
public record AnvilRepairSettings(int repairPerMaterial) {
    public static final AnvilRepairSettings DEFAULT = new AnvilRepairSettings(0);

    public static final Codec<AnvilRepairSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf("repair_per_material", 0).forGetter(AnvilRepairSettings::repairPerMaterial)
    ).apply(instance, AnvilRepairSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AnvilRepairSettings> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        AnvilRepairSettings::repairPerMaterial,
        AnvilRepairSettings::new
    );

    public AnvilRepairSettings withRepairPerMaterial(int value) {
        return new AnvilRepairSettings(Math.clamp(value, 0, 4096));
    }
}