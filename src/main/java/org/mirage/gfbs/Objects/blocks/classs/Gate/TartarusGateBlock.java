package org.mirage.gfbs.Objects.blocks.classs.Gate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.mirage.gfbs.Objects.blockEntity.Gate.GateBlockEntity;
import org.mirage.gfbs.Objects.blocks.Control.Gate.GateType;
import org.mirage.gfbs.Objects.blocks.BlockRegistration;
import org.mirage.gfbs.Objects.blocks.classs.TartarusGateCollisionBlock;
import org.mirage.gfbs.Tools.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TartarusGateBlock extends GateBlock {
    private static final VoxelShape CLOSED_SHAPE_AXIS_X = Block.box(7.0D, 0.0D, 0.0D, 9.0D, 16.0D, 16.0D);
    private static final VoxelShape CLOSED_SHAPE_AXIS_Z = Block.box(0.0D, 0.0D, 7.0D, 16.0D, 16.0D, 9.0D);
    private static final int COLLISION_RADIUS = 37;
    private static final int COLLISION_HEIGHT = 8;
    private static final long CLOSE_COLLISION_DELAY_MS = 12_300L;
    private static final int[] DISK_OFFSETS;
    private static final ConcurrentHashMap<String, Future<?>> PENDING_CLOSE_COLLISION = new ConcurrentHashMap<>();

    static {
        int r = COLLISION_RADIUS;
        int r2 = r * r;
        List<Integer> list = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (dx * dx + dz * dz > r2) continue;
                list.add(dx);
                list.add(dz);
            }
        }
        DISK_OFFSETS = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            DISK_OFFSETS[i] = list.get(i);
        }
    }

    public TartarusGateBlock(Properties properties, GateType gateType) {
        super(properties, null, gateType);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        Direction.Axis axis = state.hasProperty(AXIS) ? state.getValue(AXIS) : Direction.Axis.Z;
        return axis == Direction.Axis.X ? CLOSED_SHAPE_AXIS_X : CLOSED_SHAPE_AXIS_Z;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide) return;
        if (oldState.is(state.getBlock())) return;
        cancelPendingCloseCollision(level, pos);
        if (state.hasProperty(OPEN) && !state.getValue(OPEN)) {
            placeDiskCollision(level, pos);
        }
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                cancelPendingCloseCollision(level, pos);
                removeDiskCollision(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void applyOpenStateDirect(Level level, BlockPos gatePos, boolean open) {
        if (level.isClientSide) return;

        BlockState state = level.getBlockState(gatePos);
        if (!(state.getBlock() instanceof TartarusGateBlock)) return;

        cancelPendingCloseCollision(level, gatePos);

        if (open) {
            removeDiskCollision(level, gatePos);
        } else {
            if (state.hasProperty(OPEN) && !state.getValue(OPEN)) {
                placeDiskCollision(level, gatePos);
            } else {
                scheduleCloseCollision(level, gatePos);
            }
        }

        if (state.hasProperty(OPEN) && state.getValue(OPEN) != open) {
            level.setBlock(gatePos, state.setValue(OPEN, open), Block.UPDATE_ALL);
        }

        BlockEntity be = level.getBlockEntity(gatePos);
        if (be instanceof GateBlockEntity gateBe) {
            gateBe.setLogicalOpenNoWorld(open);
        }
    }

    @Override
    public void destroyFromCollision(Level level, BlockPos gatePos) {
        if (level.isClientSide) return;
        if (isBusy) return;
        cancelPendingCloseCollision(level, gatePos);
        removeDiskCollision(level, gatePos);
        level.removeBlock(gatePos, false);
    }

    private static void placeDiskCollision(Level level, BlockPos gatePos) {
        var collisionBlock = BlockRegistration.TARTARUS_GATE_COLLISION.get();
        int baseY = gatePos.getY();
        int baseX = gatePos.getX();
        int baseZ = gatePos.getZ();

        for (int i = 0; i < DISK_OFFSETS.length; i += 2) {
            int dx = DISK_OFFSETS[i];
            int dz = DISK_OFFSETS[i + 1];

            for (int dy = 0; dy < COLLISION_HEIGHT; dy++) {
                BlockPos p = new BlockPos(baseX + dx, baseY + dy, baseZ + dz);
                if (!level.hasChunkAt(p)) continue;

                BlockState existing = level.getBlockState(p);
                if (existing.isAir() || existing.canBeReplaced()) {
                    level.setBlock(p, collisionBlock.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static void removeDiskCollision(Level level, BlockPos gatePos) {
        var collisionBlock = BlockRegistration.TARTARUS_GATE_COLLISION.get();
        int baseY = gatePos.getY();
        int baseX = gatePos.getX();
        int baseZ = gatePos.getZ();

        List<BlockPos> toRemove = new ArrayList<>();

        for (int i = 0; i < DISK_OFFSETS.length; i += 2) {
            int dx = DISK_OFFSETS[i];
            int dz = DISK_OFFSETS[i + 1];

            for (int dy = 0; dy < COLLISION_HEIGHT; dy++) {
                BlockPos p = new BlockPos(baseX + dx, baseY + dy, baseZ + dz);
                if (!level.hasChunkAt(p)) continue;
                BlockState existing = level.getBlockState(p);
                if (existing.is(collisionBlock)) {
                    toRemove.add(p.immutable());
                }
            }
        }

        TartarusGateCollisionBlock.markNonPlayerRemoval(toRemove);
        try {
            for (BlockPos p : toRemove) {
                level.removeBlock(p, false);
            }
        } finally {
            TartarusGateCollisionBlock.unmarkNonPlayerRemoval(toRemove);
        }
    }

    private static String pendingKey(Level level, BlockPos gatePos) {
        return level.dimension().location() + "|" + gatePos.asLong();
    }

    private static void cancelPendingCloseCollision(Level level, BlockPos gatePos) {
        Future<?> f = PENDING_CLOSE_COLLISION.remove(pendingKey(level, gatePos));
        if (f != null) {
            f.cancel(false);
        }
    }

    private static void scheduleCloseCollision(Level level, BlockPos gatePos) {
        String key = pendingKey(level, gatePos);
        BlockPos frozenPos = gatePos.immutable();

        Future<?> f = Task.delay(() -> {
            if (!(level.getBlockState(frozenPos).getBlock() instanceof TartarusGateBlock)) return;
            BlockState current = level.getBlockState(frozenPos);
            if (!current.hasProperty(OPEN) || current.getValue(OPEN)) return;
            placeDiskCollision(level, frozenPos);
        }, CLOSE_COLLISION_DELAY_MS, TimeUnit.MILLISECONDS);

        Future<?> prev = PENDING_CLOSE_COLLISION.put(key, f);
        if (prev != null) {
            prev.cancel(false);
        }
    }
}
