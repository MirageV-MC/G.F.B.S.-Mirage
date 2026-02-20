package org.mirage.gfbs.Objects.blocks.classs;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.mirage.gfbs.Objects.blocks.Control.Gate.GateServerManager;
import org.mirage.gfbs.Objects.blocks.Control.Gate.GateTypes;
import org.mirage.gfbs.Objects.blocks.classs.Gate.GateBlock;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TartarusGateCollisionBlock extends Block {

    private static final VoxelShape COLLISION_SHAPE = Block.box(0, 0, 0, 16, 16, 16);
    private static final Set<BlockPos> NON_PLAYER_REMOVAL_POSITIONS = ConcurrentHashMap.newKeySet();
    private static final int COLLISION_RADIUS = 37;
    private static final int COLLISION_HEIGHT = 8;

    public TartarusGateCollisionBlock(Properties properties) {
        super(properties.noOcclusion());
    }

    public static void markNonPlayerRemoval(Collection<BlockPos> positions) {
        if (positions == null || positions.isEmpty()) return;
        for (BlockPos p : positions) {
            if (p != null) {
                NON_PLAYER_REMOVAL_POSITIONS.add(p.immutable());
            }
        }
    }

    public static void unmarkNonPlayerRemoval(Collection<BlockPos> positions) {
        if (positions == null || positions.isEmpty()) return;
        for (BlockPos p : positions) {
            if (p != null) {
                NON_PLAYER_REMOVAL_POSITIONS.remove(p);
            }
        }
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return COLLISION_SHAPE;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return COLLISION_SHAPE;
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                if (NON_PLAYER_REMOVAL_POSITIONS.contains(pos)) {
                    super.onRemove(state, level, pos, newState, isMoving);
                    return;
                }

                int r2 = COLLISION_RADIUS * COLLISION_RADIUS;

                for (BlockPos gatePos : GateServerManager.getGatesInLevel(level, GateTypes.TARTARUS_GATE)) {
                    int dx = gatePos.getX() - pos.getX();
                    int dz = gatePos.getZ() - pos.getZ();
                    int dy = pos.getY() - gatePos.getY();
                    if (dx * dx + dz * dz <= r2 && dy >= 0 && dy < COLLISION_HEIGHT) {
                        BlockState bs = level.getBlockState(gatePos);
                        if (bs.getBlock() instanceof GateBlock gateBlock) {
                            gateBlock.destroyFromCollision(level, gatePos);
                        }
                        break;
                    }
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        } else {
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
