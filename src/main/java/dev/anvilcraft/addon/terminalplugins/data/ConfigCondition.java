/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for license text.
 */
package dev.anvilcraft.addon.terminalplugins.data;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.addon.terminalplugins.AnvilCraftTerminalPlugins;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * 配方条件：{@code anvilcraft_terminal_plugins:recipes_enabled}。
 *
 * <p>对应配置项 {@code enableAnvilCraftRecipes}：关掉它，本模组那些「铁砧工艺化」的配方
 * （冲压 / 充能 / 时移）在数据包加载时就不会被加载，只保留普通工作台配方。</p>
 *
 * <p>NeoForge 1.21.1 的条件是**编解码器注册表**（{@code NeoForgeRegistries.Keys.CONDITION_CODECS}），
 * 不像老版本那样需要写 serializer。</p>
 */
public record ConfigCondition() implements ICondition {
    public static final ConfigCondition INSTANCE = new ConfigCondition();

    public static final MapCodec<ConfigCondition> CODEC = MapCodec.unit(ConfigCondition.INSTANCE);

    @Override
    public boolean test(IContext context) {
        return AnvilCraftTerminalPlugins.CONFIG.enableAnvilCraftRecipes;
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return ConfigCondition.CODEC;
    }
}