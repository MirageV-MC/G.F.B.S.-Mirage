package org.mirage.Objects.blocks.classs;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Convex89524
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class GateCollisionBlock extends Block {

    private static final VoxelShape COLLISION_SHAPE =
            Block.box(0, 0, 0, 16, 16, 16);

    public GateCollisionBlock(Properties properties) {
        super(properties.noOcclusion());
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state,
                                        @NotNull BlockGetter level,
                                        @NotNull BlockPos pos,
                                        @NotNull CollisionContext context) {
        return COLLISION_SHAPE;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state,
                                                 @NotNull BlockGetter level,
                                                 @NotNull BlockPos pos,
                                                 @NotNull CollisionContext context) {
        return COLLISION_SHAPE;
    }

    @Override
    public void onRemove(@NotNull BlockState state,
                         @NotNull Level level,
                         @NotNull BlockPos pos,
                         @NotNull BlockState newState,
                         boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                if (GateBlock.isNonPlayerRemoval(pos)) {
                    super.onRemove(state, level, pos, newState, isMoving);
                    return;
                }

                int radius = 3;
                outer:
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -radius; dy <= radius; dy++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            BlockPos checkPos = pos.offset(dx, dy, dz);
                            BlockState bs = level.getBlockState(checkPos);
                            if (bs.getBlock() instanceof GateBlock gateBlock) {
                                gateBlock.destroyFromCollision(level, checkPos);
                                break outer;
                            }
                        }
                    }
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        } else {
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
