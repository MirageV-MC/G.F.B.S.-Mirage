package org.mirage.Client.audio;

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

public final class MirageReverb {

    private static int AUX_SLOT = 0;
    private static int REVERB_EFFECT = 0;
    private static boolean INITIALIZED = false;

    private static float CURRENT_STRENGTH = 1.0f;

    private MirageReverb() {}

    public static void ensureInit() {
        if (INITIALIZED) return;
        INITIALIZED = true;

        try {
            REVERB_EFFECT = EXTEfx.alGenEffects();
            if (REVERB_EFFECT == 0 || AL11.alGetError() != AL11.AL_NO_ERROR) {
                REVERB_EFFECT = 0;
                AUX_SLOT = 0;
                return;
            }

            EXTEfx.alEffecti(REVERB_EFFECT, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_REVERB);

            EXTEfx.alEffectf(REVERB_EFFECT, EXTEfx.AL_REVERB_DECAY_TIME, 7.5f);
            EXTEfx.alEffectf(REVERB_EFFECT, EXTEfx.AL_REVERB_DENSITY, 1.0f);
            EXTEfx.alEffectf(REVERB_EFFECT, EXTEfx.AL_REVERB_DIFFUSION, 0.9f);

            applyStrengthInternal();

            AUX_SLOT = EXTEfx.alGenAuxiliaryEffectSlots();
            if (AUX_SLOT == 0 || AL11.alGetError() != AL11.AL_NO_ERROR) {
                AUX_SLOT = 0;
                safeDeleteEffect();
                return;
            }

            EXTEfx.alAuxiliaryEffectSloti(AUX_SLOT, EXTEfx.AL_EFFECTSLOT_EFFECT, REVERB_EFFECT);
            if (AL11.alGetError() != AL11.AL_NO_ERROR) {
                shutdown();
            }
        } catch (Throwable t) {
            shutdown();
        }
    }

    public static void setStrength(double strength) {
        float s;
        if (Double.isNaN(strength) || Double.isInfinite(strength)) {
            s = 1.0f;
        } else {
            s = (float) strength;
        }
        if (s < 0.0f) s = 0.0f;
        if (s > 3.0f) s = 3.0f;

        CURRENT_STRENGTH = s;

        if (INITIALIZED && REVERB_EFFECT != 0) {
            try {
                applyStrengthInternal();
            } catch (Throwable ignored) {}
        }
    }

    private static void applyStrengthInternal() {
        float scale = 0.45f * CURRENT_STRENGTH;

        EXTEfx.alEffectf(REVERB_EFFECT, EXTEfx.AL_REVERB_GAIN, 0.6f * scale);
        EXTEfx.alEffectf(REVERB_EFFECT, EXTEfx.AL_REVERB_GAINHF, 0.4f * scale);
        EXTEfx.alEffectf(REVERB_EFFECT, EXTEfx.AL_REVERB_REFLECTIONS_GAIN, 0.7f * scale);
        EXTEfx.alEffectf(REVERB_EFFECT, EXTEfx.AL_REVERB_LATE_REVERB_GAIN, 0.9f * scale);
    }

    public static void shutdown() {
        safeDeleteAuxSlot();
        safeDeleteEffect();
        AUX_SLOT = 0;
        REVERB_EFFECT = 0;
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
            if (REVERB_EFFECT != 0) {
                EXTEfx.alDeleteEffects(REVERB_EFFECT);
            }
        } catch (Throwable ignored) {}
    }
}
