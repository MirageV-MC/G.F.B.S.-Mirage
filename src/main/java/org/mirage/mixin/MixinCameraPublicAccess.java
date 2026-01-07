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

import net.minecraft.client.Camera;
import org.mirage.Client.ClientShake.ICameraPublicAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public abstract class MixinCameraPublicAccess implements ICameraPublicAccess {

    @Invoker("setRotation")
    protected abstract void mirage$invokeSetRotation(float yaw, float pitch);

    @Invoker("setPosition")
    protected abstract void mirage$invokeSetPosition(double x, double y, double z);

    @Override
    public void mirage$setRotationPublic(float yaw, float pitch) {
        mirage$invokeSetRotation(yaw, pitch);
    }

    @Override
    public void mirage$setPositionPublic(double x, double y, double z) {
        mirage$invokeSetPosition(x, y, z);
    }
}
