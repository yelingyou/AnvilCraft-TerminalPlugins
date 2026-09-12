package dev.anvilcraft.addon.terminalplugins.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

// 流体接口插件配置：缓冲容量（桶）与每周期处理批数，工作模式决定是抽进缓冲还是灌进容器。
public record FluidSettings(FluidMode mode, int batch, int capacityBuckets) {
    public static final FluidSettings DEFAULT = new FluidSettings(FluidMode.OFF, 4, 8);

    public static final Codec<FluidSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        FluidMode.CODEC.optionalFieldOf("mode", FluidMode.OFF).forGetter(FluidSettings::mode),
        Codec.INT.optionalFieldOf("batch", 4).forGetter(FluidSettings::batch),
        Codec.INT.optionalFieldOf("capacity_buckets", 8).forGetter(FluidSettings::capacityBuckets)
    ).apply(instance, FluidSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidSettings> STREAM_CODEC = StreamCodec.composite(
        FluidMode.STREAM_CODEC,
        FluidSettings::mode,
        ByteBufCodecs.VAR_INT,
        FluidSettings::batch,
        ByteBufCodecs.VAR_INT,
        FluidSettings::capacityBuckets,
        FluidSettings::new
    );

    public FluidSettings nextMode() {
        FluidMode[] values = FluidMode.values();
        return new FluidSettings(values[(this.mode.ordinal() + 1) % values.length], this.batch, this.capacityBuckets);
    }

    public FluidSettings withBatch(int value) {
        return new FluidSettings(this.mode, Math.clamp(value, 1, 16), this.capacityBuckets);
    }

    public FluidSettings withCapacityBuckets(int value) {
        return new FluidSettings(this.mode, this.batch, Math.clamp(value, 1, 64));
    }

    /** 批数档位：1 / 2 / 4 / 8 / 16，循环切换。 */
    public static int nextBatch(int current) {
        int[] steps = {1, 2, 4, 8, 16};
        for (int step : steps) {
            if (step > current) {
                return step;
            }
        }
        return steps[0];
    }

    public int capacityMb() {
        return this.capacityBuckets * 1000;
    }

    // OFF = 停用；FILL_BUFFER = 把存储里容器中的流体抽进缓冲；EMPTY_BUFFER = 把缓冲里的流体灌进空容器。
    public enum FluidMode implements StringRepresentable {
        OFF,
        FILL_BUFFER,
        EMPTY_BUFFER,
        ;

        public static final Codec<FluidMode> CODEC = StringRepresentable.fromEnum(FluidMode::values);
        public static final StreamCodec<ByteBuf, FluidMode> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}