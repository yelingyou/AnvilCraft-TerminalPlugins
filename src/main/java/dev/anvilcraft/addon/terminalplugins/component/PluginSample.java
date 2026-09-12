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
 * 会执行配方的插件（铁砧加工 / 锻造 / 压缩 / 充能）的输入输出槽内容。
 *
 * <p>{@code sample} 是**输入槽**：在面板里左键放入一个物品，插件就只加工这一种；
 * 留空表示按过滤表（或全部）自己挑。
 * {@code lastOutput} 是**输出槽**：服务端每次成功加工后写进来，玩家能直接看到上次产出了什么。</p>
 *
 * <p>整个记录都存在插件物品的数据组件里，所以拆下插件时输入输出槽的内容会跟着走。</p>
 */
public record PluginSample(ItemStack sample, ItemStack lastOutput) {
    public static final PluginSample EMPTY = new PluginSample(ItemStack.EMPTY, ItemStack.EMPTY);

    public static final Codec<PluginSample> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ItemStack.OPTIONAL_CODEC.optionalFieldOf("sample", ItemStack.EMPTY).forGetter(PluginSample::sample),
        ItemStack.OPTIONAL_CODEC.optionalFieldOf("last_output", ItemStack.EMPTY).forGetter(PluginSample::lastOutput)
    ).apply(instance, PluginSample::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PluginSample> STREAM_CODEC = StreamCodec.composite(
        ItemStack.OPTIONAL_STREAM_CODEC,
        PluginSample::sample,
        ItemStack.OPTIONAL_STREAM_CODEC,
        PluginSample::lastOutput,
        PluginSample::new
    );

    public boolean hasSample() {
        return !this.sample.isEmpty();
    }

    public PluginSample withSample(ItemStack stack) {
        return new PluginSample(stack, this.lastOutput);
    }

    public PluginSample withOutput(ItemStack stack) {
        return new PluginSample(this.sample, stack);
    }
}