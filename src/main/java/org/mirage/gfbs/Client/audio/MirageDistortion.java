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

public final class MirageDistortion {

    private static final int AL_EFFECT_DISTORTION = 0x8001;
    private static final int AL_DISTORTION_EDGE = 0x0001;
    private static final int AL_DISTORTION_GAIN = 0x0002;
    private static final int AL_DISTORTION_LOWCUTOFF = 0x0003;
    private static final int AL_DISTORTION_HIGHCUTOFF = 0x0004;
    private static final int AL_DISTORTION_EQCENTER = 0x0005;
    private static final int AL_DISTORTION_EQBANDWIDTH = 0x0006;

    private static int AUX_SLOT = 0;
    private static int DISTORTION_EFFECT = 0;
    private static boolean INITIALIZED = false;
    private static boolean SUPPORTED = false;

    private static float CURRENT_EDGE = 0.3f;
    private static float CURRENT_GAIN = 0.15f;
    private static float CURRENT_LOWCUTOFF = 150.0f;
    private static float CURRENT_HIGHCUTOFF = 4500.0f;
    private static float CURRENT_EQCENTER = 2000.0f;
    private static float CURRENT_EQBANDWIDTH = 0.6f;

    private MirageDistortion() {}

    public static void ensureInit() {
        if (INITIALIZED) return;
        INITIALIZED = true;

        try {
            DISTORTION_EFFECT = EXTEfx.alGenEffects();
            if (DISTORTION_EFFECT == 0 || AL11.alGetError() != AL11.AL_NO_ERROR) {
                System.err.println("[MirageGFBS] Failed to create distortion effect");
                DISTORTION_EFFECT = 0;
                AUX_SLOT = 0;
                return;
            }

            EXTEfx.alEffecti(DISTORTION_EFFECT, EXTEfx.AL_EFFECT_TYPE, AL_EFFECT_DISTORTION);
            int error = AL11.alGetError();
            if (error != AL11.AL_NO_ERROR) {
                System.out.println("[MirageGFBS] Distortion effect type not supported (error: " + error + ")");
                safeDeleteEffect();
                DISTORTION_EFFECT = 0;
                AUX_SLOT = 0;
                return;
            }

            SUPPORTED = true;

            applyCurrentSettingsInternal();

            AUX_SLOT = EXTEfx.alGenAuxiliaryEffectSlots();
            if (AUX_SLOT == 0 || AL11.alGetError() != AL11.AL_NO_ERROR) {
                System.err.println("[MirageGFBS] Failed to create distortion aux slot");
                AUX_SLOT = 0;
                safeDeleteEffect();
                return;
            }

            EXTEfx.alAuxiliaryEffectSloti(AUX_SLOT, EXTEfx.AL_EFFECTSLOT_EFFECT, DISTORTION_EFFECT);
            if (AL11.alGetError() != AL11.AL_NO_ERROR) {
                System.err.println("[MirageGFBS] Failed to attach distortion effect to aux slot");
                shutdown();
                return;
            }

            System.out.println("[MirageGFBS] Distortion initialized successfully (effect=" + DISTORTION_EFFECT + ", auxSlot=" + AUX_SLOT + ")");
        } catch (Throwable t) {
            System.err.println("[MirageGFBS] Exception during distortion initialization: " + t.getMessage());
            SUPPORTED = false;
            shutdown();
        }
    }

    private static void applyCurrentSettingsInternal() {
        if (!SUPPORTED || DISTORTION_EFFECT == 0) return;

        try {
            EXTEfx.alEffectf(DISTORTION_EFFECT, AL_DISTORTION_EDGE, CURRENT_EDGE);
            EXTEfx.alEffectf(DISTORTION_EFFECT, AL_DISTORTION_GAIN, CURRENT_GAIN);
            EXTEfx.alEffectf(DISTORTION_EFFECT, AL_DISTORTION_LOWCUTOFF, CURRENT_LOWCUTOFF);
            EXTEfx.alEffectf(DISTORTION_EFFECT, AL_DISTORTION_HIGHCUTOFF, CURRENT_HIGHCUTOFF);
            EXTEfx.alEffectf(DISTORTION_EFFECT, AL_DISTORTION_EQCENTER, CURRENT_EQCENTER);
            EXTEfx.alEffectf(DISTORTION_EFFECT, AL_DISTORTION_EQBANDWIDTH, CURRENT_EQBANDWIDTH);
        } catch (Throwable t) {
            System.err.println("[MirageGFBS] Failed to apply distortion settings: " + t.getMessage());
        }
    }

    public static void setStrength(float strength) {
        if (strength < 0.0f) strength = 0.0f;
        if (strength > 1.0f) strength = 1.0f;

        CURRENT_EDGE = 0.1f + (strength * 0.5f);
        CURRENT_GAIN = 0.05f + (strength * 0.25f);
        CURRENT_LOWCUTOFF = 100.0f + (strength * 150.0f);
        CURRENT_HIGHCUTOFF = 6000.0f - (strength * 2500.0f);
        CURRENT_EQCENTER = 1500.0f + (strength * 1500.0f);
        CURRENT_EQBANDWIDTH = 0.4f + (strength * 0.4f);

        if (INITIALIZED && SUPPORTED) {
            applyCurrentSettingsInternal();
        }
    }

    public static boolean isSupported() {
        return SUPPORTED;
    }

    public static void shutdown() {
        safeDeleteAuxSlot();
        safeDeleteEffect();
        AUX_SLOT = 0;
        DISTORTION_EFFECT = 0;
        INITIALIZED = false;
        SUPPORTED = false;
    }

    public static int getAuxSlot() {
        return SUPPORTED ? AUX_SLOT : 0;
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
            if (DISTORTION_EFFECT != 0) {
                EXTEfx.alDeleteEffects(DISTORTION_EFFECT);
            }
        } catch (Throwable ignored) {}
    }
}
