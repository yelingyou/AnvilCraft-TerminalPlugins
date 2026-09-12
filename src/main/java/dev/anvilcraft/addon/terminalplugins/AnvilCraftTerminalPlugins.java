/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins;

import com.mojang.logging.LogUtils;
import dev.anvilcraft.addon.terminalplugins.data.AddonDatagen;
import dev.anvilcraft.addon.terminalplugins.init.AddonBlockEntities;
import dev.anvilcraft.addon.terminalplugins.init.AddonBlocks;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.init.AddonItemGroups;
import dev.anvilcraft.addon.terminalplugins.init.AddonItems;
import dev.anvilcraft.addon.terminalplugins.init.AddonMenus;
import dev.anvilcraft.addon.terminalplugins.init.AddonNetworks;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPlugins;
import dev.anvilcraft.lib.v2.config.ConfigManager;
import dev.anvilcraft.lib.v2.registrum.Registrum;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(AnvilCraftTerminalPlugins.MOD_ID)
public class AnvilCraftTerminalPlugins {
    public static final String MOD_ID = "anvilcraft_terminal_plugins";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final TerminalPluginsConfig CONFIG = ConfigManager.register(
        AnvilCraftTerminalPlugins.MOD_ID,
        TerminalPluginsConfig::new
    );
    public static final Registrum REGISTRUM = Registrum.create(MOD_ID);

    /** 条件编解码器注册表：给配方用的 {@code recipes_enabled} 条件。 */
    public static final net.neoforged.neoforge.registries.DeferredRegister<com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.conditions.ICondition>> CONDITIONS =
        net.neoforged.neoforge.registries.DeferredRegister.create(
            net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.CONDITION_CODECS,
            MOD_ID
        );

    static {
        CONDITIONS.register(
            "recipes_enabled",
            () -> dev.anvilcraft.addon.terminalplugins.data.ConfigCondition.CODEC
        );
    }

    public AnvilCraftTerminalPlugins(IEventBus modEventBus, ModContainer modContainer) {
        CONDITIONS.register(modEventBus);
        AddonDataComponents.register(modEventBus);
        AddonItemGroups.register(modEventBus);
        AddonBlocks.register();
        AddonItems.register();
        AddonBlockEntities.register();
        AddonMenus.register();
        TerminalPlugins.register();
        AddonDatagen.init();
        modEventBus.addListener(this::registerPayloads);
    }

    private void registerPayloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        AddonNetworks.init(event.registrar("1"));
    }

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
