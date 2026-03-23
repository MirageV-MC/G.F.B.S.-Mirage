package org.mirage.gfbs.objects.blockEntity.ColoredDoor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import org.mirage.gfbs.Tools.Task;
import org.mirage.gfbs.ModSoundEvents;
import org.mirage.gfbs.objects.ModBlockEntities;
import org.mirage.gfbs.objects.blocks.classs.ColoredDoor.ColoredDoorBlock;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ColoredDoorBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final int AUTO_CLOSE_TICKS = 80;
    private static final int CLOSE_SOUND_DELAY_TICKS = 6;
    private static final int CLOSE_ANIMATION_LOCK_TICKS = 22;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Future<?> autoCloseTask = null;
    private Future<?> closeSoundTask = null;
    private Future<?> collisionEnableTask = null;
    
    private boolean lastOpen = false;

    public ColoredDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COLORED_DOOR.get(), pos, state);
        if (state.hasProperty(ColoredDoorBlock.OPEN)) {
            this.lastOpen = state.getValue(ColoredDoorBlock.OPEN);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <E extends GeoBlockEntity> PlayState predicate(AnimationState<E> event) {
        if (this.level == null) return PlayState.CONTINUE;

        BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof ColoredDoorBlock)) return PlayState.CONTINUE;
        if (state.getValue(ColoredDoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) return PlayState.CONTINUE;

        boolean isOpen = state.getValue(ColoredDoorBlock.OPEN);

        if (isOpen != lastOpen) {
            lastOpen = isOpen;
            if (isOpen) {
                event.getController().setAnimation(RawAnimation.begin().then("opendoor", Animation.LoopType.PLAY_ONCE).thenLoop("idle_open"));
            } else {
                event.getController().setAnimation(RawAnimation.begin().then("closedoor", Animation.LoopType.PLAY_ONCE).thenLoop("idle_close"));
            }
        } else if (event.getController().getCurrentAnimation() == null) {
            if (isOpen) {
                event.getController().setAnimation(RawAnimation.begin().thenLoop("idle_open"));
            } else {
                event.getController().setAnimation(RawAnimation.begin().thenLoop("idle_close"));
            }
        }

        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public void openDoor() {
        cancelTask(this.closeSoundTask);
        this.closeSoundTask = null;
        cancelTask(this.collisionEnableTask);
        this.collisionEnableTask = null;
        scheduleAutoClose();
        this.setChanged();
    }

    private long ticksToMillis(int ticks) {
        return ticks * 50L;
    }

    private void scheduleAutoClose() {
        cancelTask(this.autoCloseTask);
        this.autoCloseTask = null;
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        MinecraftServer server = serverLevel.getServer();
        BlockPos lowerPos = this.getBlockPos();
        this.autoCloseTask = Task.delay(() -> server.execute(() -> {
            if (this.isRemoved()) {
                return;
            }
            BlockState lowerState = serverLevel.getBlockState(lowerPos);
            if (!(lowerState.getBlock() instanceof ColoredDoorBlock) || !lowerState.getValue(ColoredDoorBlock.OPEN)) {
                return;
            }

            serverLevel.setBlock(lowerPos, lowerState.setValue(ColoredDoorBlock.OPEN, false).setValue(ColoredDoorBlock.COLLIDABLE, false), Block.UPDATE_ALL);
            BlockPos upperPos = lowerPos.above();
            BlockState upperState = serverLevel.getBlockState(upperPos);
            if (upperState.getBlock() instanceof ColoredDoorBlock && upperState.getValue(ColoredDoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) {
                serverLevel.setBlock(upperPos, upperState.setValue(ColoredDoorBlock.OPEN, false).setValue(ColoredDoorBlock.COLLIDABLE, false), Block.UPDATE_ALL);
            }

            scheduleCloseSoundDelay(server, serverLevel, lowerPos);
            scheduleCollisionEnableDelay(server, serverLevel, lowerPos);
        }), ticksToMillis(AUTO_CLOSE_TICKS), TimeUnit.MILLISECONDS);
    }

    private void scheduleCloseSoundDelay(MinecraftServer server, ServerLevel level, BlockPos pos) {
        cancelTask(this.closeSoundTask);
        this.closeSoundTask = Task.delay(() -> server.execute(() -> {
            if (this.isRemoved()) {
                return;
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof ColoredDoorBlock)) {
                return;
            }
            level.playSound(null, pos, ModSoundEvents.getSoundOrThrow("mobile.door_close"), SoundSource.BLOCKS, 1.0F, 1.0F);
        }), ticksToMillis(CLOSE_SOUND_DELAY_TICKS), TimeUnit.MILLISECONDS);
    }

    private void scheduleCollisionEnableDelay(MinecraftServer server, ServerLevel level, BlockPos pos) {
        cancelTask(this.collisionEnableTask);
        this.collisionEnableTask = Task.delay(() -> server.execute(() -> {
            if (this.isRemoved()) {
                return;
            }
            BlockState lowerState = level.getBlockState(pos);
            if (lowerState.getBlock() instanceof ColoredDoorBlock && !lowerState.getValue(ColoredDoorBlock.OPEN) && !lowerState.getValue(ColoredDoorBlock.COLLIDABLE)) {
                level.setBlock(pos, lowerState.setValue(ColoredDoorBlock.COLLIDABLE, true), Block.UPDATE_ALL);
            }

            BlockPos posUp = pos.above();
            BlockState upperState = level.getBlockState(posUp);
            if (upperState.getBlock() instanceof ColoredDoorBlock && upperState.getValue(ColoredDoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER && !upperState.getValue(ColoredDoorBlock.OPEN) && !upperState.getValue(ColoredDoorBlock.COLLIDABLE)) {
                level.setBlock(posUp, upperState.setValue(ColoredDoorBlock.COLLIDABLE, true), Block.UPDATE_ALL);
            }
        }), ticksToMillis(CLOSE_ANIMATION_LOCK_TICKS), TimeUnit.MILLISECONDS);
    }

    private void cancelTask(Future<?> task) {
        if (task != null) {
            task.cancel(false);
        }
    }

    private void cancelScheduledTasks() {
        cancelTask(this.autoCloseTask);
        this.autoCloseTask = null;
        cancelTask(this.closeSoundTask);
        this.closeSoundTask = null;
        cancelTask(this.collisionEnableTask);
        this.collisionEnableTask = null;
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag) {
        super.saveAdditional(tag);
    }

    @Override
    public void load(net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public void setRemoved() {
        cancelScheduledTasks();
        super.setRemoved();
    }
}
