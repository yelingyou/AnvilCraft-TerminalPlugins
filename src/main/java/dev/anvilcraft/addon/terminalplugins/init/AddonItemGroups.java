/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.init;

import dev.anvilcraft.addon.terminalplugins.AnvilCraftTerminalPlugins;
import dev.dubhe.anvilcraft.init.item.ModItemGroups;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static dev.anvilcraft.addon.terminalplugins.AnvilCraftTerminalPlugins.REGISTRUM;

public class AddonItemGroups {
    private static final DeferredRegister<CreativeModeTab> DEFERRED_REGISTER = DeferredRegister.create(
        Registries.CREATIVE_MODE_TAB,
        AnvilCraftTerminalPlugins.MOD_ID
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TERMINAL_PLUGINS = DEFERRED_REGISTER.register(
        "terminal_plugins",
        () -> CreativeModeTab.builder()
            .icon(AddonItems.FILTER_PLUGIN::asStack)
            // 物品由 Registrum 依据 REGISTRUM.defaultCreativeTab(...) 自动加入本页签；
            // 这里若再手动 accept 会触发 "already exists in the tab's list"。
            .displayItems((context, entries) -> {
            })
            .title(REGISTRUM.addLang(
                "itemGroup",
                AnvilCraftTerminalPlugins.of("terminal_plugins"),
                "AnvilCraft: Terminal Plugins"
            ))
            .withTabsBefore(ModItemGroups.ANVILCRAFT_BUILD_BLOCK.getId())
            .build()
    );

    public static void register(IEventBus modEventBus) {
        AddonItemGroups.DEFERRED_REGISTER.register(modEventBus);
    }
}
