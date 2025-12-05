package org.mirage.Objects.blockEntity;

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

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.mirage.Mirage_gfbs;
import org.mirage.ModSoundEvents;
import org.mirage.Objects.ModBlockEntities;
import org.mirage.Objects.blocks.BlockRegistration;
import org.mirage.Objects.blocks.Control.GateServerManager;
import org.mirage.Objects.blocks.classs.GateBlock;
import org.mirage.api.GateClientAPI;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

public class GateBlockEntity extends BlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @OnlyIn(Dist.CLIENT)
    private static final List<GateBlockEntity> CLIENT_GATES = new ArrayList<>();

    private boolean logicalOpen = false;
    private boolean lastLogicalOpen = false;

    public GateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GATE.get(), pos, state);
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

        BlockState state = this.level.getBlockState(this.worldPosition);
        if (state.getBlock() instanceof GateBlock gateBlock) {
            gateBlock.applyOpenState(this.level, this.getBlockPos(), open);
        }

        this.setChanged();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null) {
            return;
        }

        if (level.isClientSide) {
            CLIENT_GATES.add(this);
            this.logicalOpen = GateClientAPI.GLOBAL_GATE_STATE;
            this.lastLogicalOpen = this.logicalOpen;
        } else {
            GateServerManager.registerGate(level, worldPosition);
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

        if (level.isClientSide) {
            CLIENT_GATES.remove(this);
        } else {
            GateServerManager.unregisterGate(level, worldPosition);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static List<GateBlockEntity> getClientGates() {
        return CLIENT_GATES;
    }

    @Override
    public AABB getRenderBoundingBox() {
        BlockPos pos = this.worldPosition;
        return new AABB(
                pos.getX() - 3, pos.getY(),     pos.getZ() - 3,
                pos.getX() + 3, pos.getY() + 4, pos.getZ() + 3
        );
    }

    public Direction.Axis getAxis() {
        BlockState state = getBlockState();
        if (state.hasProperty(GateBlock.AXIS)) {
            return state.getValue(GateBlock.AXIS);
        }
        return Direction.Axis.Z;
    }
}
