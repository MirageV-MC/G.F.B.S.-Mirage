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
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class FluorescentTubeBlock extends AbstractFluorescentLampBlock {

    protected static final VoxelShape SHAPE_X = Shapes.or(
            net.minecraft.world.level.block.Block.box(0.0D, 14.0D, 6.0D,
                    16.0D, 16.0D, 10.0D)
    );

    protected static final VoxelShape SHAPE_Z = Shapes.or(
            net.minecraft.world.level.block.Block.box(6.0D, 14.0D, 0.0D,
                    10.0D, 16.0D, 16.0D)
    );

    public FluorescentTubeBlock() {
        this(AbstractFluorescentLampBlock.defaultProperties());
    }

    public FluorescentTubeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                               @NotNull BlockPos pos, @NotNull CollisionContext context) {
        Direction dir = state.getValue(FACING);
        if (dir == Direction.NORTH || dir == Direction.SOUTH) {
            return SHAPE_X;
        }
        return SHAPE_Z;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction dir = ctx.getHorizontalDirection().getOpposite();
        return this.defaultBlockState()
                .setValue(FACING, dir)
                .setValue(LIT, Boolean.FALSE);
    }
}
