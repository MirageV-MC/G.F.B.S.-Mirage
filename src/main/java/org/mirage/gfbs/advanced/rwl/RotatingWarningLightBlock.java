package org.mirage.gfbs.advanced.rwl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.mirage.gfbs.Objects.ModBlockEntities;

public class RotatingWarningLightBlock extends Block implements EntityBlock {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;

    public RotatingWarningLightBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(POWERED, false)
                .setValue(FACE, AttachFace.FLOOR)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, FACE, FACING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        Player player = context.getPlayer();

        AttachFace attachFace = toAttachFace(clickedFace);

        Direction facing;
        if (attachFace == AttachFace.WALL) {
            facing = clickedFace.getAxis().isHorizontal() ? clickedFace : Direction.NORTH;
        } else {
            facing = player != null ? player.getDirection().getOpposite() : Direction.NORTH;
        }

        boolean powered = false;
        if (level instanceof ServerLevel sl) {
            powered = RWLLevelState.get(sl).isEnabled();
        }

        return this.defaultBlockState()
                .setValue(FACE, attachFace)
                .setValue(FACING, facing)
                .setValue(POWERED, powered);
    }

    private static AttachFace toAttachFace(Direction clickedFace) {
        return switch (clickedFace) {
            case UP -> AttachFace.FLOOR;
            case DOWN -> AttachFace.CEILING;
            default -> AttachFace.WALL;
        };
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RotatingWarningLightBlockEntity(
                ModBlockEntities.RWL_ENTITY.get(),
                pos,
                state
        );
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.RWL_ENTITY.get(), RotatingWarningLightBlockEntity::tick);
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> BlockEntityTicker<T> createTickerHelper(
            BlockEntityType<T> type,
            BlockEntityType<RotatingWarningLightBlockEntity> rwlType,
            BlockEntityTicker<RotatingWarningLightBlockEntity> ticker
    ) {
        return type == rwlType ? (BlockEntityTicker<T>) ticker : null;
    }
}
