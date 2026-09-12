package dev.anvilcraft.addon.terminalplugins.network;

import dev.anvilcraft.addon.terminalplugins.AnvilCraftTerminalPlugins;
import dev.anvilcraft.addon.terminalplugins.client.TerminalPluginClientEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

// 服务端 -> 客户端：插件操作失败时的提示（对齐精妙背包的错误反馈包）。
public record PluginFeedbackPacket(String translationKey) implements CustomPacketPayload {
    public static final Type<PluginFeedbackPacket> TYPE = new Type<>(
        AnvilCraftTerminalPlugins.of("plugin_feedback")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PluginFeedbackPacket> STREAM_CODEC = StreamCodec.ofMember(
        PluginFeedbackPacket::encode,
        PluginFeedbackPacket::decode
    );

    public static final IPayloadHandler<PluginFeedbackPacket> HANDLER = PluginFeedbackPacket::handle;

    public static PluginFeedbackPacket decode(RegistryFriendlyByteBuf buf) {
        return new PluginFeedbackPacket(buf.readUtf(128));
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(this.translationKey, 128);
    }

    public static void handle(PluginFeedbackPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isServerbound()) {
                return;
            }
            TerminalPluginClientEvents.showFeedback(packet.translationKey());
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PluginFeedbackPacket.TYPE;
    }
}