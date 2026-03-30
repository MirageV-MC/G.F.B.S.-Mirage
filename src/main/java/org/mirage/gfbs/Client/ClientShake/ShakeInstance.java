package org.mirage.gfbs.Client.ClientShake;

import net.minecraft.world.phys.Vec3;

import java.util.Random;

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

public class ShakeInstance {
    public final float speed;
    public final float maxAmplitude;
    public final int duration;
    public final int riseTime;
    public final int fallTime;
    public final long startTime;

    private float currentAmplitude = 0;
    private Vec3 currentShakeDirection = Vec3.ZERO;
    private Vec3 targetShakeDirection = Vec3.ZERO;
    private long lastDirectionChangeTime = 0;

    private Vec3 posOffset = Vec3.ZERO;
    private Vec3 posVelocity = Vec3.ZERO;

    private Vec3 rotImpulse = Vec3.ZERO;
    private Vec3 rotImpulseVel = Vec3.ZERO;
    private Vec3 targetKick = Vec3.ZERO;
    private double kickSmoothFactor = 0.0;

    private float targetImpulse = 0f;
    private float impulseSmoothFactor = 0f;

    private final Random random = new Random();

    private volatile boolean finished = false;
    private volatile Vec3 cachedShakeOffset = Vec3.ZERO;
    private volatile Vec3 cachedRotationOffset = Vec3.ZERO;
    private volatile Vec3 cachedPositionOffset = Vec3.ZERO;

    public ShakeInstance(float speed, float maxAmplitude, int duration, int riseTime, int fallTime) {
        this.speed = speed;
        this.maxAmplitude = maxAmplitude;
        this.duration = duration;
        this.riseTime = riseTime;
        this.fallTime = fallTime;
        this.startTime = System.currentTimeMillis();
    }

    public boolean isFinished() {
        return finished;
    }

    public void update() {
        long now = System.currentTimeMillis();
        long elapsed = now - startTime;

        if (elapsed > duration) {
            finished = true;
            cachedShakeOffset = Vec3.ZERO;
            cachedRotationOffset = Vec3.ZERO;
            cachedPositionOffset = Vec3.ZERO;
            return;
        }

        currentAmplitude = calculateCurrentAmplitude(elapsed);

        if (currentAmplitude <= 0) {
            cachedShakeOffset = Vec3.ZERO;
            cachedRotationOffset = Vec3.ZERO;
            cachedPositionOffset = Vec3.ZERO;
            return;
        }

        Vec3 shakeOffset = calculateShakeOffsetInternal(elapsed);
        cachedShakeOffset = shakeOffset;

        updateDirectionInternal(now);

        cachedRotationOffset = calculateRotationOffsetInternal(shakeOffset, elapsed);
        cachedPositionOffset = calculateCameraPositionOffsetInternal(elapsed);
    }

    public Vec3 getShakeOffset() {
        return cachedShakeOffset;
    }

    public Vec3 getRotationOffset() {
        return cachedRotationOffset;
    }

    public Vec3 getPositionOffset() {
        return cachedPositionOffset;
    }

    public Vec3 getCurrentShakeDirection() {
        return currentShakeDirection;
    }

    private Vec3 calculateShakeOffsetInternal(long elapsed) {
        double timeFactor = elapsed * speed / 1000.0;

        double xOffset = Math.sin(timeFactor) * currentAmplitude;
        double yOffset = Math.sin(timeFactor * 1.17 + 0.5) * currentAmplitude;
        double zOffset = Math.sin(timeFactor * 0.83 + 1.2) * currentAmplitude;

        double harmonicFactor = 2.3;
        xOffset += Math.sin(timeFactor * harmonicFactor) * currentAmplitude * 0.3;
        yOffset += Math.sin(timeFactor * harmonicFactor * 1.17 + 1.7) * currentAmplitude * 0.3;
        zOffset += Math.sin(timeFactor * harmonicFactor * 0.83 + 2.9) * currentAmplitude * 0.3;

        double micro = currentAmplitude * 0.18;
        xOffset += ClientShakeHandler.improvedNoise(timeFactor * 12.0 + 10.0) * micro;
        yOffset += ClientShakeHandler.improvedNoise(timeFactor * 14.0 + 20.0) * micro;
        zOffset += ClientShakeHandler.improvedNoise(timeFactor * 10.0 + 30.0) * micro;

        zOffset += ClientShakeHandler.improvedNoise(timeFactor * 6.0 + 200.0) * (currentAmplitude * 0.35);

        return new Vec3(xOffset, yOffset, zOffset);
    }

