/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.network;

import dev.anvilcraft.addon.terminalplugins.AnvilCraftTerminalPlugins;
import dev.anvilcraft.addon.terminalplugins.block.entity.PluginStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

/**
 * 客户端 → 服务端：请求把终端上已安装的插件全部拆回安装台的暂存槽。
 */
public record UninstallPluginsPacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<UninstallPluginsPacket> TYPE = new Type<>(
        AnvilCraftTerminalPlugins.of("uninstall_plugins")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, UninstallPluginsPacket> STREAM_CODEC = StreamCodec.ofMember(
        UninstallPluginsPacket::encode,
        UninstallPluginsPacket::decode
    );

    public static final IPayloadHandler<UninstallPluginsPacket> HANDLER = UninstallPluginsPacket::handle;

    public static UninstallPluginsPacket decode(RegistryFriendlyByteBuf buf) {
        return new UninstallPluginsPacket(buf.readBlockPos());
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    public static void handle(UninstallPluginsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.flow().isServerbound()) {
                return;
            }
            if (!(context.player().level().getBlockEntity(packet.pos()) instanceof PluginStationBlockEntity station)) {
                return;
            }
            station.uninstallAll();
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return UninstallPluginsPacket.TYPE;
    }
}
