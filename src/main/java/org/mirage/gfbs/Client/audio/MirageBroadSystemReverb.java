package org.mirage.gfbs.Client.audio;

import org.lwjgl.openal.AL11;
import org.lwjgl.openal.EXTEfx;

public final class MirageBroadSystemReverb {

    private static int AUX_SLOT = 0;
    private static int REVERB_EFFECT = 0;
    private static boolean INITIALIZED = false;
    private static boolean SUPPORTED = false;

    private static float ROOM_SIZE = 100.0f;
    private static float PRE_DELAY = 0.0f;
    private static float REVERB_FEEL = 20.0f;
    private static float DAMPING = 4.0f;
    private static float LOW_TONE = 19.0f;
    private static float HIGH_TONE = 100.0f;
    private static float WET_GAIN = 3.0f;
    private static float DRY_GAIN = -2.0f;
    private static float STEREO_WIDTH = 14.0f;

    private MirageBroadSystemReverb() {}

    public static void ensureInit() {
        if (INITIALIZED) return;
        INITIALIZED = true;

        try {
            REVERB_EFFECT = EXTEfx.alGenEffects();
            if (REVERB_EFFECT == 0 || AL11.alGetError() != AL11.AL_NO_ERROR) {
                System.err.println("[MirageGFBS] Failed to create broad system reverb effect");
                REVERB_EFFECT = 0;
                AUX_SLOT = 0;
                return;
            }

            EXTEfx.alEffecti(REVERB_EFFECT, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_REVERB);
            int error = AL11.alGetError();
            if (error != AL11.AL_NO_ERROR) {
                System.out.println("[MirageGFBS] Broad system reverb effect type not supported (error: " + error + ")");
                safeDeleteEffect();
                REVERB_EFFECT = 0;
                AUX_SLOT = 0;
                return;
            }

            SUPPORTED = true;

            applyReverbSettingsInternal();

            AUX_SLOT = EXTEfx.alGenAuxiliaryEffectSlots();
            if (AUX_SLOT == 0 || AL11.alGetError() != AL11.AL_NO_ERROR) {
                System.err.println("[MirageGFBS] Failed to create broad system reverb aux slot");
                AUX_SLOT = 0;
                safeDeleteEffect();
                return;
            }

            EXTEfx.alAuxiliaryEffectSloti(AUX_SLOT, EXTEfx.AL_EFFECTSLOT_EFFECT, REVERB_EFFECT);
            if (AL11.alGetError() != AL11.AL_NO_ERROR) {
                System.err.println("[MirageGFBS] Failed to attach broad system reverb effect to aux slot");
                shutdown();
                return;
            }

            System.out.println("[MirageGFBS] Broad system reverb initialized successfully (effect=" + REVERB_EFFECT + ", auxSlot=" + AUX_SLOT + ")");
        } catch (Throwable t) {
            System.err.println("[MirageGFBS] Exception during broad system reverb initialization: " + t.getMessage());
            SUPPORTED = false;
            shutdown();
        }
    }

