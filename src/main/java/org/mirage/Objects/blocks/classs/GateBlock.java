package org.mirage.Objects.blocks.classs;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Convex89524
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mirage.Mirage_gfbs;
import org.mirage.Objects.blockEntity.GateBlockEntity;
import org.mirage.Phenomenon.network.Network.ClientEventHandler;
import org.mirage.Phenomenon.network.Network.ClientToServer;
import org.mirage.Tools.Task;

public class GateBlock extends Block implements EntityBlock {

    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    private static final Set<BlockPos> NON_PLAYER_REMOVAL_POSITIONS = ConcurrentHashMap.newKeySet();

    private final Supplier<Block> collisionBlockSupplier;

    private static final BlockPos[] COLLISION_OFFSETS = new BlockPos[]{
            new BlockPos(0, 0, 0),
            new BlockPos(0, 1, 0),
            new BlockPos(0, 2, 0),
            new BlockPos(0, 0, 1),
            new BlockPos(0, 1, 1),
            new BlockPos(0, 2, 1),
            new BlockPos(0, 0, 2),
            new BlockPos(0, 1, 2),
            new BlockPos(0, 2, 2),
            new BlockPos(0, 0, -1),
            new BlockPos(0, 1, -1),
            new BlockPos(0, 2, -1),
            new BlockPos(0, 0, -2),
            new BlockPos(0, 1, -2),
            new BlockPos(0, 2, -2)
    };

    private static final BlockPos[] COLLISION_OFFSETS_2 = new BlockPos[]{
            new BlockPos(0, 0, 3),
            new BlockPos(0, 1, 3),
            new BlockPos(0, 2, 3),
            new BlockPos(0, 0, -3),
            new BlockPos(0, 1, -3),
            new BlockPos(0, 2, -3),
    };

