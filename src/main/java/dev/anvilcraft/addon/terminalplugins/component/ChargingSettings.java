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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 充能插件配置。
 *
 * <p>两件事可以分别开关：
 * <ul>
 *   <li>{@code runRecipes}：执行铁砧工艺的**充能配方**（`anvilcraft:charger_charging`），原料从存储取、产物回存储；</li>
 *   <li>{@code chargeItems}：把电网的电**按本体配置换算成 FE**，给玩家身上的 FE 物品充电。</li>
 * </ul>
</p>
 *
 * <p>{@code activeRecipe} / {@code progress} 是充能配方的进度：写进插件物品本身的数据组件，
 * 所以把插件拆下来、换到别的终端上，进度都还在。</p>
 *
 * @param runRecipes   是否执行充能配方
 * @param chargeItems  是否给 FE 物品充电
 * @param powerKw      向电网申请的功率（kW）；配方要求功率高于它就跑不动
 * @param activeRecipe 正在充能的配方 id（空串 = 没有进行中的配方）
 * @param progress     已进行的 tick 数
 */
public record ChargingSettings(
    boolean runRecipes,
    boolean chargeItems,
    int powerKw,
    String activeRecipe,
    int progress
) {
    /** 可申请的电网上限档位（kW）。 */
    public static final int[] POWERS = {4, 16, 64, 256};

    public static final ChargingSettings DEFAULT = new ChargingSettings(true, true, 16, "", 0);

    public static final Codec<ChargingSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("run_recipes", true).forGetter(ChargingSettings::runRecipes),
        Codec.BOOL.optionalFieldOf("charge_items", true).forGetter(ChargingSettings::chargeItems),
        Codec.INT.optionalFieldOf("power_kw", 16).forGetter(ChargingSettings::powerKw),
        Codec.STRING.optionalFieldOf("active_recipe", "").forGetter(ChargingSettings::activeRecipe),
        Codec.INT.optionalFieldOf("progress", 0).forGetter(ChargingSettings::progress)
    ).apply(instance, ChargingSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChargingSettings> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        ChargingSettings::runRecipes,
        ByteBufCodecs.BOOL,
        ChargingSettings::chargeItems,
        ByteBufCodecs.VAR_INT,
        ChargingSettings::powerKw,
        ByteBufCodecs.STRING_UTF8,
        ChargingSettings::activeRecipe,
        ByteBufCodecs.VAR_INT,
        ChargingSettings::progress,
        ChargingSettings::new
    );

    /** 主档位：申请功率 4 → 16 → 64 → 256 kW 循环。 */
    public ChargingSettings nextPower() {
        for (int power : ChargingSettings.POWERS) {
            if (power > this.powerKw) {
                return new ChargingSettings(
                    this.runRecipes, this.chargeItems, power, this.activeRecipe, this.progress);
            }
        }
        return new ChargingSettings(
            this.runRecipes, this.chargeItems, ChargingSettings.POWERS[0], this.activeRecipe, this.progress);
    }

    public ChargingSettings withRunRecipes(boolean value) {
        return new ChargingSettings(value, this.chargeItems, this.powerKw, this.activeRecipe, this.progress);
    }

    public ChargingSettings withChargeItems(boolean value) {
        return new ChargingSettings(this.runRecipes, value, this.powerKw, this.activeRecipe, this.progress);
    }

    public ChargingSettings withProgress(String recipe, int ticks) {
        return new ChargingSettings(this.runRecipes, this.chargeItems, this.powerKw, recipe, ticks);
    }

    public ChargingSettings cleared() {
        return this.withProgress("", 0);
    }

    public boolean idle() {
        return !this.runRecipes && !this.chargeItems;
    }
}