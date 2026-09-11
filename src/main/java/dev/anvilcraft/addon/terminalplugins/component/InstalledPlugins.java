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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 终端物品上已安装的插件列表。
 *
 * <p>每个元素都是一枚插件物品堆栈，插件自身的配置（过滤表、半径、模式等）保存在该堆栈的组件里，
 * 因此拆卸插件时配置会随物品一起被取回。</p>
 */
public record InstalledPlugins(List<ItemStack> plugins) {
    public static final int MAX_SLOTS = 9;
    public static final InstalledPlugins EMPTY = new InstalledPlugins(List.of());
    public static final Codec<InstalledPlugins> CODEC = ItemStack.OPTIONAL_CODEC
        .listOf()
        .xmap(InstalledPlugins::new, InstalledPlugins::plugins);
    public static final StreamCodec<RegistryFriendlyByteBuf, InstalledPlugins> STREAM_CODEC = ItemStack.OPTIONAL_STREAM_CODEC
        .apply(ByteBufCodecs.list(InstalledPlugins.MAX_SLOTS))
        .map(InstalledPlugins::new, InstalledPlugins::plugins);

    public InstalledPlugins(List<ItemStack> plugins) {
        this.plugins = List.copyOf(plugins);
    }

    public boolean isEmpty() {
        return this.plugins.isEmpty();
    }

    public int size() {
        return this.plugins.size();
    }

    public InstalledPlugins with(ItemStack plugin) {
        List<ItemStack> list = new ArrayList<>(this.plugins);
        list.add(plugin.copyWithCount(1));
        return new InstalledPlugins(list);
    }

    public InstalledPlugins without(int index) {
        if (index < 0 || index >= this.plugins.size()) {
            return this;
        }
        List<ItemStack> list = new ArrayList<>(this.plugins);
        list.remove(index);
        return new InstalledPlugins(list);
    }

    public InstalledPlugins replaced(int index, ItemStack plugin) {
        if (index < 0 || index >= this.plugins.size()) {
            return this;
        }
        List<ItemStack> list = new ArrayList<>(this.plugins);
        list.set(index, plugin.copyWithCount(1));
        return new InstalledPlugins(list);
    }

    public boolean has(dev.anvilcraft.addon.terminalplugins.plugin.PluginKind kind) {
        return this.plugins.stream().anyMatch(stack -> stack.getItem() instanceof dev.anvilcraft.addon.terminalplugins.item.TerminalPluginItem plugin
            && plugin.kind() == kind);
    }
}