    private Vec3 calculateCameraPositionOffsetInternal(long elapsed) {
        float amp = currentAmplitude;

        if (amp <= 0) return Vec3.ZERO;

        double t = elapsed * speed / 1000.0;
        double intensity = Math.min(1.0, amp / Math.max(1e-6f, maxAmplitude));

        double base = amp * (0.035 + 0.03 * intensity);
        double side = Math.sin(t * 1.9) * base * 0.45 + ClientShakeHandler.improvedNoise(t * 8.0 + 7.0) * base * 0.55;
        double up = Math.sin(t * 2.2 + 1.3) * base * 0.35 + ClientShakeHandler.improvedNoise(t * 9.0 + 17.0) * base * 0.65;

        double fore = ClientShakeHandler.improvedNoise(t * 5.0 + 70.0) * base * 1.25;

        Vec3 target = new Vec3(side, up, fore);
        double stiffness = 0.22 + 0.10 * intensity;
        double damping = 0.80;

        Vec3 diff = target.subtract(posOffset);
        posVelocity = posVelocity.scale(damping).add(diff.scale(stiffness));
        posOffset = posOffset.add(posVelocity);

        double limit = base * 3.2;
        double len = posOffset.length();
        if (len > limit && len > 1e-6) {
            posOffset = posOffset.scale(limit / len);
        }

        return posOffset;
    }

    private void updateDirectionInternal(long now) {
        long DIRECTION_CHANGE_INTERVAL = 80;

        if (now - lastDirectionChangeTime > DIRECTION_CHANGE_INTERVAL) {
            double changeIntensity = 0.5 + 0.5 * (currentAmplitude / maxAmplitude);
            targetShakeDirection = targetShakeDirection.scale(1.0 - changeIntensity)
                    .add(ClientShakeHandler.generateRandomDirection(random).scale(changeIntensity))
                    .normalize();
            lastDirectionChangeTime = now;
        }

        double DIRECTION_SMOOTHING_FACTOR = 0.12;
        double smoothFactor = DIRECTION_SMOOTHING_FACTOR * (1.0 + 0.5 * (currentAmplitude / maxAmplitude));
        Vec3 directionDiff = targetShakeDirection.subtract(currentShakeDirection);
        currentShakeDirection = currentShakeDirection.add(directionDiff.scale(smoothFactor)).normalize();
    }

