package org.mirage.Objects.blocks.classs.FlBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.mirage.Objects.blocks.Bases.FlBlock.AbstractFluorescentLampBlock;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC
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

import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class ArcLampBlock extends AbstractFluorescentLampBlock {

    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;

    public ArcLampBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACE, AttachFace.FLOOR));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction clickedFace = ctx.getClickedFace();

        AttachFace face;
        if (clickedFace == Direction.UP) {
            face = AttachFace.FLOOR;
        } else if (clickedFace == Direction.DOWN) {
            face = AttachFace.CEILING;
        } else {
            face = AttachFace.WALL;
        }

        Direction horizontal = ctx.getHorizontalDirection();

        BlockState state = super.getStateForPlacement(ctx);
        if (state == null) state = this.defaultBlockState();

        return state
                .setValue(FACE, face)
                .setValue(FACING, horizontal);
    }
}
