package org.mirage.Objects.blocks.classs.Gate;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mirage.Objects.blockEntity.CheckPointGate.CheckPointGateBlockEntity;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class CheckPointGateBlock extends GateBlock implements EntityBlock {
    private static final Set<BlockPos> NON_PLAYER_REMOVAL_POSITIONS =
            ConcurrentHashMap.newKeySet();

    public CheckPointGateBlock(Properties properties,
                               Supplier<Block> collisionBlockSupplier) {
        super(properties, collisionBlockSupplier);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos,
                                                @NotNull BlockState state) {
        return new CheckPointGateBlockEntity(pos, state);
    }

    public static boolean isNonPlayerRemoval(BlockPos pos) {
        return NON_PLAYER_REMOVAL_POSITIONS.contains(pos);
    }
    
    public static void markNonPlayerRemoval(BlockPos pos) {
        NON_PLAYER_REMOVAL_POSITIONS.add(pos);
    }

    public static void unmarkNonPlayerRemoval(BlockPos pos) {
        NON_PLAYER_REMOVAL_POSITIONS.remove(pos);
    }
}
