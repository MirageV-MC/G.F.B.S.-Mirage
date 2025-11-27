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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class WhiteCubeLampBlock extends AbstractFluorescentLampBlock {
    private static final VoxelShape SHAPE = Block.box(
            0.0D, 0.0D, 0.0D,
            16.0D, 16.0D, 16.0D
    );

    public WhiteCubeLampBlock() {
        super(
                AbstractFluorescentLampBlock.defaultProperties()
                        .lightLevel(state -> state.getValue(LIT) ? 15 : 0)
        );
    }

    @Override
    protected int getLitLightLevel(@NotNull BlockState state) {
        return 15;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state,
                                        @NotNull BlockGetter level,
                                        @NotNull BlockPos pos,
                                        @NotNull CollisionContext context) {
        return SHAPE;
    }
}