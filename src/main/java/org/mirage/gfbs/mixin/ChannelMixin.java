package org.mirage.gfbs.mixin;

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

import com.mojang.blaze3d.audio.Channel;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.EXTEfx;
import org.mirage.gfbs.Client.audio.BroadSystemAudioMarker;
import org.mirage.gfbs.Client.audio.MirageEqualizer;
import org.mirage.gfbs.Client.audio.MirageReverb;
import org.mirage.gfbs.ClientConfig.GFBSClientConfigAPI;
import org.mirage.gfbs.ClientConfig.instance.GFBSClientAudioConfig;
import org.mirage.gfbs.accessor.ChannelMixinAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Channel.class)
public abstract class ChannelMixin implements ChannelMixinAccessor {

    @Shadow private int source;

    private boolean mirage$gfbs_isBroadSystemSound = false;
    private boolean mirage$gfbs_effectsApplied = false;

    @Inject(method = "play", at = @At("HEAD"))
    private void mirage$gfbs_applyAudioEffects(CallbackInfo ci) {
        if (BroadSystemAudioMarker.isBroadSystemSound()) {
            this.mirage$gfbs_isBroadSystemSound = true;
            BroadSystemAudioMarker.clear();
        }

        applyReverbEffect();
        applyEqualizerEffect();
    }

    private void applyReverbEffect() {
        if (!GFBSClientConfigAPI.get(GFBSClientAudioConfig.ENABLE_REVERB)) {
            return;
        }

        MirageReverb.ensureInit();

        int auxSlot = MirageReverb.getAuxSlot();
        if (auxSlot == 0) {
            return;
        }

        AL11.alSource3i(
                this.source,
                EXTEfx.AL_AUXILIARY_SEND_FILTER,
                auxSlot,
                0,
                EXTEfx.AL_FILTER_NULL
        );

        AL11.alSourcei(this.source, EXTEfx.AL_AUXILIARY_SEND_FILTER_GAIN_AUTO, AL11.AL_TRUE);
        AL11.alSourcei(this.source, EXTEfx.AL_AUXILIARY_SEND_FILTER_GAINHF_AUTO, AL11.AL_TRUE);
    }

    private void applyEqualizerEffect() {
        if (!mirage$gfbs_isBroadSystemSound) {
            return;
        }

        MirageEqualizer.ensureInit();

        int eqAuxSlot = MirageEqualizer.getAuxSlot();
        if (eqAuxSlot == 0) {
            System.out.println("[MirageGFBS] Equalizer aux slot is 0, effect not applied");
            return;
        }

        AL11.alSource3i(
                this.source,
                EXTEfx.AL_AUXILIARY_SEND_FILTER,
                eqAuxSlot,
                1,
                EXTEfx.AL_FILTER_NULL
        );

        int error = AL11.alGetError();
        if (error != AL11.AL_NO_ERROR) {
            System.err.println("[MirageGFBS] Failed to apply equalizer effect: " + error);
        } else {
            System.out.println("[MirageGFBS] Equalizer effect applied to broadcast sound (source=" + this.source + ", auxSlot=" + eqAuxSlot + ")");
        }

        mirage$gfbs_effectsApplied = true;
    }

    @Override
    public void mirage$gfbs_markAsBroadSystemSound() {
        this.mirage$gfbs_isBroadSystemSound = true;
    }
}
