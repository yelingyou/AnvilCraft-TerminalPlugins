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
import dev.anvilcraft.addon.terminalplugins.component.AlchemySettings;
import dev.anvilcraft.addon.terminalplugins.component.ChargingSettings;
import dev.anvilcraft.addon.terminalplugins.component.FeedingSettings;
import dev.anvilcraft.addon.terminalplugins.component.PluginSample;
import dev.anvilcraft.addon.terminalplugins.component.FluidSettings;
import dev.anvilcraft.addon.terminalplugins.component.ToolSwapSettings;
import dev.anvilcraft.addon.terminalplugins.component.XpPumpSettings;
import dev.anvilcraft.addon.terminalplugins.component.MagnetSettings;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginContext;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPlugin;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPluginRegistry;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalStorage;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalStorageResolver;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPluginManager;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import java.util.Optional;

/**
 * 客户端 → 服务端：对某个终端的插件执行一次操作（安装台里的终端或玩家身上的终端）。
 *
 * <p>station 有值时操作安装台台面上的终端；否则按 containerSlot 定位玩家物品栏里的终端
 * （-1 = 主手，-2 = 副手，>=0 = Inventory 索引）。</p>
 */
public record PluginActionPacket(
    Optional<BlockPos> station,
    int containerSlot,
    int action,
    int pluginIndex,
    int entryIndex,
    ItemStack filter,
    float value
) implements CustomPacketPayload {
    public static final int CYCLE_PRIMARY = 0;
    public static final int CYCLE_SECONDARY = 1;
    public static final int REMOVE = 2;
    public static final int MOVE_UP = 3;
    public static final int MOVE_DOWN = 4;
    public static final int TOGGLE_ENABLED = 5;
    public static final int SET_ALCHEMY_FILTER = 6;
    public static final int CYCLE_ALCHEMY_TRIGGER = 7;
    public static final int ADJUST_ALCHEMY_VALUE = 8;
    public static final int CYCLE_ALCHEMY_NEARBY = 9;
    public static final int ADJUST_ALCHEMY_RADIUS = 10;
    public static final int SET_FILTER_SLOT = 11;
    public static final int ADJUST_MAGNET_RANGE = 12;
    public static final int ADJUST_FEEDING_THRESHOLD = 13;
    /** 流体插件：每周期处理批数。 */
    public static final int ADJUST_FLUID_BATCH = 15;
    /** 流体插件：缓冲容量（桶）。 */
    public static final int ADJUST_FLUID_CAPACITY = 16;
    /** 工具切换插件：旧工具是否放回存储。 */
    public static final int TOGGLE_TOOL_SWAP_RETURN = 17;
    /** 经验泵：存入阈值等级。 */
    public static final int ADJUST_XP_STORE_LEVEL = 18;
    /** 经验泵：取出保留等级。 */
    public static final int ADJUST_XP_KEEP_LEVEL = 19;
    /** 经验泵：每周期宝石数。 */
    public static final int ADJUST_XP_BATCH = 20;
    /** 充能插件：是否执行充能配方。 */
    public static final int TOGGLE_CHARGING_RECIPES = 21;
    /** 充能插件：是否给 FE 物品充电。 */
    public static final int TOGGLE_CHARGING_ITEMS = 22;
    /** 即时动作：铁砧加工一次（由插件自己实现 onAction）。 */
    public static final int ANVIL_PROCESS_NOW = 23;
    /** 会执行配方的插件：设置输入槽（空物品 = 清空）。 */
    public static final int SET_SAMPLE_SLOT = 24;
    /** 即时动作：锻造一次（皇家锻造台语义）。 */
    public static final int SMITHING_NOW = 25;

    public static final Type<PluginActionPacket> TYPE = new Type<>(
        AnvilCraftTerminalPlugins.of("plugin_action")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PluginActionPacket> STREAM_CODEC = StreamCodec.ofMember(
        PluginActionPacket::encode,
        PluginActionPacket::decode
    );

    public static final IPayloadHandler<PluginActionPacket> HANDLER = PluginActionPacket::handle;

    /** 针对安装台终端的构造。 */
    public static PluginActionPacket station(BlockPos pos, int action, int pluginIndex, int entryIndex, ItemStack filter, float value) {
        return new PluginActionPacket(Optional.of(pos), -1, action, pluginIndex, entryIndex, filter, value);
    }

    /** 针对玩家身上终端的构造。 */
    public static PluginActionPacket held(int containerSlot, int action, int pluginIndex, int entryIndex, ItemStack filter, float value) {
        return new PluginActionPacket(Optional.empty(), containerSlot, action, pluginIndex, entryIndex, filter, value);
    }

    public static PluginActionPacket decode(RegistryFriendlyByteBuf buf) {
        boolean hasStation = buf.readBoolean();
        Optional<BlockPos> station = hasStation ? Optional.of(buf.readBlockPos()) : Optional.empty();
        int containerSlot = buf.readVarInt();
        int action = buf.readVarInt();
        int pluginIndex = buf.readVarInt();
        int entryIndex = buf.readVarInt();
        ItemStack filter = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        float value = buf.readFloat();
        return new PluginActionPacket(station, containerSlot, action, pluginIndex, entryIndex, filter, value);
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(this.station.isPresent());
        this.station.ifPresent(buf::writeBlockPos);
        buf.writeVarInt(this.containerSlot);
        buf.writeVarInt(this.action);
        buf.writeVarInt(this.pluginIndex);
        buf.writeVarInt(this.entryIndex);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, this.filter);
        buf.writeFloat(this.value);
    }

    public static void handle(PluginActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.flow().isServerbound()) {
                return;
            }
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            PluginStationBlockEntity station = null;
            ItemStack terminal;
            if (packet.station().isPresent()
                && player.serverLevel().getBlockEntity(packet.station().get()) instanceof PluginStationBlockEntity be) {
                station = be;
                terminal = be.getTerminal();
            } else if (packet.station().isEmpty()) {
                terminal = PluginActionPacket.heldTerminal(player, packet.containerSlot());
            } else {
                return;
            }
            if (terminal.isEmpty() || !TerminalPluginManager.isTerminal(terminal)) {
                PluginActionPacket.sendFeedback(player, "message.anvilcraft_terminal_plugins.no_terminal");
                return;
            }
            int pluginCount = TerminalPluginManager.installed(terminal).size();
            if (packet.pluginIndex() < 0 || packet.pluginIndex() >= pluginCount) {
                PluginActionPacket.sendFeedback(player, "message.anvilcraft_terminal_plugins.stale_plugin");
                return;
            }
            ItemStack uninstalled = PluginActionPacket.apply(player, terminal, packet);
            if (!uninstalled.isEmpty()) {
                player.getInventory().placeItemBackInInventory(uninstalled);
            }
            if (station != null) {
                station.afterPluginChange();
            } else {
                player.getInventory().setChanged();
                player.inventoryMenu.broadcastChanges();
                player.containerMenu.broadcastChanges();
            }
        });
    }

    private static void sendFeedback(ServerPlayer player, String key) {
        PacketDistributor.sendToPlayer(player, new PluginFeedbackPacket(key));
    }

    /** 定位玩家身上（物品栏 / 双手）的终端物品堆栈。 */
    private static ItemStack heldTerminal(ServerPlayer player, int containerSlot) {
        return switch (containerSlot) {
            case -1 -> player.getMainHandItem();
            case -2 -> player.getOffhandItem();
            default -> containerSlot >= 0 && containerSlot < player.getInventory().getContainerSize()
                ? player.getInventory().getItem(containerSlot)
                : ItemStack.EMPTY;
        };
    }

    /** 执行操作；若卸下了插件则返回该插件（用于还给玩家）。 */
    private static ItemStack apply(ServerPlayer player, ItemStack terminal, PluginActionPacket packet) {
        switch (packet.action()) {
            case CYCLE_PRIMARY -> TerminalPluginManager.cycleSetting(terminal, packet.pluginIndex(), false);
            case CYCLE_SECONDARY -> TerminalPluginManager.cycleSetting(terminal, packet.pluginIndex(), true);
            case REMOVE -> {
                return TerminalPluginManager.remove(terminal, packet.pluginIndex());
            }
            case MOVE_UP -> TerminalPluginManager.move(terminal, packet.pluginIndex(), -1);
            case MOVE_DOWN -> TerminalPluginManager.move(terminal, packet.pluginIndex(), 1);
            case TOGGLE_ENABLED -> TerminalPluginManager.toggleEnabled(terminal, packet.pluginIndex());
            case SET_ALCHEMY_FILTER -> TerminalPluginManager.updateAlchemy(
                terminal,
                packet.pluginIndex(),
                settings -> settings.withEntry(
                    packet.entryIndex(),
                    settings.normalizedEntries().get(Math.clamp(packet.entryIndex(), 0, AlchemySettings.ENTRY_COUNT - 1))
                        .withFilter(packet.filter())
                )
            );
            case CYCLE_ALCHEMY_TRIGGER -> TerminalPluginManager.updateAlchemy(
                terminal,
                packet.pluginIndex(),
                settings -> {
                    int index = Math.clamp(packet.entryIndex(), 0, AlchemySettings.ENTRY_COUNT - 1);
                    AlchemySettings.AlchemyEntry entry = settings.normalizedEntries().get(index);
                    return settings.withEntry(index, entry.withTrigger(entry.trigger().next()));
                }
            );
            case ADJUST_ALCHEMY_VALUE -> TerminalPluginManager.updateAlchemy(
                terminal,
                packet.pluginIndex(),
                settings -> {
                    int index = Math.clamp(packet.entryIndex(), 0, AlchemySettings.ENTRY_COUNT - 1);
                    AlchemySettings.AlchemyEntry entry = settings.normalizedEntries().get(index);
                    return settings.withEntry(index, entry.withValue(entry.value() + packet.value()));
                }
            );
            case CYCLE_ALCHEMY_NEARBY -> TerminalPluginManager.updateAlchemy(
                terminal,
                packet.pluginIndex(),
                settings -> settings.withNearby(settings.nearby().next())
            );
            case ADJUST_ALCHEMY_RADIUS -> TerminalPluginManager.updateAlchemy(
                terminal,
                packet.pluginIndex(),
                settings -> new AlchemySettings(
                    settings.normalizedEntries(),
                    settings.nearby(),
                    Math.clamp(settings.radius() + Math.round(packet.value()), 1, 8),
                    settings.interval(),
                    settings.matchAmplifier()
                )
            );
            case SET_FILTER_SLOT -> TerminalPluginManager.update(terminal, packet.pluginIndex(), plugin -> {
                FilterContent content = plugin.getOrDefault(ModComponents.FILTER_CONTENT, new FilterContent());
                NonNullList<ItemStack> list = NonNullList.of(
                    ItemStack.EMPTY,
                    content.list().toArray(new ItemStack[0])
                );
                int slot = Math.clamp(packet.entryIndex(), 0, Math.max(0, list.size() - 1));
                if (slot < list.size()) {
                    list.set(slot, packet.filter().isEmpty() ? ItemStack.EMPTY : packet.filter().copyWithCount(1));
                }
                plugin.set(ModComponents.FILTER_CONTENT, content.setList(list));
                return plugin;
            });
            case ADJUST_MAGNET_RANGE -> TerminalPluginManager.update(terminal, packet.pluginIndex(), plugin -> {
                MagnetSettings magnetSettings = plugin.getOrDefault(
                    AddonDataComponents.MAGNET_SETTINGS, MagnetSettings.DEFAULT);
                plugin.set(
                    AddonDataComponents.MAGNET_SETTINGS,
                    magnetSettings.withRange(magnetSettings.range() + Math.round(packet.value()))
                );
                return plugin;
            });
            case ADJUST_FEEDING_THRESHOLD -> TerminalPluginManager.update(terminal, packet.pluginIndex(), plugin -> {
                FeedingSettings feedingSettings = plugin.getOrDefault(
                    AddonDataComponents.FEEDING_SETTINGS, FeedingSettings.DEFAULT);
                plugin.set(AddonDataComponents.FEEDING_SETTINGS, new FeedingSettings(
                    Math.clamp(feedingSettings.hungerThreshold() + Math.round(packet.value()), 1, 20),
                    feedingSettings.allowHarmful(),
                    feedingSettings.keepSaturation()
                ));
                return plugin;
            });
            case ADJUST_FLUID_BATCH -> TerminalPluginManager.update(terminal, packet.pluginIndex(), plugin -> {
                FluidSettings fluidSettings = plugin.getOrDefault(
                    AddonDataComponents.FLUID_SETTINGS, FluidSettings.DEFAULT);
                plugin.set(AddonDataComponents.FLUID_SETTINGS, new FluidSettings(
                    fluidSettings.mode(),
                    Math.clamp(fluidSettings.batch() + Math.round(packet.value()), 1, 16),
                    fluidSettings.capacityBuckets()
                ));
                return plugin;
            });
            case ADJUST_FLUID_CAPACITY -> TerminalPluginManager.update(terminal, packet.pluginIndex(), plugin -> {
                FluidSettings fluidSettings = plugin.getOrDefault(
                    AddonDataComponents.FLUID_SETTINGS, FluidSettings.DEFAULT);
                plugin.set(AddonDataComponents.FLUID_SETTINGS, new FluidSettings(
                    fluidSettings.mode(),
                    fluidSettings.batch(),
                    Math.clamp(fluidSettings.capacityBuckets() + Math.round(packet.value()), 1, 64)
                ));
                return plugin;
            });
            case TOGGLE_TOOL_SWAP_RETURN -> TerminalPluginManager.update(terminal, packet.pluginIndex(), plugin -> {
                ToolSwapSettings toolSwapSettings = plugin.getOrDefault(
                    AddonDataComponents.TOOL_SWAP_SETTINGS, ToolSwapSettings.DEFAULT);
                plugin.set(
                    AddonDataComponents.TOOL_SWAP_SETTINGS,
                    toolSwapSettings.withReturnWorn(!toolSwapSettings.returnWorn())
                );
                return plugin;
            });
            case ADJUST_XP_STORE_LEVEL -> TerminalPluginManager.update(terminal, packet.pluginIndex(), plugin -> {
                XpPumpSettings xpSettings = plugin.getOrDefault(
                    AddonDataComponents.XP_PUMP_SETTINGS, XpPumpSettings.DEFAULT);
                plugin.set(
                    AddonDataComponents.XP_PUMP_SETTINGS,
                    xpSettings.withStoreLevel(xpSettings.storeLevel() + Math.round(packet.value()))
                );
                return plugin;
            });
            case ADJUST_XP_KEEP_LEVEL -> TerminalPluginManager.update(terminal, packet.pluginIndex(), plugin -> {
                XpPumpSettings xpSettings = plugin.getOrDefault(
                    AddonDataComponents.XP_PUMP_SETTINGS, XpPumpSettings.DEFAULT);
                plugin.set(
                    AddonDataComponents.XP_PUMP_SETTINGS,
                    xpSettings.withKeepLevel(xpSettings.keepLevel() + Math.round(packet.value()))
                );
                return plugin;
            });
            case ADJUST_XP_BATCH -> TerminalPluginManager.update(terminal, packet.pluginIndex(), plugin -> {
                XpPumpSettings xpSettings = plugin.getOrDefault(
                    AddonDataComponents.XP_PUMP_SETTINGS, XpPumpSettings.DEFAULT);
                plugin.set(
                    AddonDataComponents.XP_PUMP_SETTINGS,
                    xpSettings.withGemsPerCycle(xpSettings.gemsPerCycle() + Math.round(packet.value()))
                );
                return plugin;
            });
            case TOGGLE_CHARGING_RECIPES -> TerminalPluginManager.update(terminal, packet.pluginIndex(), plugin -> {
                ChargingSettings chargingSettings = plugin.getOrDefault(
                    AddonDataComponents.CHARGING_SETTINGS, ChargingSettings.DEFAULT);
                plugin.set(
                    AddonDataComponents.CHARGING_SETTINGS,
                    chargingSettings.withRunRecipes(!chargingSettings.runRecipes())
                );
                return plugin;
            });
            case TOGGLE_CHARGING_ITEMS -> TerminalPluginManager.update(terminal, packet.pluginIndex(), plugin -> {
                ChargingSettings chargingSettings = plugin.getOrDefault(
                    AddonDataComponents.CHARGING_SETTINGS, ChargingSettings.DEFAULT);
                plugin.set(
                    AddonDataComponents.CHARGING_SETTINGS,
                    chargingSettings.withChargeItems(!chargingSettings.chargeItems())
                );
                return plugin;
            });
            case ANVIL_PROCESS_NOW, SMITHING_NOW -> {
                ItemStack pluginStack = TerminalPluginManager.installed(terminal).get(packet.pluginIndex());
                TerminalPlugin plugin = TerminalPluginRegistry.behaviorOf(pluginStack).orElse(null);
                if (plugin != null) {
                    TerminalStorage storage = TerminalStorageResolver.resolve(player, terminal);
                    boolean handled = plugin.onAction(
                        new PluginContext(player, terminal, pluginStack, storage, player.tickCount),
                        packet.action()
                    );
                    if (!handled) {
                        PluginActionPacket.sendFeedback(
                            player,
                            "message.anvilcraft_terminal_plugins.nothing_to_process"
                        );
                    }
                }
            }
            case SET_SAMPLE_SLOT -> TerminalPluginManager.update(terminal, packet.pluginIndex(), plugin -> {
                PluginSample sample = plugin.getOrDefault(
                    AddonDataComponents.PLUGIN_SAMPLE, PluginSample.EMPTY);
                plugin.set(
                    AddonDataComponents.PLUGIN_SAMPLE,
                    sample.withSample(packet.filter().isEmpty()
                        ? ItemStack.EMPTY
                        : packet.filter().copyWithCount(1))
                );
                return plugin;
            });
            default -> {
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PluginActionPacket.TYPE;
    }
}
