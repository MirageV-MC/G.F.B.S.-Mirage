package org.mirage.gfbs.Client;

import net.minecraft.util.Mth;

import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

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

public final class ExposureController {

    private static final float RISE_TIME_SEC = 0.48f;
    private static final float FALL_TIME_SEC = 0.45f;
    private static final float MAX_SPEED = 6.0f;
    private static final long FRAME_TIME_NS = 16_666_667L;

    private static volatile float exposure = 0.0f;
    private static volatile float target = 0.0f;
    private static volatile float velocity = 0.0f;

    public static volatile int rgb = 0xFFFFFF;

    private static volatile float curR = 1.0f, curG = 1.0f, curB = 1.0f;
    private static volatile float startR = 1.0f, startG = 1.0f, startB = 1.0f;
    private static volatile float targetR = 1.0f, targetG = 1.0f, targetB = 1.0f;
    private static volatile float colorT = 1.0f;

    public static volatile float COLOR_LERP_TIME_SEC = 1.0f;

    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static Thread animationThread;
    private static final Object stateLock = new Object();

    private ExposureController() {}

    public static void startAnimationThread() {
        if (running.compareAndSet(false, true)) {
            animationThread = new Thread(() -> {
                long lastTime = System.nanoTime();
                while (running.get()) {
                    try {
                        long now = System.nanoTime();
                        float dtSec = (now - lastTime) / 1_000_000_000.0f;
                        lastTime = now;

                        tick(dtSec);

                        long elapsed = System.nanoTime() - now;
                        long sleepNs = FRAME_TIME_NS - elapsed;
                        if (sleepNs > 0) {
                            Thread.sleep(sleepNs / 1_000_000, (int)(sleepNs % 1_000_000));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "ExposureController-Animation");
            animationThread.setDaemon(true);
            animationThread.start();
        }
    }

    public static void stopAnimationThread() {
        running.set(false);
        if (animationThread != null) {
            animationThread.interrupt();
            animationThread = null;
        }
    }

    public static void setExposure(float value, Color rgb_) {
        synchronized (stateLock) {
            target = Mth.clamp(value, 0.0f, 1.0f);

            startR = curR;
            startG = curG;
            startB = curB;

            targetR = Mth.clamp(rgb_.getRed() / 255.0f, 0.0f, 1.0f);
            targetG = Mth.clamp(rgb_.getGreen() / 255.0f, 0.0f, 1.0f);
            targetB = Mth.clamp(rgb_.getBlue() / 255.0f, 0.0f, 1.0f);

            colorT = 0.0f;
        }
    }

    private static void tick(float dtSec) {
        if (dtSec <= 0.0f) return;

        synchronized (stateLock) {
            float diff = target - exposure;
            float smoothTime = diff >= 0.0f ? RISE_TIME_SEC : FALL_TIME_SEC;
            smoothTime = Math.max(0.001f, smoothTime);

            float omega = 2.0f / smoothTime;
            float x = omega * dtSec;
            float exp = 1.0f / (1.0f + x + 0.48f * x * x + 0.235f * x * x * x);

            float originalTo = target;

            float change = exposure - target;
            float maxChange = MAX_SPEED * smoothTime;
            change = Mth.clamp(change, -maxChange, maxChange);
            target = exposure - change;

            float temp = (velocity + omega * change) * dtSec;
            velocity = (velocity - omega * temp) * exp;

            float output = target + (change + temp) * exp;

            if ((originalTo - exposure) > 0.0f == output > originalTo) {
                output = originalTo;
                velocity = 0.0f;
            }

            exposure = Mth.clamp(output, 0.0f, 1.0f);
            target = originalTo;

            if (colorT < 1.0f) {
                float step = dtSec / Math.max(0.001f, COLOR_LERP_TIME_SEC);
                colorT = Mth.clamp(colorT + step, 0.0f, 1.0f);

                curR = Mth.lerp(colorT, startR, targetR);
                curG = Mth.lerp(colorT, startG, targetG);
                curB = Mth.lerp(colorT, startB, targetB);

                int rI = (int)(Mth.clamp(curR, 0.0f, 1.0f) * 255.0f + 0.5f);
                int gI = (int)(Mth.clamp(curG, 0.0f, 1.0f) * 255.0f + 0.5f);
                int bI = (int)(Mth.clamp(curB, 0.0f, 1.0f) * 255.0f + 0.5f);
                rgb = (rI << 16) | (gI << 8) | bI;
            } else {
                curR = targetR;
                curG = targetG;
                curB = targetB;
            }

            if (Math.abs(target - exposure) < 1e-4f && Math.abs(velocity) < 1e-4f) {
                exposure = target;
                velocity = 0.0f;
            }
        }
    }

    public static float alpha() {
        return Mth.clamp(exposure * exposure, 0.0f, 1.0f);
    }

    public static boolean isActive() {
        return exposure > 0.0001f;
    }

    public static float getExposure() {
        return exposure;
    }

    public static float getTarget() {
        return target;
    }

    public static void reset() {
        synchronized (stateLock) {
            exposure = 0.0f;
            target = 0.0f;
            velocity = 0.0f;

            curR = curG = curB = 1.0f;
            startR = startG = startB = 1.0f;
            targetR = targetG = targetB = 1.0f;
            colorT = 1.0f;
            rgb = 0xFFFFFF;
        }
    }
}
