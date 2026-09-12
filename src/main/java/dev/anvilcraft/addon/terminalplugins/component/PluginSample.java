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
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * 会执行配方的插件的输入槽 / 输出槽内容。
 *
 * <p>{@code sample} 是主输入槽（锻造里是**工具/基底**，铁砧加工/压缩/充能里是要加工的物品）；
 * {@code addition} 与 {@code template} 是锻造专用的**金属（附加物）**与**模板**槽
 * —— 对应皇家锻造台的三个格子，模板只当钥匙、不会被消耗。</p>
 *
 * <p>{@code lastOutput} 是输出槽：服务端每次成功加工后写进来。</p>
 *
 * <p>整个记录都存在插件物品的数据组件里，拆下插件时槽位内容会跟着走。</p>
 */
public record PluginSample(ItemStack sample, ItemStack addition, ItemStack template, ItemStack lastOutput) {
    public static final PluginSample EMPTY = new PluginSample(
        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
    );

    public static final Codec<PluginSample> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ItemStack.OPTIONAL_CODEC.optionalFieldOf("sample", ItemStack.EMPTY).forGetter(PluginSample::sample),
        ItemStack.OPTIONAL_CODEC.optionalFieldOf("addition", ItemStack.EMPTY).forGetter(PluginSample::addition),
        ItemStack.OPTIONAL_CODEC.optionalFieldOf("template", ItemStack.EMPTY).forGetter(PluginSample::template),
        ItemStack.OPTIONAL_CODEC.optionalFieldOf("last_output", ItemStack.EMPTY).forGetter(PluginSample::lastOutput)
    ).apply(instance, PluginSample::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PluginSample> STREAM_CODEC = StreamCodec.composite(
        ItemStack.OPTIONAL_STREAM_CODEC,
        PluginSample::sample,
        ItemStack.OPTIONAL_STREAM_CODEC,
        PluginSample::addition,
        ItemStack.OPTIONAL_STREAM_CODEC,
        PluginSample::template,
        ItemStack.OPTIONAL_STREAM_CODEC,
        PluginSample::lastOutput,
        PluginSample::new
    );

    public boolean hasSample() {
        return !this.sample.isEmpty();
    }

    public boolean hasAddition() {
        return !this.addition.isEmpty();
    }

    public boolean hasTemplate() {
        return !this.template.isEmpty();
    }

    public PluginSample withSample(ItemStack stack) {
        return new PluginSample(stack, this.addition, this.template, this.lastOutput);
    }

    public PluginSample withAddition(ItemStack stack) {
        return new PluginSample(this.sample, stack, this.template, this.lastOutput);
    }

    public PluginSample withTemplate(ItemStack stack) {
        return new PluginSample(this.sample, this.addition, stack, this.lastOutput);
    }

    public PluginSample withOutput(ItemStack stack) {
        return new PluginSample(this.sample, this.addition, this.template, stack);
    }
}