package org.mirage.gfbs.objects.blocks.classs.ColoredDoor;

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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mirage.gfbs.ModSoundEvents;
import org.mirage.gfbs.objects.blockEntity.ColoredDoor.ColoredDoorBlockEntity;

import net.minecraft.sounds.SoundSource;

import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class ColoredDoorBlock extends BaseEntityBlock {

    public static final BooleanProperty PLAYER_CAN_OPEN = BooleanProperty.create("player_can_open");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty COLLIDABLE = BooleanProperty.create("collidable");
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;

    protected static final VoxelShape SHAPE_NORTH = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 2.0D);
    protected static final VoxelShape SHAPE_SOUTH = Block.box(0.0D, 0.0D, 14.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape SHAPE_WEST = Block.box(0.0D, 0.0D, 0.0D, 2.0D, 16.0D, 16.0D);
    protected static final VoxelShape SHAPE_EAST = Block.box(14.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    protected static final VoxelShape SHAPE_NORTH_UPPER = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 2.0D);
    protected static final VoxelShape SHAPE_SOUTH_UPPER = Block.box(0.0D, 0.0D, 14.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape SHAPE_WEST_UPPER = Block.box(0.0D, 0.0D, 0.0D, 2.0D, 16.0D, 16.0D);
    protected static final VoxelShape SHAPE_EAST_UPPER = Block.box(14.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    public enum DoorColor {
        BLUE, RED, BLACK, ORANGE
    }

    private final DoorColor color;

    public ColoredDoorBlock(Properties properties, DoorColor color) {
        super(properties);
        this.color = color;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(PLAYER_CAN_OPEN, Boolean.FALSE)
                .setValue(POWERED, Boolean.FALSE)
                .setValue(OPEN, Boolean.FALSE)
                .setValue(COLLIDABLE, Boolean.TRUE)
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(HINGE, DoorHingeSide.LEFT)
        );
    }

    public DoorColor getColor() {
        return color;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN, COLLIDABLE, PLAYER_CAN_OPEN, POWERED, FACING, HALF, HINGE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos blockpos = context.getClickedPos();
        Level level = context.getLevel();
        if (blockpos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(blockpos.above()).canBeReplaced(context)) {
            return this.defaultBlockState()
                    .setValue(FACING, context.getHorizontalDirection())
                    .setValue(HALF, DoubleBlockHalf.LOWER)
                    .setValue(HINGE, this.getHinge(context));
        } else {
            return null;
        }
    }

    private DoorHingeSide getHinge(BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction direction = context.getHorizontalDirection();
        BlockPos posUp = pos.above();
        Direction directionCCW = direction.getCounterClockWise();
        BlockPos posLeft = pos.relative(directionCCW);
        BlockState stateLeft = level.getBlockState(posLeft);
        BlockPos posLeftUp = posUp.relative(directionCCW);
        BlockState stateLeftUp = level.getBlockState(posLeftUp);
        Direction directionCW = direction.getClockWise();
        BlockPos posRight = pos.relative(directionCW);
        BlockState stateRight = level.getBlockState(posRight);
        BlockPos posRightUp = posUp.relative(directionCW);
        BlockState stateRightUp = level.getBlockState(posRightUp);
        int leftMatch = (stateLeft.is(this) || stateLeftUp.is(this)) ? -1 : 0;
        int rightMatch = (stateRight.is(this) || stateRightUp.is(this)) ? 1 : 0;
        boolean hasLeftSolid = stateLeft.isCollisionShapeFullBlock(level, posLeft) || stateLeftUp.isCollisionShapeFullBlock(level, posLeftUp);
        boolean hasRightSolid = stateRight.isCollisionShapeFullBlock(level, posRight) || stateRightUp.isCollisionShapeFullBlock(level, posRightUp);
        if ((!hasLeftSolid || hasRightSolid) && leftMatch <= 0 && rightMatch <= 0) {
            if ((!hasRightSolid || hasLeftSolid) && leftMatch <= 0 && rightMatch <= 0) {
                int ox = direction.getStepX();
                int oz = direction.getStepZ();
                Vec3 vec3 = context.getClickLocation();
                double x = vec3.x - (double)pos.getX();
                double z = vec3.z - (double)pos.getZ();
                return (ox >= 0 || !(z < 0.5D)) && (ox <= 0 || !(z > 0.5D)) && (oz >= 0 || !(x > 0.5D)) && (oz <= 0 || !(x < 0.5D)) ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
            } else {
                return DoorHingeSide.RIGHT;
            }
        } else {
            return DoorHingeSide.LEFT;
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y && half == DoubleBlockHalf.LOWER == (direction == Direction.UP)) {
            return neighborState.is(this) && neighborState.getValue(HALF) != half ? state.setValue(FACING, neighborState.getValue(FACING)).setValue(OPEN, neighborState.getValue(OPEN)).setValue(COLLIDABLE, neighborState.getValue(COLLIDABLE)).setValue(PLAYER_CAN_OPEN, neighborState.getValue(PLAYER_CAN_OPEN)).setValue(POWERED, neighborState.getValue(POWERED)).setValue(HINGE, neighborState.getValue(HINGE)) : Blocks.AIR.defaultBlockState();
        } else {
            return half == DoubleBlockHalf.LOWER && direction == Direction.DOWN && !state.canSurvive(level, currentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            DoubleBlockHalf half = state.getValue(HALF);
            if (half == DoubleBlockHalf.UPPER) {
                BlockPos below = pos.below();
                BlockState belowState = level.getBlockState(below);
                if (belowState.is(state.getBlock()) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER) {
                    level.setBlock(below, Blocks.AIR.defaultBlockState(), 35);
                    level.levelEvent(player, 2001, below, Block.getId(belowState));
                }
            } else {
                BlockPos above = pos.above();
                BlockState aboveState = level.getBlockState(above);
                if (aboveState.is(state.getBlock()) && aboveState.getValue(HALF) == DoubleBlockHalf.UPPER) {
                    level.setBlock(above, Blocks.AIR.defaultBlockState(), 35);
                    level.levelEvent(player, 2001, above, Block.getId(aboveState));
                }
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return RenderShape.INVISIBLE;
        }
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        if (!hasActiveDoorShape(state)) {
            return Shapes.empty();
        }
        return getShapeForFacing(state.getValue(FACING), state.getValue(HALF));
    }

    @Override
    public @NotNull VoxelShape getVisualShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        if (!hasActiveDoorShape(state)) {
            return Shapes.empty();
        }
        return getShapeForFacing(state.getValue(FACING), state.getValue(HALF));
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        if (!hasActiveDoorShape(state)) {
            return Shapes.empty();
        }
        return getShapeForFacing(state.getValue(FACING), state.getValue(HALF));
    }

    @Override
    public @NotNull VoxelShape getInteractionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        if (!hasActiveDoorShape(state)) {
            return Shapes.empty();
        }
        return getShapeForFacing(state.getValue(FACING), state.getValue(HALF));
    }

    private boolean hasActiveDoorShape(BlockState state) {
        return !state.getValue(OPEN) && state.getValue(COLLIDABLE);
    }

    private VoxelShape getShapeForFacing(Direction facing, DoubleBlockHalf half) {
        Direction actualFacing = facing.getOpposite();
        if (half == DoubleBlockHalf.UPPER) {
            switch (actualFacing) {
                case SOUTH:
                    return SHAPE_SOUTH_UPPER;
                case EAST:
                    return SHAPE_EAST_UPPER;
                case WEST:
                    return SHAPE_WEST_UPPER;
                case NORTH:
                default:
                    return SHAPE_NORTH_UPPER;
            }
        } else {
            switch (actualFacing) {
                case SOUTH:
                    return SHAPE_SOUTH;
                case EAST:
                    return SHAPE_EAST;
                case WEST:
                    return SHAPE_WEST;
                case NORTH:
                default:
                    return SHAPE_NORTH;
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(PLAYER_CAN_OPEN)) {
            return InteractionResult.PASS;
        }
        if (state.getValue(OPEN) || !state.getValue(COLLIDABLE)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            this.openDoorFromAnyHalf(level, pos, state);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
            BlockPos upperPos = lowerPos.above();
            BlockState lowerState = level.getBlockState(lowerPos);
            BlockState upperState = level.getBlockState(upperPos);
            if (lowerState.is(this) && lowerState.getValue(HALF) == DoubleBlockHalf.LOWER && upperState.is(this) && upperState.getValue(HALF) == DoubleBlockHalf.UPPER) {
                boolean powered = level.hasNeighborSignal(lowerPos) || level.hasNeighborSignal(upperPos);
                boolean wasPowered = lowerState.getValue(POWERED);
                if (powered != wasPowered) {
                    level.setBlock(lowerPos, lowerState.setValue(POWERED, powered), 3);
                    level.setBlock(upperPos, upperState.setValue(POWERED, powered), 3);
                    if (powered) {
                        this.openDoorFromAnyHalf(level, lowerPos, lowerState.setValue(POWERED, powered));
                    }
                }
            }
        }
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
    }

    private void openDoorFromAnyHalf(Level level, BlockPos pos, BlockState state) {
        BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        BlockPos upperPos = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos : pos.above();
        BlockState lowerState = level.getBlockState(lowerPos);
        BlockState upperState = level.getBlockState(upperPos);
        if (!lowerState.is(this) || lowerState.getValue(HALF) != DoubleBlockHalf.LOWER) {
            return;
        }
        if (!upperState.is(this) || upperState.getValue(HALF) != DoubleBlockHalf.UPPER) {
            return;
        }
        if (lowerState.getValue(OPEN) || !lowerState.getValue(COLLIDABLE)) {
            return;
        }
        level.setBlock(lowerPos, lowerState.setValue(OPEN, true).setValue(COLLIDABLE, false), 3);
        level.setBlock(upperPos, upperState.setValue(OPEN, true).setValue(COLLIDABLE, false), 3);
        level.playSound(null, lowerPos, ModSoundEvents.getSoundOrThrow("mobile.door_open"), SoundSource.BLOCKS, 1.0F, 1.0F);
        BlockEntity be = level.getBlockEntity(lowerPos);
        if (be instanceof ColoredDoorBlockEntity doorBlockEntity) {
            doorBlockEntity.openDoor();
        }

        Direction facing = lowerState.getValue(FACING);
        DoorHingeSide hinge = lowerState.getValue(HINGE);
        for (Direction dir : new Direction[]{facing.getClockWise(), facing.getCounterClockWise()}) {
            BlockPos neighborPos = lowerPos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.is(this) && neighborState.getValue(HALF) == DoubleBlockHalf.LOWER) {
                if (neighborState.getValue(FACING) == facing && neighborState.getValue(HINGE) != hinge) {
                    if (!neighborState.getValue(OPEN) && neighborState.getValue(COLLIDABLE)) {
                        this.openDoorFromAnyHalf(level, neighborPos, neighborState);
                    }
                }
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            return null; // No BlockEntity for the top half
        }
        return new ColoredDoorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }
}
