package org.mirage.gfbs.Objects.blockEntity.Gate;

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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.mirage.gfbs.ModSoundEvents;
import org.mirage.gfbs.Objects.ModBlockEntities;
import org.mirage.gfbs.Objects.blocks.BlockRegistration;
import org.mirage.gfbs.Objects.blocks.Control.Gate.GateServerManager;
import org.mirage.gfbs.Objects.blocks.Control.Gate.GateType;
import org.mirage.gfbs.Objects.blocks.Control.Gate.GateTypes;
import org.mirage.gfbs.Objects.blocks.classs.Gate.GateBlock;
import org.mirage.gfbs.Utils.SyncField.SyncField;
import org.mirage.gfbs.Utils.SyncField.SyncManager;
import org.mirage.gfbs.api.GateClientAPI;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GateBlockEntity extends BlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @SyncField
    private static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.CopyOnWriteArrayList<GateBlockEntity>> CLIENT_GATES_BY_TYPE = new java.util.concurrent.ConcurrentHashMap<>();
    @SyncField
    private boolean logicalOpen = false;

    private boolean lastLogicalOpen = false;

    public GateBlockEntity(BlockPos pos, BlockState state) {
        super(resolveType(state), pos, state);
        SyncManager.registerBlockEntity(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<>(
                        this,
                        "gate_controller",
                        0,
                        this::animationPredicate
                )
        );
    }

    public void setLogicalOpen(boolean open) {
        this.logicalOpen = open;
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.level.getBlockState(this.worldPosition);
            if (state.getBlock() instanceof GateBlock gateBlock) {
                gateBlock.applyOpenStateDirect(this.level, this.getBlockPos(), open);
            }
        }
        this.setChanged();
    }

    public void setLogicalOpenNoWorld(boolean open) {
        this.logicalOpen = open;
        this.setChanged();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null) {
            return;
        }

        if (level.isClientSide) {
            String typeId = getGateType().id();
            CLIENT_GATES_BY_TYPE.computeIfAbsent(typeId, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(this);
            this.logicalOpen = GateClientAPI.getGlobalState(getGateType());
            this.lastLogicalOpen = this.logicalOpen;
        } else {
            BlockState st = this.getBlockState();
            if (st.hasProperty(GateBlock.OPEN)) {
                this.logicalOpen = st.getValue(GateBlock.OPEN);
            }
            GateServerManager.registerGate(level, getGateType(), worldPosition);
        }
    }

    private <E extends GeoBlockEntity> PlayState animationPredicate(AnimationState<E> state) {
        AnimationController<?> controller = state.getController();

        if (logicalOpen != lastLogicalOpen) {
            lastLogicalOpen = logicalOpen;

            if (logicalOpen) {
                controller.setAnimation(
                        RawAnimation.begin()
                                .then("animation.gate.open", Animation.LoopType.PLAY_ONCE)
                                .thenLoop("animation.gate.open_idle")
                );
            } else {
                controller.setAnimation(
                        RawAnimation.begin()
                                .then("animation.gate.close", Animation.LoopType.PLAY_ONCE)
                                .thenLoop("animation.gate.idle")
                );
            }

            if (level.isClientSide){
                level.playLocalSound(worldPosition, ModSoundEvents.getSoundOrNull("surroundings.big_gate_reverb"), SoundSource.BLOCKS, 1.0F, 1.0F, false);
            }

            return PlayState.CONTINUE;
        }

        if (controller.getCurrentAnimation() == null) {
            if (logicalOpen) {
                controller.setAnimation(RawAnimation.begin().thenLoop("animation.gate.open_idle"));
            } else {
                controller.setAnimation(RawAnimation.begin().thenLoop("animation.gate.idle"));
            }
        }

        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public boolean isLogicalOpen() {
        return this.logicalOpen;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level == null) {
            return;
        }

        SyncManager.unregisterBlockEntity(this);

        if (level.isClientSide) {
            String typeId = getGateType().id();
            java.util.concurrent.CopyOnWriteArrayList<GateBlockEntity> list = CLIENT_GATES_BY_TYPE.get(typeId);
            if (list != null) {
                list.remove(this);
                if (list.isEmpty()) {
                    CLIENT_GATES_BY_TYPE.remove(typeId);
                }
            }
        } else {
            GateServerManager.unregisterGate(level, getGateType(), worldPosition);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static java.util.List<GateBlockEntity> getClientGates() {
        return getClientGates(GateTypes.STANDARD);
    }

    @OnlyIn(Dist.CLIENT)
    public static java.util.List<GateBlockEntity> getClientGates(GateType type) {
        java.util.concurrent.CopyOnWriteArrayList<GateBlockEntity> list = CLIENT_GATES_BY_TYPE.get(type.id());
        return list == null ? java.util.Collections.emptyList() : list;
    }
    @Override
    public AABB getRenderBoundingBox() {
        final double R = 3.0E7;

        BlockPos pos = this.worldPosition;
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        return new AABB(
                cx - R, cy - R, cz - R,
                cx + R, cy + R, cz + R
        );
    }

    public GateType getGateType() {
        if (this.level != null) {
            BlockState st = this.getBlockState();
            if (st.getBlock() instanceof GateBlock gateBlock) {
                GateType t = gateBlock.getGateType();
                return t == null ? GateTypes.STANDARD : t;
            }
        }
        return GateTypes.STANDARD;
    }

    public Direction.Axis getAxis() {
        BlockState state = getBlockState();
        if (state.hasProperty(GateBlock.AXIS)) {
            return state.getValue(GateBlock.AXIS);
        }
        return Direction.Axis.Z;
    }

    private static BlockEntityType<?> resolveType(BlockState state) {
        if (state.is(BlockRegistration.CHECK_POINT_GATE.get())) {
            return ModBlockEntities.CHECK_POINT_GATE.get();
        }
        return ModBlockEntities.GATE.get();
    }

    public void refreshAnimationState() {
        this.lastLogicalOpen = !this.logicalOpen;
    }
}
