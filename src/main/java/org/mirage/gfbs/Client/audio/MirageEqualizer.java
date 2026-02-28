package org.mirage.gfbs.Client.audio;

import org.lwjgl.openal.AL11;
import org.lwjgl.openal.EXTEfx;

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
 */

public final class MirageEqualizer {

    private static int AUX_SLOT = 0;
    private static int EQUALIZER_EFFECT = 0;
    private static boolean INITIALIZED = false;

    private static float LOW_GAIN = 2.0f;
    private static float MID1_GAIN = 3.0f;
    private static float MID2_GAIN = 1.5f;
    private static float HIGH_GAIN = -6.0f;

    private MirageEqualizer() {}

    public static void ensureInit() {
        if (INITIALIZED) return;
        INITIALIZED = true;

        try {
            EQUALIZER_EFFECT = EXTEfx.alGenEffects();
            if (EQUALIZER_EFFECT == 0 || AL11.alGetError() != AL11.AL_NO_ERROR) {
                System.err.println("[MirageGFBS] Failed to create equalizer effect");
                EQUALIZER_EFFECT = 0;
                AUX_SLOT = 0;
                return;
            }

            EXTEfx.alEffecti(EQUALIZER_EFFECT, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_EQUALIZER);

            applyEqualizerSettingsInternal();

            AUX_SLOT = EXTEfx.alGenAuxiliaryEffectSlots();
            if (AUX_SLOT == 0 || AL11.alGetError() != AL11.AL_NO_ERROR) {
                System.err.println("[MirageGFBS] Failed to create equalizer aux slot");
                AUX_SLOT = 0;
                safeDeleteEffect();
                return;
            }

            EXTEfx.alAuxiliaryEffectSloti(AUX_SLOT, EXTEfx.AL_EFFECTSLOT_EFFECT, EQUALIZER_EFFECT);
            if (AL11.alGetError() != AL11.AL_NO_ERROR) {
                System.err.println("[MirageGFBS] Failed to attach equalizer effect to aux slot");
                shutdown();
                return;
            }

            System.out.println("[MirageGFBS] Equalizer initialized successfully (effect=" + EQUALIZER_EFFECT + ", auxSlot=" + AUX_SLOT + ")");
        } catch (Throwable t) {
            System.err.println("[MirageGFBS] Exception during equalizer initialization: " + t.getMessage());
            shutdown();
        }
    }

    private static void applyEqualizerSettingsInternal() {
        EXTEfx.alEffectf(EQUALIZER_EFFECT, EXTEfx.AL_EQUALIZER_LOW_GAIN, LOW_GAIN);
        EXTEfx.alEffectf(EQUALIZER_EFFECT, EXTEfx.AL_EQUALIZER_MID1_GAIN, MID1_GAIN);
        EXTEfx.alEffectf(EQUALIZER_EFFECT, EXTEfx.AL_EQUALIZER_MID2_GAIN, MID2_GAIN);
        EXTEfx.alEffectf(EQUALIZER_EFFECT, EXTEfx.AL_EQUALIZER_HIGH_GAIN, HIGH_GAIN);

        EXTEfx.alEffectf(EQUALIZER_EFFECT, EXTEfx.AL_EQUALIZER_LOW_CUTOFF, 150.0f);
        EXTEfx.alEffectf(EQUALIZER_EFFECT, EXTEfx.AL_EQUALIZER_MID1_CENTER, 800.0f);
        EXTEfx.alEffectf(EQUALIZER_EFFECT, EXTEfx.AL_EQUALIZER_MID1_WIDTH, 1.2f);
        EXTEfx.alEffectf(EQUALIZER_EFFECT, EXTEfx.AL_EQUALIZER_MID2_CENTER, 2500.0f);
        EXTEfx.alEffectf(EQUALIZER_EFFECT, EXTEfx.AL_EQUALIZER_MID2_WIDTH, 0.8f);
        EXTEfx.alEffectf(EQUALIZER_EFFECT, EXTEfx.AL_EQUALIZER_HIGH_CUTOFF, 4000.0f);
    }

    public static void setLowGain(float gain) {
        LOW_GAIN = clampGain(gain);
        if (INITIALIZED && EQUALIZER_EFFECT != 0) {
            try {
                EXTEfx.alEffectf(EQUALIZER_EFFECT, EXTEfx.AL_EQUALIZER_LOW_GAIN, LOW_GAIN);
            } catch (Throwable ignored) {}
        }
    }

    public static void setMid1Gain(float gain) {
        MID1_GAIN = clampGain(gain);
        if (INITIALIZED && EQUALIZER_EFFECT != 0) {
            try {
                EXTEfx.alEffectf(EQUALIZER_EFFECT, EXTEfx.AL_EQUALIZER_MID1_GAIN, MID1_GAIN);
            } catch (Throwable ignored) {}
        }
    }

    public static void setMid2Gain(float gain) {
        MID2_GAIN = clampGain(gain);
        if (INITIALIZED && EQUALIZER_EFFECT != 0) {
            try {
                EXTEfx.alEffectf(EQUALIZER_EFFECT, EXTEfx.AL_EQUALIZER_MID2_GAIN, MID2_GAIN);
            } catch (Throwable ignored) {}
        }
    }

    public static void setHighGain(float gain) {
        HIGH_GAIN = clampGain(gain);
        if (INITIALIZED && EQUALIZER_EFFECT != 0) {
            try {
                EXTEfx.alEffectf(EQUALIZER_EFFECT, EXTEfx.AL_EQUALIZER_HIGH_GAIN, HIGH_GAIN);
            } catch (Throwable ignored) {}
        }
    }

    private static float clampGain(float gain) {
        if (Float.isNaN(gain) || Float.isInfinite(gain)) {
            return 0.0f;
        }
        return Math.max(-80.0f, Math.min(80.0f, gain));
    }

    public static void shutdown() {
        safeDeleteAuxSlot();
        safeDeleteEffect();
        AUX_SLOT = 0;
        EQUALIZER_EFFECT = 0;
        INITIALIZED = false;
    }

    public static int getAuxSlot() {
        return AUX_SLOT;
    }

    private static void safeDeleteAuxSlot() {
        try {
            if (AUX_SLOT != 0) {
                EXTEfx.alDeleteAuxiliaryEffectSlots(AUX_SLOT);
            }
        } catch (Throwable ignored) {}
    }

    private static void safeDeleteEffect() {
        try {
            if (EQUALIZER_EFFECT != 0) {
                EXTEfx.alDeleteEffects(EQUALIZER_EFFECT);
            }
        } catch (Throwable ignored) {}
    }
}