    public GateBlock(Properties properties, Supplier<Block> collisionBlockSupplier) {
        super(properties);
        this.collisionBlockSupplier = collisionBlockSupplier;
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(OPEN, Boolean.FALSE)
                        .setValue(AXIS, Direction.Axis.Z)
        );
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state,
                                        @NotNull BlockGetter level,
                                        @NotNull BlockPos pos,
                                        @NotNull CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state,
                                                 @NotNull BlockGetter level,
                                                 @NotNull BlockPos pos,
                                                 @NotNull CollisionContext context) {
        return Shapes.empty();
    }


    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void onPlace(@NotNull BlockState state,
                        @NotNull Level level,
                        @NotNull BlockPos pos,
                        @NotNull BlockState oldState,
                        boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        if (level.isClientSide) {
            return;
        }
        if (oldState.is(state.getBlock())) {
            return;
        }
        if (!state.getValue(OPEN)) {
            placeCollisionBlocks(level, pos, false);
        }
    }

    @Override
    public void onRemove(@NotNull BlockState state,
                         @NotNull Level level,
                         @NotNull BlockPos pos,
                         @NotNull BlockState newState,
                         boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                removeCollisionBlocks(level, pos, false);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        } else {
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    private void placeCollisionBlocks(Level level, BlockPos gatePos, boolean canSleep) {
        if (this.collisionBlockSupplier == null) {
            return;
        }
        Block collisionBlock = this.collisionBlockSupplier.get();
        if (collisionBlock == null) {
            return;
        }

        BlockState gateState = level.getBlockState(gatePos);
        Direction.Axis axis = gateState.hasProperty(AXIS) ? gateState.getValue(AXIS) : Direction.Axis.Z;

        if (canSleep) {
            Task.delay(() -> {
                BlockState currentState = level.getBlockState(gatePos);
                if (!(currentState.getBlock() instanceof GateBlock)) {
                    return;
                }
                if (currentState.hasProperty(OPEN) && currentState.getValue(OPEN)) {
                    return;
                }

                Direction.Axis currentAxis = currentState.hasProperty(AXIS)
                        ? currentState.getValue(AXIS)
                        : Direction.Axis.Z;

                for (BlockPos offset : COLLISION_OFFSETS) {
                    BlockPos realOffset = rotateOffsetForAxis(offset, currentAxis);
                    BlockPos targetPos = gatePos.offset(realOffset);
                    BlockState existing = level.getBlockState(targetPos);
                    if (existing.isAir() || existing.canBeReplaced()) {
                        level.setBlock(targetPos, collisionBlock.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }, 7, TimeUnit.SECONDS);

            for (BlockPos offset : COLLISION_OFFSETS_2) {
                BlockPos realOffset = rotateOffsetForAxis(offset, axis);
                BlockPos targetPos = gatePos.offset(realOffset);
                BlockState existing = level.getBlockState(targetPos);
                if (existing.isAir() || existing.canBeReplaced()) {
                    level.setBlock(targetPos, collisionBlock.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        } else {
            for (BlockPos offset : COLLISION_OFFSETS) {
                BlockPos realOffset = rotateOffsetForAxis(offset, axis);
                BlockPos targetPos = gatePos.offset(realOffset);
                BlockState existing = level.getBlockState(targetPos);
                if (existing.isAir() || existing.canBeReplaced()) {
                    level.setBlock(targetPos, collisionBlock.defaultBlockState(), Block.UPDATE_ALL);
                }
            }

            for (BlockPos offset : COLLISION_OFFSETS_2) {
                BlockPos realOffset = rotateOffsetForAxis(offset, axis);
                BlockPos targetPos = gatePos.offset(realOffset);
                BlockState existing = level.getBlockState(targetPos);
                if (existing.isAir() || existing.canBeReplaced()) {
                    level.setBlock(targetPos, collisionBlock.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }


    private static BlockPos rotateOffsetForAxis(BlockPos offset, Direction.Axis axis) {
        if (axis == Direction.Axis.X) {
            return offset;
        } else {
            return new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
        }
    }


    private void removeCollisionBlocks(Level level, BlockPos gatePos, boolean onlyCore) {
        if (this.collisionBlockSupplier == null) {
            return;
        }
        Block collisionBlock = this.collisionBlockSupplier.get();
        if (collisionBlock == null) {
            return;
        }

        BlockState gateState = level.getBlockState(gatePos);
        Direction.Axis axis = gateState.hasProperty(AXIS) ? gateState.getValue(AXIS) : Direction.Axis.Z;

        Set<BlockPos> positionsToRemove = new HashSet<>();

        if (onlyCore){
            for (BlockPos offset : COLLISION_OFFSETS) {
                BlockPos realOffset = rotateOffsetForAxis(offset, axis);
                BlockPos targetPos = gatePos.offset(realOffset);
                positionsToRemove.add(targetPos.immutable());
            }
        } else {
            for (BlockPos offset : COLLISION_OFFSETS) {
                BlockPos realOffset = rotateOffsetForAxis(offset, axis);
                BlockPos targetPos = gatePos.offset(realOffset);
                positionsToRemove.add(targetPos.immutable());
            }

            for (BlockPos offset : COLLISION_OFFSETS_2) {
                BlockPos realOffset = rotateOffsetForAxis(offset, axis);
                BlockPos targetPos = gatePos.offset(realOffset);
                positionsToRemove.add(targetPos.immutable());
            }
        }

        NON_PLAYER_REMOVAL_POSITIONS.addAll(positionsToRemove);

        try {
            if (onlyCore){
                for (BlockPos offset : COLLISION_OFFSETS) {
                    BlockPos realOffset = rotateOffsetForAxis(offset, axis);
                    BlockPos targetPos = gatePos.offset(realOffset);
                    BlockState existing = level.getBlockState(targetPos);
                    if (existing.is(collisionBlock)) {
                        level.removeBlock(targetPos, false);
                    }
                }
            } else {
                for (BlockPos offset : COLLISION_OFFSETS) {
                    BlockPos realOffset = rotateOffsetForAxis(offset, axis);
                    BlockPos targetPos = gatePos.offset(realOffset);
                    BlockState existing = level.getBlockState(targetPos);
                    if (existing.is(collisionBlock)) {
                        level.removeBlock(targetPos, false);
                    }
                }

                for (BlockPos offset : COLLISION_OFFSETS_2) {
                    BlockPos realOffset = rotateOffsetForAxis(offset, axis);
                    BlockPos targetPos = gatePos.offset(realOffset);
                    BlockState existing = level.getBlockState(targetPos);
                    if (existing.is(collisionBlock)) {
                        level.removeBlock(targetPos, false);
                    }
                }
            }
        } finally {
            Task.delay(() -> {
                NON_PLAYER_REMOVAL_POSITIONS.removeAll(positionsToRemove);
            }, 1, TimeUnit.SECONDS);
        }
    }

    public static boolean isNonPlayerRemoval(BlockPos pos) {
        return NON_PLAYER_REMOVAL_POSITIONS.contains(pos);
    }

    public void applyOpenStateDirect(Level level, BlockPos gatePos, boolean open) {
        if (level.isClientSide) return;

        BlockState state = level.getBlockState(gatePos);
        if (!(state.getBlock() instanceof GateBlock)) return;

        if (open) {
            removeCollisionBlocks(level, gatePos, true);
        } else {
            placeCollisionBlocks(level, gatePos, true);
        }

        if (state.getValue(OPEN) != open) {
            level.setBlock(gatePos, state.setValue(OPEN, open), Block.UPDATE_ALL);
        }
    }

    public void applyOpenState(Level level, BlockPos gatePos, boolean open) {
        if (level.isClientSide) {
            ClientToServer.runWithBoolean("mirage_gate_operation", open,(player, _open) -> {
                applyOpenStateDirect(player.level(), gatePos, _open);
            });
        } else {
            applyOpenStateDirect(level, gatePos, open);
        }
    }

    public static boolean isBusy = false;

    static {
        ClientEventHandler.registerEvent("mirage_gate_busy",(compoundTag)->{
            isBusy = true;
            Task.delay(()->{isBusy = false;},2, TimeUnit.SECONDS);
        });
    }

    public void destroyFromCollision(Level level, BlockPos gatePos) {
        if (level.isClientSide) {
            return;
        }
        if (isBusy){
            return;
        }
        removeCollisionBlocks(level, gatePos, false);
        level.removeBlock(gatePos, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN, AXIS);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new GateBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction dir = context.getHorizontalDirection();
        Direction.Axis axis = dir.getAxis();

        return this.defaultBlockState()
                .setValue(OPEN, Boolean.FALSE)
                .setValue(AXIS, axis);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        if (rot == Rotation.CLOCKWISE_90 || rot == Rotation.COUNTERCLOCKWISE_90) {
            Direction.Axis axis = state.getValue(AXIS);
            return state.setValue(AXIS, axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
        }
        return state;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(Direction.NORTH));
    }

}
