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

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.mirage.Objects.ModBlockEntities;
import org.mirage.Objects.blocks.BlockRegistration;
import org.mirage.Objects.blocks.classs.GateBlock;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GateBlockEntity extends BlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private boolean logicalOpen = false;

    public GateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GATE.get(), pos, state);
        this.logicalOpen = state.getValue(GateBlock.OPEN);
    }

    public void setOpen(boolean open) {
        this.logicalOpen = open;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<>(
                        this,
                        "gate_controller",
                        0, // 默认 tick，别改
                        this::animationPredicate
                )
        );
    }

    private <E extends GeoBlockEntity> PlayState animationPredicate(AnimationState<E> state) {

        AnimationController<?> controller = state.getController();

        if (controller.getCurrentAnimation() != null) {
            String anim = controller.getCurrentAnimation().animation().name();

            if (anim.equals("animation.gate.open") ||
                    anim.equals("animation.gate.close")) {
                return PlayState.CONTINUE;
            }
        }

        if (logicalOpen) {
            controller.setAnimation(RawAnimation.begin().thenLoop("animation.gate.open_idle"));
        } else {
            controller.setAnimation(RawAnimation.begin().thenLoop("animation.gate.idle"));
        }

        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
