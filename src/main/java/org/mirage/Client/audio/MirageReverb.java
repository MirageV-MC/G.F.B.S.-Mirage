package org.mirage.Client.audio;

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

import org.lwjgl.openal.EXTEfx;

public final class MirageReverb {

    private static boolean INITIALIZED = false;
    private static int AUX_SLOT = 0;
    private static int REVERB_EFFECT = 0;

    private MirageReverb() {}

    public static void ensureInitialized() {
        if (INITIALIZED) {
            return;
        }
        INITIALIZED = true;

        REVERB_EFFECT = EXTEfx.alGenEffects();
        EXTEfx.alEffecti(REVERB_EFFECT, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_REVERB);

        EXTEfx.alEffectf(REVERB_EFFECT, EXTEfx.AL_REVERB_DECAY_TIME, 7.5f);
        EXTEfx.alEffectf(REVERB_EFFECT, EXTEfx.AL_REVERB_DENSITY, 1.0f);
        EXTEfx.alEffectf(REVERB_EFFECT, EXTEfx.AL_REVERB_DIFFUSION, 0.9f);

        EXTEfx.alEffectf(REVERB_EFFECT, EXTEfx.AL_REVERB_GAIN, 0.6f * 0.45f);                 // 0.15f
        EXTEfx.alEffectf(REVERB_EFFECT, EXTEfx.AL_REVERB_GAINHF, 0.4f * 0.45f);               // 0.10f
        EXTEfx.alEffectf(REVERB_EFFECT, EXTEfx.AL_REVERB_REFLECTIONS_GAIN, 0.7f * 0.45f);     // 0.175f
        EXTEfx.alEffectf(REVERB_EFFECT, EXTEfx.AL_REVERB_LATE_REVERB_GAIN, 0.9f * 0.45f);     // 0.225f

        AUX_SLOT = EXTEfx.alGenAuxiliaryEffectSlots();
        EXTEfx.alAuxiliaryEffectSloti(AUX_SLOT, EXTEfx.AL_EFFECTSLOT_EFFECT, REVERB_EFFECT);
    }

    public static int getAuxSlot() {
        return AUX_SLOT;
    }
}
