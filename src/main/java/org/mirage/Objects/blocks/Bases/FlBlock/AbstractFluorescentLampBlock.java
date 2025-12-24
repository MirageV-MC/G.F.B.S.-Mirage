package org.mirage.Objects.blocks.Bases.FlBlock;

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
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.NotNull;
import org.mirage.Objects.blocks.Control.FluorescentTubeRegistry;
import org.mirage.Objects.blocks.Control.FluorescentTubeSavedData;

public abstract class AbstractFluorescentLampBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of()
                .strength(0.3F)
                .sound(SoundType.GLASS)
                .lightLevel(state -> state.getValue(LIT) ? 15 : 0)
                .noOcclusion();
    }

    protected AbstractFluorescentLampBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(LIT, Boolean.FALSE)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    protected int getLitLightLevel(@NotNull BlockState state) {
        return 15;
    }

    protected void onPoweredStateChanged(Level level, BlockPos pos, BlockState newState) {
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction dir = ctx.getClickedFace();
        return this.defaultBlockState()
                .setValue(FACING, dir)
                .setValue(LIT, Boolean.FALSE);
    }

    @Override
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state,
                            LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            FluorescentTubeRegistry.register(serverLevel, pos);
            FluorescentTubeSavedData.get(serverLevel).add(pos);
        }

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            try {
                Class<?> api = Class.forName("org.mirage.Objects.blocks.Control.FluorescentTubeClientAPI");
                api.getMethod("registerTube", BlockPos.class).invoke(null, pos);
                if (level.isClientSide) {
                    boolean desired = api.getField("globalState").getBoolean(null);
                    BlockState cur = level.getBlockState(pos);
                    if (cur.getBlock() instanceof AbstractFluorescentLampBlock) {
                        if (!java.util.Objects.equals(cur.getValue(LIT), desired)) {
                            level.setBlock(pos, cur.setValue(LIT, desired), Block.UPDATE_CLIENTS);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        });
    }

    @Override
    public boolean isSignalSource(@NotNull BlockState state) {
        return false;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(LIT) ? getLitLightLevel(state) : 0;
    }

    @Override
    public void onRemove(@NotNull BlockState state, Level level, @NotNull BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!level.isClientSide
                && level instanceof ServerLevel serverLevel
                && state.getBlock() != newState.getBlock()) {
            FluorescentTubeRegistry.unregister(serverLevel, pos);
            FluorescentTubeSavedData.get(serverLevel).remove(pos);
        }
        if (level.isClientSide && state.getBlock() != newState.getBlock()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                try {
                    Class<?> api = Class.forName("org.mirage.Objects.blocks.Control.FluorescentTubeClientAPI");
                    api.getMethod("unregisterTube", BlockPos.class).invoke(null, pos);
                } catch (Throwable ignored) {
                }
            });
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}