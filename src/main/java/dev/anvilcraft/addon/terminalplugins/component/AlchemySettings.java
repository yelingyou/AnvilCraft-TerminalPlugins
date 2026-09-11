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
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;

/**
 * 炼金插件配置：语义对齐精妙背包的「炼金升级」——按条件自动使用药水。
 *
 * @param entries        条目列表，每条 = 过滤用药水 + 触发条件 + 阈值
 * @param nearby         除自己以外，还自动照顾哪些附近实体
 * @param radius         附近实体检查半径
 * @param interval       检查间隔（tick）
 * @param matchAmplifier 匹配药水时是否要求效果等级一致
 */
public record AlchemySettings(
    List<AlchemyEntry> entries,
    NearbyTarget nearby,
    int radius,
    int interval,
    boolean matchAmplifier
) {
    public static final int ENTRY_COUNT = 4;
    public static final AlchemySettings DEFAULT = new AlchemySettings(
        AlchemySettings.emptyEntries(),
        NearbyTarget.NONE,
        3,
        5,
        false
    );

    public static final Codec<AlchemySettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        AlchemyEntry.CODEC.listOf().optionalFieldOf("entries", AlchemySettings.emptyEntries())
            .forGetter(AlchemySettings::entries),
        NearbyTarget.CODEC.optionalFieldOf("nearby", NearbyTarget.NONE).forGetter(AlchemySettings::nearby),
        Codec.INT.optionalFieldOf("radius", 3).forGetter(AlchemySettings::radius),
        Codec.INT.optionalFieldOf("interval", 5).forGetter(AlchemySettings::interval),
        Codec.BOOL.optionalFieldOf("match_amplifier", false).forGetter(AlchemySettings::matchAmplifier)
    ).apply(instance, AlchemySettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemySettings> STREAM_CODEC = StreamCodec.composite(
        AlchemyEntry.STREAM_CODEC.apply(ByteBufCodecs.list(AlchemySettings.ENTRY_COUNT)),
        AlchemySettings::entries,
        NearbyTarget.STREAM_CODEC,
        AlchemySettings::nearby,
        ByteBufCodecs.VAR_INT,
        AlchemySettings::radius,
        ByteBufCodecs.VAR_INT,
        AlchemySettings::interval,
        ByteBufCodecs.BOOL,
        AlchemySettings::matchAmplifier,
        AlchemySettings::new
    );

    private static List<AlchemyEntry> emptyEntries() {
        List<AlchemyEntry> list = new ArrayList<>();
        for (int index = 0; index < AlchemySettings.ENTRY_COUNT; index++) {
            list.add(AlchemyEntry.EMPTY);
        }
        return list;
    }

    /** 保证条目数量固定，便于用索引定位。 */
    public List<AlchemyEntry> normalizedEntries() {
        List<AlchemyEntry> list = new ArrayList<>(this.entries);
        while (list.size() < AlchemySettings.ENTRY_COUNT) {
            list.add(AlchemyEntry.EMPTY);
        }
        return List.copyOf(list.subList(0, AlchemySettings.ENTRY_COUNT));
    }

    public AlchemySettings withEntry(int index, AlchemyEntry entry) {
        List<AlchemyEntry> list = new ArrayList<>(this.normalizedEntries());
        if (index < 0 || index >= list.size()) {
            return this;
        }
        list.set(index, entry);
        return new AlchemySettings(list, this.nearby, this.radius, this.interval, this.matchAmplifier);
    }

    public AlchemySettings withNearby(NearbyTarget target) {
        return new AlchemySettings(this.normalizedEntries(), target, this.radius, this.interval, this.matchAmplifier);
    }

    /** 除自己以外还要照顾的实体范围。 */
    public enum NearbyTarget implements StringRepresentable {
        NONE,
        PLAYERS,
        MOBS,
        ALL,
        ;

        public static final Codec<NearbyTarget> CODEC = StringRepresentable.fromEnum(NearbyTarget::values);
        public static final StreamCodec<ByteBuf, NearbyTarget> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public boolean matches(LivingEntity entity) {
            return switch (this) {
                case NONE -> false;
                case PLAYERS -> entity instanceof Player;
                case MOBS -> !(entity instanceof Player);
                case ALL -> true;
            };
        }

        public NearbyTarget next() {
            NearbyTarget[] values = NearbyTarget.values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }

    /** 触发条件，取自精妙背包炼金升级的同一组语义。 */
    public enum AlchemyTrigger implements StringRepresentable {
        NEVER((entity, value) -> false, -1.0F),
        ALWAYS((entity, value) -> true, -1.0F),
        UNDER_WATER((entity, value) -> entity.isUnderWater(), -1.0F),
        ON_FIRE((entity, value) -> entity.isOnFire(), -1.0F),
        FALLING((entity, value) -> entity.fallDistance > 2.0F, -1.0F),
        SPRINTING((entity, value) -> entity.isSprinting(), -1.0F),
        HURT((entity, value) -> entity.getHealth() > 0.0F
                               && entity.getHealth() < entity.getMaxHealth()
                               && entity.getHealth() / entity.getMaxHealth() < value, 0.75F),
        NEGATIVE_EFFECT((entity, value) -> entity.getActiveEffects().stream()
            .anyMatch(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL), -1.0F),
        ;

        public static final Codec<AlchemyTrigger> CODEC = StringRepresentable.fromEnum(AlchemyTrigger::values);
        public static final StreamCodec<ByteBuf, AlchemyTrigger> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

        private final BiPredicate<LivingEntity, Float> predicate;
        private final float defaultValue;

        AlchemyTrigger(BiPredicate<LivingEntity, Float> predicate, float defaultValue) {
            this.predicate = predicate;
            this.defaultValue = defaultValue;
        }

        public boolean test(LivingEntity entity, float value) {
            return this.predicate.test(entity, value);
        }

        public float defaultValue() {
            return this.defaultValue;
        }

        /** 只有受伤阈值条件用到数值。 */
        public boolean usesValue() {
            return this == HURT;
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public AlchemyTrigger next() {
            AlchemyTrigger[] values = AlchemyTrigger.values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }

    /** 单个条目：过滤药水 + 触发条件 + 阈值。 */
    public record AlchemyEntry(ItemStack filter, AlchemyTrigger trigger, float value) {
        public static final AlchemyEntry EMPTY = new AlchemyEntry(ItemStack.EMPTY, AlchemyTrigger.NEVER, -1.0F);

        public static final Codec<AlchemyEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("filter", ItemStack.EMPTY).forGetter(AlchemyEntry::filter),
            AlchemyTrigger.CODEC.optionalFieldOf("trigger", AlchemyTrigger.NEVER).forGetter(AlchemyEntry::trigger),
            Codec.FLOAT.optionalFieldOf("value", -1.0F).forGetter(AlchemyEntry::value)
        ).apply(instance, AlchemyEntry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, AlchemyEntry> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC,
            AlchemyEntry::filter,
            AlchemyTrigger.STREAM_CODEC,
            AlchemyEntry::trigger,
            ByteBufCodecs.FLOAT,
            AlchemyEntry::value,
            AlchemyEntry::new
        );

        public boolean isEmpty() {
            return this.filter.isEmpty() || this.trigger == AlchemyTrigger.NEVER;
        }

        public AlchemyEntry withFilter(ItemStack newFilter) {
            AlchemyTrigger newTrigger = this.trigger;
            float newValue = this.value;
            if (newFilter.isEmpty()) {
                newTrigger = AlchemyTrigger.NEVER;
                newValue = -1.0F;
            } else if (this.trigger == AlchemyTrigger.NEVER) {
                newTrigger = AlchemyTrigger.HURT;
                newValue = AlchemyTrigger.HURT.defaultValue();
            }
            return new AlchemyEntry(newFilter, newTrigger, newValue);
        }

        public AlchemyEntry withTrigger(AlchemyTrigger newTrigger) {
            return new AlchemyEntry(this.filter, newTrigger, newTrigger.defaultValue());
        }

        public AlchemyEntry withValue(float newValue) {
            return new AlchemyEntry(this.filter, this.trigger, Math.clamp(newValue, 0.0F, 1.0F));
        }
    }
}
