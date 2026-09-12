/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.block;

import dev.anvilcraft.addon.terminalplugins.block.entity.PluginStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * 插件安装台：右键打开界面，把插件安装到终端上或从终端上取下。
 */
public class PluginStationBlock extends Block implements EntityBlock {
    public PluginStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hitResult
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        MenuProvider provider = new SimpleMenuProvider(
            (containerId, inventory, p) -> new dev.anvilcraft.addon.terminalplugins.inventory.PluginStationMenu(
                containerId, inventory, pos
            ),
            Component.translatable("container.anvilcraft_terminal_plugins.plugin_station")
        );
        player.openMenu(provider, pos);
        return InteractionResult.CONSUME;
    }

    /**
     * 只有方块真的被移除时才掉落内容物。
     *
     * <p>不能写在 {@code BlockEntity#setRemoved()} 里：区块卸载同样会调用它，
     * 那样玩家走远一点东西就全掉地上了。</p>
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PluginStationBlockEntity station) {
            station.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PluginStationBlockEntity(
            dev.anvilcraft.addon.terminalplugins.init.AddonBlockEntities.PLUGIN_STATION.get(),
            pos,
            state
        );
    }
}