    private Vec3 calculateRotationOffsetInternal(Vec3 shakeOffset, long elapsed) {
        double amp = shakeOffset.length();
        double intensity = amp / Math.max(1e-6f, maxAmplitude);

        double nonLinearity = 1.0 + 0.75 * intensity * intensity;
        double dampingFactor = 0.72 + 0.28 * Math.exp(-intensity * 3.2);

        Vec3 baseRot = currentShakeDirection.scale(amp);

        double t = elapsed * 0.001;
        Vec3 microRot = new Vec3(
                ClientShakeHandler.improvedNoise(t * 28.0 + 11.0),
                ClientShakeHandler.improvedNoise(t * 31.0 + 23.0),
                ClientShakeHandler.improvedNoise(t * 26.0 + 37.0)
        ).scale(amp * 0.22);

        if (random.nextFloat() < 0.0035f * intensity) {
            Vec3 newKick = new Vec3(
                    (random.nextFloat() * 2f - 1f),
                    (random.nextFloat() * 2f - 1f) * 0.65,
                    (random.nextFloat() * 2f - 1f) * 0.35
            ).scale(amp * (0.6 + 0.9 * intensity));
            targetKick = newKick;
            kickSmoothFactor = 0.0;
        }

        if (kickSmoothFactor < 1.0) {
            kickSmoothFactor = Math.min(1.0, kickSmoothFactor + 0.08);
            double ease = kickSmoothFactor * kickSmoothFactor * (3.0 - 2.0 * kickSmoothFactor);
            Vec3 smoothedKick = targetKick.scale(ease);
            rotImpulseVel = rotImpulseVel.add(smoothedKick.scale(0.15));
        }

        double springK = 0.18 + 0.10 * intensity;
        double springD = 0.78;
        rotImpulseVel = rotImpulseVel.scale(springD).add(rotImpulse.scale(-springK));
        rotImpulse = rotImpulse.add(rotImpulseVel);

        Vec3 inertial = new Vec3(
                posVelocity.x * 12.0,
                posVelocity.z * 18.0,
                -posVelocity.x * 20.0
        );

        Vec3 rot = baseRot.add(microRot).add(rotImpulse).add(inertial);

        double yaw = rot.x * 12.0 * nonLinearity * dampingFactor;
        double pitch = rot.y * 13.5 * nonLinearity * dampingFactor;
        double roll = rot.z * 12.2 * nonLinearity * dampingFactor;

        double facingBias = (0.6 + 0.4 * intensity) * amp;
        yaw += ClientShakeHandler.improvedNoise(t * 6.0 + 300.0) * facingBias * 2.4;
        pitch += ClientShakeHandler.improvedNoise(t * 6.5 + 500.0) * facingBias * 1.6;
        roll += ClientShakeHandler.improvedNoise(t * 6.2 + 700.0) * facingBias * 1.8;

        return new Vec3(yaw, pitch, roll);
    }

    private float calculateCurrentAmplitude(long elapsed) {
        if (elapsed > duration) return 0;

        float baseAmplitude;
        if (elapsed < riseTime) {
            float progress = (float) elapsed / riseTime;
            baseAmplitude = maxAmplitude * (float) (1.0 - Math.cos(progress * Math.PI * 0.5));
        } else if (elapsed < duration - fallTime) {
            baseAmplitude = maxAmplitude;
        } else {
            int fallStart = duration - fallTime;
            float fallProgress = (float) (elapsed - fallStart) / fallTime;
            baseAmplitude = maxAmplitude * (float) Math.exp(-4.5 * fallProgress) *
                    (float) (0.6 + 0.4 * Math.cos(fallProgress * Math.PI));
        }

        if (baseAmplitude > 0) {
            float noiseAmplitude = 0.1f * baseAmplitude;

            float lowFreqNoise = (float) ClientShakeHandler.improvedNoise(elapsed * 0.002) * noiseAmplitude;
            float midFreqNoise = (float) ClientShakeHandler.improvedNoise(elapsed * 0.015 + 100) * noiseAmplitude * 0.7f;
            float highFreqNoise = (float) ClientShakeHandler.improvedNoise(elapsed * 0.06 + 200) * noiseAmplitude * 0.4f;

            if (random.nextFloat() < 0.005f * (baseAmplitude / maxAmplitude)) {
                targetImpulse = (random.nextFloat() * 2.0f - 1.0f) * noiseAmplitude * 0.8f;
                impulseSmoothFactor = 0f;
            }

            if (impulseSmoothFactor < 1.0f) {
                impulseSmoothFactor = Math.min(1.0f, impulseSmoothFactor + 0.06f);
                float ease = impulseSmoothFactor * impulseSmoothFactor * (3.0f - 2.0f * impulseSmoothFactor);
                float smoothedImpulse = targetImpulse * ease;
                lowFreqNoise += smoothedImpulse;
            }

            return Math.max(0, baseAmplitude + lowFreqNoise + midFreqNoise + highFreqNoise);
        }

        return baseAmplitude;
    }
}
