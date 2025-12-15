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

package org.mirage.Phenomenon.BlackHole;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class BlackHole {
    private final double eventHorizonRadius;
    private final double lensingFactor;
    private Vec3 position;

    public BlackHole(double eventHorizonRadius, double lensingFactor, Vec3 position) {
        this.eventHorizonRadius = eventHorizonRadius;
        this.lensingFactor = Math.max(1.0, lensingFactor); // 确保最小为1
        this.position = position;
    }

    public void updatePosition(Player player) {
        this.position = player.position().add(0, 5, 0);
    }

    public void applyGravity(Entity entity) {
        Vec3 toBlackHole = position.subtract(entity.position());

        double distanceSqr = toBlackHole.lengthSqr();
        double influenceRadius = eventHorizonRadius * 10;
        double distance = Math.sqrt(distanceSqr);

        if (distanceSqr < influenceRadius * influenceRadius) {
            double forceMagnitude = 0.1 * eventHorizonRadius / (distance * distance + 0.01);
            Vec3 force = toBlackHole.normalize().scale(forceMagnitude);
            entity.setDeltaMovement(entity.getDeltaMovement().add(force));
        }
    }

    public double getRenderRadius(float partialTicks) {
        return eventHorizonRadius;
    }

    public double getLensingFactor() {
        return lensingFactor;
    }

    public Vec3 getPosition() {
        return position;
    }

    public void setPosition(Vec3 position) {
        this.position = position;
    }
}