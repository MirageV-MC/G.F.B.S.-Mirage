package org.mirage.Client;

import net.minecraft.util.Mth;

import java.awt.*;

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

    private static float exposure = 0.0f; // current [0..1]
    private static float target = 0.0f;   // target  [0..1]
    private static float velocity = 0.0f; // smoothing velocity

    public static int rgb = 0xFFFFFF; // current RGB (no alpha)

    private static float curR = 1.0f, curG = 1.0f, curB = 1.0f;
    private static float startR = 1.0f, startG = 1.0f, startB = 1.0f;
    private static float targetR = 1.0f, targetG = 1.0f, targetB = 1.0f;
    private static float colorT = 1.0f; // 0..1 progress

    public static float COLOR_LERP_TIME_SEC = 1.0f;

    private static final float RISE_TIME_SEC = 0.48f;

    private static final float FALL_TIME_SEC = 0.45f;

    private static final float MAX_SPEED = 6.0f;

    private ExposureController() {}

    public static void setExposure(float value, Color rgb_) {
        target = Mth.clamp(value, 0.0f, 1.0f);

        startR = curR;
        startG = curG;
        startB = curB;

        targetR = Mth.clamp(rgb_.getRed() / 255.0f, 0.0f, 1.0f);
        targetG = Mth.clamp(rgb_.getGreen() / 255.0f, 0.0f, 1.0f);
        targetB = Mth.clamp(rgb_.getBlue() / 255.0f, 0.0f, 1.0f);

        colorT = 0.0f;
    }

    public static void tick(float dtSec) {
        if (dtSec <= 0.0f) return;

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