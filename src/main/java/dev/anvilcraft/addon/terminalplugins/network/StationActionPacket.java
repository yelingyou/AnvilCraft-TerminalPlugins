/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for full license text.
 */
package dev.anvilcraft.addon.terminalplugins.network;

import dev.anvilcraft.addon.terminalplugins.AnvilCraftTerminalPlugins;
import dev.anvilcraft.addon.terminalplugins.block.entity.PluginStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

/**
 * 客户端 → 服务端：安装台上的显式操作。
 *
 * <p>安装台不再「放进去就自动装」——那样插件会瞬间消失，玩家根本看不出发生了什么。
 * 现在暂存槽只是暂存，安装 / 取下都由界面上的按钮明确触发。</p>
 */
public record StationActionPacket(BlockPos pos, int action) implements CustomPacketPayload {
    /** 把暂存槽里的插件全部装到终端上。 */
    public static final int INSTALL_ALL = 0;
    /** 把终端上已安装的插件全部拆回暂存槽。 */
    public static final int UNINSTALL_ALL = 1;

    public static final Type<StationActionPacket> TYPE = new Type<>(
        AnvilCraftTerminalPlugins.of("station_action")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, StationActionPacket> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            StationActionPacket::pos,
            ByteBufCodecs.VAR_INT,
            StationActionPacket::action,
            StationActionPacket::new
        );

    public static final IPayloadHandler<StationActionPacket> HANDLER = StationActionPacket::handle;

    public static void handle(StationActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.flow().isServerbound()) {
                return;
            }
            if (!(context.player().level().getBlockEntity(packet.pos()) instanceof PluginStationBlockEntity station)) {
                return;
            }
            switch (packet.action()) {
                case StationActionPacket.INSTALL_ALL -> station.applyPlugins();
                case StationActionPacket.UNINSTALL_ALL -> station.uninstallAll();
                default -> {
                }
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return StationActionPacket.TYPE;
    }
}