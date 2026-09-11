/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.data;

import dev.anvilcraft.addon.terminalplugins.AnvilCraftTerminalPlugins;
import dev.anvilcraft.addon.terminalplugins.data.lang.AddonLangHandler;
import dev.anvilcraft.lib.v2.registrum.providers.ProviderType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import static dev.anvilcraft.addon.terminalplugins.AnvilCraftTerminalPlugins.REGISTRUM;

@EventBusSubscriber(modid = AnvilCraftTerminalPlugins.MOD_ID)
public class AddonDatagen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
    }

    /**
     * 初始化生成器：只生成配置项的语言键，其余语言键手写在 src/main/resources 中。
     */
    public static void init() {
        REGISTRUM.addDataGenerator(ProviderType.LANG, AddonLangHandler::init);
    }
}
