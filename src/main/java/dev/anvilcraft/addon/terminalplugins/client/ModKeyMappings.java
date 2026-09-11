/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

// 客户端按键：在终端界面里开关插件面板。
public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories.anvilcraft_terminal_plugins";

    public static final KeyMapping OPEN_PLUGIN_PANEL = new KeyMapping(
        "key.anvilcraft_terminal_plugins.open_panel",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_K,
        ModKeyMappings.CATEGORY
    );

    private ModKeyMappings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.OPEN_PLUGIN_PANEL);
    }
}
