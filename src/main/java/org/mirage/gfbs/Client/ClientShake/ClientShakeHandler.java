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

package org.mirage.gfbs.Client.ClientShake;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;
import java.util.Random;

@Mod.EventBusSubscriber(modid = "mirage_gfbs", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientShakeHandler {
    public static float currentAmplitude = 0;
    public static long startTime = 0;
    public static float speed;
    public static float maxAmplitude;
    public static int duration;
    public static int riseTime;
    public static int fallTime;

    private static final Random random = new Random();

    private static Vec3 currentShakeDirection = Vec3.ZERO;
    private static Vec3 targetShakeDirection = Vec3.ZERO;
    private static long lastDirectionChangeTime = 0;
    private static final long DIRECTION_CHANGE_INTERVAL = 80;
    private static final double DIRECTION_SMOOTHING_FACTOR = 0.12;

    private static Vec3 posOffset = Vec3.ZERO;
    private static Vec3 posVelocity = Vec3.ZERO;

    private static Vec3 rotImpulse = Vec3.ZERO;
    private static Vec3 rotImpulseVel = Vec3.ZERO;

    private static double lastNoiseValue = 0;
    private static double noiseVelocity = 0;

    // ===== Public =====

    public static void resetShake() {
        startTime = 0;
        currentAmplitude = 0;
        currentShakeDirection = Vec3.ZERO;
        targetShakeDirection = Vec3.ZERO;

        posOffset = Vec3.ZERO;
        posVelocity = Vec3.ZERO;

        rotImpulse = Vec3.ZERO;
        rotImpulseVel = Vec3.ZERO;

        lastNoiseValue = 0;
        noiseVelocity = 0;
    }

    // ===== Events =====

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Vec3 shakeOffset = calculateShakeOffset();
        if (shakeOffset.equals(Vec3.ZERO)) return;

        long now = System.currentTimeMillis();

        if (now - lastDirectionChangeTime > DIRECTION_CHANGE_INTERVAL) {
            double changeIntensity = 0.5 + 0.5 * (currentAmplitude / maxAmplitude);
            targetShakeDirection = targetShakeDirection.scale(1.0 - changeIntensity)
                    .add(generateRandomDirection().scale(changeIntensity))
                    .normalize();
            lastDirectionChangeTime = now;
        }

        double smoothFactor = DIRECTION_SMOOTHING_FACTOR * (1.0 + 0.5 * (currentAmplitude / maxAmplitude));
        Vec3 directionDiff = targetShakeDirection.subtract(currentShakeDirection);
        currentShakeDirection = currentShakeDirection.add(directionDiff.scale(smoothFactor)).normalize();

        applyShakeToCameraAngles(event, shakeOffset);
    }

    @SubscribeEvent
    public static void onCameraPosition(ViewportEvent event) {
        if (startTime == 0) return;

        String simple = event.getClass().getSimpleName();
        if (!"ComputeCameraPosition".equals(simple)) return;

        Vec3 localOffset = calculateCameraPositionOffset();
        if (localOffset.equals(Vec3.ZERO)) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        Vec3 forward = player.getLookAngle();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = forward.cross(up);
        double rightLen = right.length();
        if (rightLen > 1e-6) right = right.scale(1.0 / rightLen);

        Vec3 worldOffset = right.scale(localOffset.x).add(up.scale(localOffset.y)).add(forward.scale(localOffset.z));

        try {
            Method getPosition = event.getClass().getMethod("getPosition");
            Method setPosition = event.getClass().getMethod("setPosition", Vec3.class);

            Object posObj = getPosition.invoke(event);
            if (!(posObj instanceof Vec3)) return;

            Vec3 pos = (Vec3) posObj;
            setPosition.invoke(event, pos.add(worldOffset));
        } catch (Throwable ignored) {
        }
    }

    // ===== Core Shake =====

    private static Vec3 calculateShakeOffset() {
        if (startTime == 0) return Vec3.ZERO;

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > duration) {
            resetShake();
            return Vec3.ZERO;
        }

        currentAmplitude = calculateCurrentAmplitude(elapsed);
        if (currentAmplitude <= 0) return Vec3.ZERO;

        double timeFactor = elapsed * speed / 1000.0;

        double xOffset = Math.sin(timeFactor) * currentAmplitude;
        double yOffset = Math.sin(timeFactor * 1.17 + 0.5) * currentAmplitude;
        double zOffset = Math.sin(timeFactor * 0.83 + 1.2) * currentAmplitude;

        double harmonicFactor = 2.3;
        xOffset += Math.sin(timeFactor * harmonicFactor) * currentAmplitude * 0.3;
        yOffset += Math.sin(timeFactor * harmonicFactor * 1.17 + 1.7) * currentAmplitude * 0.3;
        zOffset += Math.sin(timeFactor * harmonicFactor * 0.83 + 2.9) * currentAmplitude * 0.3;

        double micro = currentAmplitude * 0.18;
        xOffset += improvedNoise(timeFactor * 12.0 + 10.0) * micro;
        yOffset += improvedNoise(timeFactor * 14.0 + 20.0) * micro;
        zOffset += improvedNoise(timeFactor * 10.0 + 30.0) * micro;

        zOffset += improvedNoise(timeFactor * 6.0 + 200.0) * (currentAmplitude * 0.35);

        return new Vec3(xOffset, yOffset, zOffset);
    }

    private static Vec3 calculateCameraPositionOffset() {
        if (startTime == 0) return Vec3.ZERO;

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > duration) {
            return Vec3.ZERO;
        }

        float amp = currentAmplitude;
        if (amp <= 0) return Vec3.ZERO;

        double t = elapsed * speed / 1000.0;
        double intensity = Math.min(1.0, amp / Math.max(1e-6f, maxAmplitude));

        double base = amp * (0.035 + 0.03 * intensity);
        double side = Math.sin(t * 1.9) * base * 0.45 + improvedNoise(t * 8.0 + 7.0) * base * 0.55;
        double up = Math.sin(t * 2.2 + 1.3) * base * 0.35 + improvedNoise(t * 9.0 + 17.0) * base * 0.65;

        double fore = improvedNoise(t * 5.0 + 70.0) * base * 1.25;

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

    private static void applyShakeToCameraAngles(ViewportEvent.ComputeCameraAngles event, Vec3 shakeOffset) {
        double amp = shakeOffset.length();
        double intensity = amp / Math.max(1e-6f, maxAmplitude);

        double nonLinearity = 1.0 + 0.75 * intensity * intensity;

        double dampingFactor = 0.72 + 0.28 * Math.exp(-intensity * 3.2);

        Vec3 baseRot = currentShakeDirection.scale(amp);

        double t = (System.currentTimeMillis() - startTime) * 0.001;
        Vec3 microRot = new Vec3(
                improvedNoise(t * 28.0 + 11.0),
                improvedNoise(t * 31.0 + 23.0),
                improvedNoise(t * 26.0 + 37.0)
        ).scale(amp * 0.22);

        if (random.nextFloat() < 0.0035f * intensity) {
            Vec3 kick = new Vec3(
                    (random.nextFloat() * 2f - 1f),
                    (random.nextFloat() * 2f - 1f) * 0.65,
                    (random.nextFloat() * 2f - 1f) * 0.35
            ).scale(amp * (1.2 + 1.8 * intensity));
            rotImpulseVel = rotImpulseVel.add(kick);
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

        event.setYaw((float) (event.getYaw() + rot.x * 12.0 * nonLinearity * dampingFactor));
        event.setPitch((float) (event.getPitch() + rot.y * 13.5 * nonLinearity * dampingFactor));
        event.setRoll((float) (event.getRoll() + rot.z * 12.2 * nonLinearity * dampingFactor));

        double facingBias = (0.6 + 0.4 * intensity) * amp;
        event.setYaw((float) (event.getYaw() + improvedNoise(t * 6.0 + 300.0) * facingBias * 2.4));
        event.setPitch((float) (event.getPitch() + improvedNoise(t * 6.5 + 500.0) * facingBias * 1.6));
        event.setRoll((float) (event.getRoll() + improvedNoise(t * 6.2 + 700.0) * facingBias * 1.8));
    }

    // ===== Helpers =====

    private static Vec3 generateRandomDirection() {
        double x = random.nextGaussian() * 0.5;
        double y = random.nextGaussian() * 0.5;
        double z = random.nextGaussian() * 0.5;

        Vec3 direction = new Vec3(x, y, z);
        double length = direction.length();
        if (length > 1e-5) direction = direction.scale(1.0 / length);

        return direction;
    }

    private static float calculateCurrentAmplitude(long elapsed) {
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

            float lowFreqNoise = (float) improvedNoise(elapsed * 0.002) * noiseAmplitude;
            float midFreqNoise = (float) improvedNoise(elapsed * 0.015 + 100) * noiseAmplitude * 0.7f;
            float highFreqNoise = (float) improvedNoise(elapsed * 0.06 + 200) * noiseAmplitude * 0.4f;

            if (random.nextFloat() < 0.005f * (baseAmplitude / maxAmplitude)) {
                float impulse = (random.nextFloat() * 2.0f - 1.0f) * noiseAmplitude * 1.5f;
                lowFreqNoise += impulse;
            }

            return Math.max(0, baseAmplitude + lowFreqNoise + midFreqNoise + highFreqNoise);
        }

        return baseAmplitude;
    }

    private static double improvedNoise(double x) {
        int X = (int) Math.floor(x) & 255;
        x -= Math.floor(x);

        double u = x * x * x * (x * (x * 6 - 15) + 10);

        double a = grad(hash(X), x);
        double b = grad(hash(X + 1), x - 1);

        return lerp(u, a, b);
    }

    private static double lerp(double t, double a, double b) {
        double t2 = t * t;
        double t3 = t2 * t;
        return (2 * t3 - 3 * t2 + 1) * a + (t3 - 2 * t2 + t) * (b - a);
    }

    private static int hash(int n) {
        n = (n << 13) ^ n;
        n = (n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff;
        return n;
    }

    private static double grad(int hash, double x) {
        int h = hash & 15;
        double[] gradients = {1, -1, 0.5, -0.5, 0.7071, -0.7071, 0.25, -0.25};
        return gradients[h & 7] * x;
    }
}