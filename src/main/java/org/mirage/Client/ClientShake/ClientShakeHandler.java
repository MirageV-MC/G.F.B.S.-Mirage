/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Convex89524

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

package org.mirage.Client.ClientShake;

import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = "mirage_gfbs", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientShakeHandler {
    // 参数
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
    private static final long DIRECTION_CHANGE_INTERVAL = 80; // 略微减少方向变化间隔
    private static final double DIRECTION_SMOOTHING_FACTOR = 0.12; // 调整平滑过渡因子

    private static double lastNoiseValue = 0;
    private static double noiseVelocity = 0;

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

        return new Vec3(xOffset, yOffset, zOffset);
    }

    public static void resetShake() {
        startTime = 0;
        currentAmplitude = 0;
        currentShakeDirection = Vec3.ZERO;
        targetShakeDirection = Vec3.ZERO;
        lastNoiseValue = 0;
        noiseVelocity = 0;
    }

    private static Vec3 generateRandomDirection() {
        double x = random.nextGaussian() * 0.5;
        double y = random.nextGaussian() * 0.5;
        double z = random.nextGaussian() * 0.5;

        Vec3 direction = new Vec3(x, y, z);
        double length = direction.length();
        if (length > 1e-5) {
            direction = direction.scale(1.0 / length);
        }

        return direction;
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        Vec3 shakeOffset = calculateShakeOffset();
        if (shakeOffset.equals(Vec3.ZERO)) return;

        long currentTime = System.currentTimeMillis();

        if (currentTime - lastDirectionChangeTime > DIRECTION_CHANGE_INTERVAL) {
            double changeIntensity = 0.5 + 0.5 * (currentAmplitude / maxAmplitude);
            targetShakeDirection = targetShakeDirection.scale(1.0 - changeIntensity)
                    .add(generateRandomDirection().scale(changeIntensity))
                    .normalize();
            lastDirectionChangeTime = currentTime;
        }

        double smoothFactor = DIRECTION_SMOOTHING_FACTOR * (1.0 + 0.5 * (currentAmplitude / maxAmplitude));
        Vec3 directionDiff = targetShakeDirection.subtract(currentShakeDirection);
        currentShakeDirection = currentShakeDirection.add(directionDiff.scale(smoothFactor)).normalize();

        applyShakeToCamera(event, shakeOffset);
    }

    private static void applyShakeToCamera(ViewportEvent.ComputeCameraAngles event, Vec3 shakeOffset) {
        double intensity = shakeOffset.length() / maxAmplitude;
        double nonLinearity = 1.0 + 0.5 * intensity * intensity;

        double dampingFactor = 0.7 + 0.3 * Math.exp(-intensity * 3.0);

        Vec3 rotationOffset = currentShakeDirection.scale(shakeOffset.length());

        event.setYaw((float) (event.getYaw() + rotationOffset.x * 12.0 * nonLinearity * dampingFactor));
        event.setPitch((float) (event.getPitch() + rotationOffset.y * 7.0 * nonLinearity * dampingFactor));
        event.setRoll((float) (event.getRoll() + rotationOffset.z * 4.0 * nonLinearity * dampingFactor));
    }

    private static float calculateCurrentAmplitude(long elapsed) {
        if (elapsed > duration) {
            return 0;
        }

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
        double b = grad(hash(X+1), x-1);

        return cubicInterpolate(a, b, u);
    }

    private static double cubicInterpolate(double a, double b, double t) {
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