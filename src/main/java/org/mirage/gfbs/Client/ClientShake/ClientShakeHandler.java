/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private static final List<ShakeInstance> activeShakes = Collections.synchronizedList(new ArrayList<>());
    private static final Object shakeListLock = new Object();

    public static void init() {
        ShakeAnimationThread.start();
    }

    public static void shutdown() {
        ShakeAnimationThread.stop();
    }

    public static void resetShake() {
        synchronized (shakeListLock) {
            activeShakes.clear();
        }
        startTime = 0;
        currentAmplitude = 0;
    }

    public static void addShake(float speed, float maxAmplitude, int duration, int riseTime, int fallTime) {
        synchronized (shakeListLock) {
            activeShakes.add(new ShakeInstance(speed, maxAmplitude, duration, riseTime, fallTime));
        }
    }

    public static void updateAllShakes() {
        synchronized (shakeListLock) {
            activeShakes.removeIf(ShakeInstance::isFinished);
            for (ShakeInstance shake : activeShakes) {
                shake.update();
            }
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        List<ShakeInstance> shakesSnapshot;
        synchronized (shakeListLock) {
            if (activeShakes.isEmpty()) return;
            shakesSnapshot = new ArrayList<>(activeShakes);
        }

        double totalYaw = 0;
        double totalPitch = 0;
        double totalRoll = 0;

        for (ShakeInstance shake : shakesSnapshot) {
            Vec3 rot = shake.getRotationOffset();
            if (rot.equals(Vec3.ZERO)) continue;

            totalYaw += rot.x;
            totalPitch += rot.y;
            totalRoll += rot.z;
        }

        event.setYaw((float) (event.getYaw() + totalYaw));
        event.setPitch((float) (event.getPitch() + totalPitch));
        event.setRoll((float) (event.getRoll() + totalRoll));
    }

    @SubscribeEvent
    public static void onCameraPosition(ViewportEvent event) {
        String simple = event.getClass().getSimpleName();
        if (!"ComputeCameraPosition".equals(simple)) return;

        List<ShakeInstance> shakesSnapshot;
        synchronized (shakeListLock) {
            if (activeShakes.isEmpty()) return;
            shakesSnapshot = new ArrayList<>(activeShakes);
        }

        Vec3 totalLocalOffset = Vec3.ZERO;

        for (ShakeInstance shake : shakesSnapshot) {
            Vec3 localOffset = shake.getPositionOffset();
            totalLocalOffset = totalLocalOffset.add(localOffset);
        }

        if (totalLocalOffset.equals(Vec3.ZERO)) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        Vec3 forward = player.getLookAngle();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = forward.cross(up);
        double rightLen = right.length();
        if (rightLen > 1e-6) right = right.scale(1.0 / rightLen);

        Vec3 worldOffset = right.scale(totalLocalOffset.x).add(up.scale(totalLocalOffset.y)).add(forward.scale(totalLocalOffset.z));

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

    public static Vec3 generateRandomDirection(Random rand) {
        double x = rand.nextGaussian() * 0.5;
        double y = rand.nextGaussian() * 0.5;
        double z = rand.nextGaussian() * 0.5;

        Vec3 direction = new Vec3(x, y, z);
        double length = direction.length();
        if (length > 1e-5) direction = direction.scale(1.0 / length);

        return direction;
    }

    private static Vec3 generateRandomDirection() {
        return generateRandomDirection(random);
    }

    public static double improvedNoise(double x) {
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
