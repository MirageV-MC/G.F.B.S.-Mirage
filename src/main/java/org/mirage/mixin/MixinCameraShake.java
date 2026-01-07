package org.mirage.mixin;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC
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

import org.joml.Quaternionf;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.mirage.Client.ClientShake.ShakeQsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCameraShake {

    @Invoker("setRotation")
    protected abstract void mirage$invokeSetRotation(float yaw, float pitch);

    @Invoker("setPosition")
    protected abstract void mirage$invokeSetPosition(double x, double y, double z);

    @Inject(
            method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
            at = @At("RETURN")
    )
    private void mirage$afterSetup(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        ShakeQsClient.ShakeFrame f = ShakeQsClient.computeShakeFrame();
        if (f == null) return;

        Camera cam = (Camera)(Object)this;

        float newYaw = cam.getYRot() + f.ryDeg;
        float newPitch = cam.getXRot() + f.rxDeg;
        mirage$invokeSetRotation(newYaw, newPitch);

        Vec3 p = cam.getPosition();
        mirage$invokeSetPosition(p.x + f.dx, p.y + f.dy, p.z + f.dz);

        Quaternionf q = cam.rotation();
        q.rotateZ(f.rzRad);
    }

//    @Inject(method = "tick", at = @At("TAIL"))
//    private void applyCameraShake(CallbackInfo ci) {
//        ShakeQsClient.applyShake((Camera)(Object)this, 0f);
//    }
}
