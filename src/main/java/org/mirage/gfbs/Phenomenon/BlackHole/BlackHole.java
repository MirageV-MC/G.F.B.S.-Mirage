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

package org.mirage.gfbs.Phenomenon.BlackHole;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class BlackHole {
    private final String id;
    private double targetEventHorizonRadius;
    private double currentEventHorizonRadius;
    private final double lensingFactor;
    private Vec3 position;
    private double accretionDiskOpacity = 0.0;
    private double targetAccretionDiskOpacity = 1.0;
    private boolean isBlockBased = false;

    public enum AnimationState {
        SPAWNING,
        ACTIVE,
        DESPAWNING,
        REMOVED
    }

    private AnimationState animationState = AnimationState.SPAWNING;
    private long animationStartTime = System.currentTimeMillis();
    private static final long EVENT_HORIZON_ANIM_DURATION = 1000;
    private static final long ACCRETION_DISK_ANIM_DURATION = 5000;
    private static final long DESPAWN_ANIM_DURATION = 1000;

    private double despawnDiskInnerExpansion = 1.0;
    private double despawnDiskOuterExpansion = 1.0;

    public BlackHole(String id, double eventHorizonRadius, double lensingFactor, Vec3 position) {
        this.id = id;
        this.targetEventHorizonRadius = eventHorizonRadius;
        this.currentEventHorizonRadius = 0.0;
        this.lensingFactor = Math.max(1.0, lensingFactor);
        this.position = position;
        this.animationStartTime = System.currentTimeMillis();
    }

    public BlackHole(double eventHorizonRadius, double lensingFactor, Vec3 position) {
        this("black_hole_" + System.currentTimeMillis(), eventHorizonRadius, lensingFactor, position);
    }

    public void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - animationStartTime;

        switch (animationState) {
            case SPAWNING:
                if (elapsed < EVENT_HORIZON_ANIM_DURATION) {
                    double t = (double) elapsed / EVENT_HORIZON_ANIM_DURATION;
                    currentEventHorizonRadius = targetEventHorizonRadius * easeInOutCubic(t);
                    accretionDiskOpacity = 0.0;
                } else if (elapsed < EVENT_HORIZON_ANIM_DURATION + ACCRETION_DISK_ANIM_DURATION) {
                    currentEventHorizonRadius = targetEventHorizonRadius;
                    long diskElapsed = elapsed - EVENT_HORIZON_ANIM_DURATION;
                    double t = (double) diskElapsed / ACCRETION_DISK_ANIM_DURATION;
                    accretionDiskOpacity = easeInOutCubic(t);
                } else {
                    currentEventHorizonRadius = targetEventHorizonRadius;
                    accretionDiskOpacity = 1.0;
                    animationState = AnimationState.ACTIVE;
                }
                break;

            case ACTIVE:
                currentEventHorizonRadius = targetEventHorizonRadius;
                accretionDiskOpacity = targetAccretionDiskOpacity;
                despawnDiskInnerExpansion = 1.0;
                despawnDiskOuterExpansion = 1.0;
                break;

            case DESPAWNING:
                if (elapsed < DESPAWN_ANIM_DURATION) {
                    double t = (double) elapsed / DESPAWN_ANIM_DURATION;
                    double eased = easeInOutCubic(t);
                    currentEventHorizonRadius = targetEventHorizonRadius * (1.0 - eased);
                    accretionDiskOpacity = 1.0 - eased;
                    despawnDiskInnerExpansion = 1.0 + eased * 1.5;
                    despawnDiskOuterExpansion = 1.0 + eased * 4.0;
                } else {
                    currentEventHorizonRadius = 0.0;
                    accretionDiskOpacity = 0.0;
                    despawnDiskInnerExpansion = 2.5;
                    despawnDiskOuterExpansion = 5.0;
                    animationState = AnimationState.REMOVED;
                }
                break;

            case REMOVED:
                break;
        }
    }

    public void startDespawnAnimation() {
        if (animationState == AnimationState.ACTIVE || animationState == AnimationState.SPAWNING) {
            animationState = AnimationState.DESPAWNING;
            animationStartTime = System.currentTimeMillis();
        }
    }

    public boolean isReadyForRemoval() {
        return animationState == AnimationState.REMOVED;
    }

    private double easeInOutCubic(double t) {
        return t < 0.5
            ? 4.0 * t * t * t
            : 1.0 - Math.pow(-2.0 * t + 2.0, 3.0) / 2.0;
    }

    public void updatePosition(Player player) {
        this.position = player.position().add(0, 5, 0);
    }

    public void applyGravity(Entity entity) {
        if (animationState == AnimationState.REMOVED) return;

        Vec3 toBlackHole = position.subtract(entity.position());

        double distanceSqr = toBlackHole.lengthSqr();
        double influenceRadius = currentEventHorizonRadius * 10;
        double distance = Math.sqrt(distanceSqr);

        if (distanceSqr < influenceRadius * influenceRadius) {
            double forceMagnitude = 0.1 * currentEventHorizonRadius / (distance * distance + 0.01);
            Vec3 force = toBlackHole.normalize().scale(forceMagnitude);
            entity.setDeltaMovement(entity.getDeltaMovement().add(force));
        }
    }

    public double getRenderRadius(float partialTicks) {
        return targetEventHorizonRadius;
    }

    public double getEventHorizonScale() {
        return currentEventHorizonRadius / Math.max(targetEventHorizonRadius, 0.001);
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

    public String getId() {
        return id;
    }

    public double getEventHorizonRadius() {
        return targetEventHorizonRadius;
    }

    public void setEventHorizonRadius(double radius) {
        this.targetEventHorizonRadius = radius;
        if (animationState == AnimationState.ACTIVE) {
            this.currentEventHorizonRadius = radius;
        }
    }

    public double getAccretionDiskOpacity() {
        return accretionDiskOpacity;
    }

    public void setAccretionDiskOpacity(double opacity) {
        this.targetAccretionDiskOpacity = Mth.clamp(opacity, 0.0, 1.0);
        if (animationState == AnimationState.ACTIVE) {
            this.accretionDiskOpacity = this.targetAccretionDiskOpacity;
        }
    }

    public double getDespawnDiskInnerExpansion() {
        return despawnDiskInnerExpansion;
    }

    public double getDespawnDiskOuterExpansion() {
        return despawnDiskOuterExpansion;
    }

    public AnimationState getAnimationState() {
        return animationState;
    }

    public boolean isBlockBased() {
        return isBlockBased;
    }

    public void setBlockBased(boolean blockBased) {
        isBlockBased = blockBased;
    }

    @Override
    public String toString() {
        return id;
    }
}