    private static void applyReverbSettingsInternal() {
        if (!SUPPORTED || REVERB_EFFECT == 0) return;

        try {
            float roomSizeNorm = ROOM_SIZE / 100.0f;
            float preDelaySec = PRE_DELAY / 1000.0f;
            float reverbFeelNorm = REVERB_FEEL / 100.0f;
            float dampingNorm = DAMPING / 100.0f;
            float lowToneNorm = LOW_TONE / 100.0f;
            float highToneNorm = HIGH_TONE / 100.0f;

            float density = 1.0f;
            float diffusion = 0.9f;
            float decayTime = 1.0f + roomSizeNorm * 6.5f;

            float gainScale = 0.45f * (1.0f + WET_GAIN / 10.0f);
            gainScale = Math.max(0.1f, Math.min(1.0f, gainScale));

            float gain = 0.6f * gainScale;
            float gainHf = 0.4f * gainScale * (0.5f + highToneNorm * 0.5f);
            float reflectionsGain = 0.7f * gainScale * (0.5f + lowToneNorm * 0.5f);
            float lateReverbGain = 0.9f * gainScale;

            EXTEfx.alEffectf(REVERB_EFFECT, EXTEfx.AL_REVERB_DENSITY, density);
            EXTEfx.alEffectf(REVERB_EFFECT, EXTEfx.AL_REVERB_DIFFUSION, diffusion);
            EXTEfx.alEffectf(REVERB_EFFECT, EXTEfx.AL_REVERB_GAIN, gain);
            EXTEfx.alEffectf(REVERB_EFFECT, EXTEfx.AL_REVERB_GAINHF, gainHf);
            EXTEfx.alEffectf(REVERB_EFFECT, EXTEfx.AL_REVERB_DECAY_TIME, decayTime);
            EXTEfx.alEffectf(REVERB_EFFECT, EXTEfx.AL_REVERB_REFLECTIONS_GAIN, reflectionsGain);
            EXTEfx.alEffectf(REVERB_EFFECT, EXTEfx.AL_REVERB_REFLECTIONS_DELAY, preDelaySec);
            EXTEfx.alEffectf(REVERB_EFFECT, EXTEfx.AL_REVERB_LATE_REVERB_GAIN, lateReverbGain);

            int error = AL11.alGetError();
            if (error != AL11.AL_NO_ERROR) {
                System.err.println("[MirageGFBS] Error applying broad system reverb settings: " + error);
            } else {
                System.out.println("[MirageGFBS] Broad system reverb settings applied: decayTime=" + decayTime + ", gain=" + gain);
            }
        } catch (Throwable t) {
            System.err.println("[MirageGFBS] Failed to apply broad system reverb settings: " + t.getMessage());
        }
    }

    public static void setRoomSize(float percent) {
        ROOM_SIZE = Math.max(0.0f, Math.min(100.0f, percent));
        if (INITIALIZED && SUPPORTED) {
            applyReverbSettingsInternal();
        }
    }

    public static void setPreDelay(float ms) {
        PRE_DELAY = Math.max(0.0f, Math.min(300.0f, ms));
        if (INITIALIZED && SUPPORTED) {
            applyReverbSettingsInternal();
        }
    }

    public static void setReverbFeel(float percent) {
        REVERB_FEEL = Math.max(0.0f, Math.min(100.0f, percent));
        if (INITIALIZED && SUPPORTED) {
            applyReverbSettingsInternal();
        }
    }

    public static void setDamping(float percent) {
        DAMPING = Math.max(0.0f, Math.min(100.0f, percent));
        if (INITIALIZED && SUPPORTED) {
            applyReverbSettingsInternal();
        }
    }

    public static void setLowTone(float percent) {
        LOW_TONE = Math.max(0.0f, Math.min(100.0f, percent));
        if (INITIALIZED && SUPPORTED) {
            applyReverbSettingsInternal();
        }
    }

    public static void setHighTone(float percent) {
        HIGH_TONE = Math.max(0.0f, Math.min(100.0f, percent));
        if (INITIALIZED && SUPPORTED) {
            applyReverbSettingsInternal();
        }
    }

    public static void setWetGain(float db) {
        WET_GAIN = Math.max(-60.0f, Math.min(20.0f, db));
        if (INITIALIZED && SUPPORTED) {
            applyReverbSettingsInternal();
        }
    }

    public static void setDryGain(float db) {
        DRY_GAIN = Math.max(-60.0f, Math.min(20.0f, db));
    }

    public static void setStereoWidth(float percent) {
        STEREO_WIDTH = Math.max(0.0f, Math.min(100.0f, percent));
        if (INITIALIZED && SUPPORTED) {
            applyReverbSettingsInternal();
        }
    }

    public static float getDryGain() {
        return DRY_GAIN;
    }

    public static boolean isSupported() {
        return SUPPORTED;
    }

    public static void shutdown() {
        safeDeleteAuxSlot();
        safeDeleteEffect();
        AUX_SLOT = 0;
        REVERB_EFFECT = 0;
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
            if (REVERB_EFFECT != 0) {
                EXTEfx.alDeleteEffects(REVERB_EFFECT);
            }
        } catch (Throwable ignored) {}
    }
}
