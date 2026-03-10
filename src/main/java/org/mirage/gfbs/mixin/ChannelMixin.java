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
import org.mirage.gfbs.Client.audio.MirageBroadSystemReverb;
import org.mirage.gfbs.Client.audio.MirageDistortion;
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

    @Inject(method = "play", at = @At("RETURN"))
    private void mirage$gfbs_applyAudioEffects(CallbackInfo ci) {
        if (BroadSystemAudioMarker.isBroadSystemSound()) {
            this.mirage$gfbs_isBroadSystemSound = true;
            BroadSystemAudioMarker.clear();
        }

        applyBroadSystemReverbEffect();
        applyReverbEffect();
        applyDistortionEffect();
        applyEqualizerEffect();
    }

    private void applyBroadSystemReverbEffect() {
        if (!mirage$gfbs_isBroadSystemSound) {
            return;
        }

        if (!GFBSClientConfigAPI.get(GFBSClientAudioConfig.ENABLE_BROAD_SYSTEM_REVERB)) {
            return;
        }

        MirageBroadSystemReverb.ensureInit();

        if (!MirageBroadSystemReverb.isSupported()) {
            return;
        }

        int auxSlot = MirageBroadSystemReverb.getAuxSlot();
        if (auxSlot == 0) {
            return;
        }

        MirageBroadSystemReverb.setRoomSize(GFBSClientConfigAPI.get(GFBSClientAudioConfig.BROAD_SYSTEM_REVERB_ROOM_SIZE).floatValue());
        MirageBroadSystemReverb.setPreDelay(GFBSClientConfigAPI.get(GFBSClientAudioConfig.BROAD_SYSTEM_REVERB_PRE_DELAY).floatValue());
        MirageBroadSystemReverb.setReverbFeel(GFBSClientConfigAPI.get(GFBSClientAudioConfig.BROAD_SYSTEM_REVERB_REVERB_FEEL).floatValue());
        MirageBroadSystemReverb.setDamping(GFBSClientConfigAPI.get(GFBSClientAudioConfig.BROAD_SYSTEM_REVERB_DAMPING).floatValue());
        MirageBroadSystemReverb.setLowTone(GFBSClientConfigAPI.get(GFBSClientAudioConfig.BROAD_SYSTEM_REVERB_LOW_TONE).floatValue());
        MirageBroadSystemReverb.setHighTone(GFBSClientConfigAPI.get(GFBSClientAudioConfig.BROAD_SYSTEM_REVERB_HIGH_TONE).floatValue());
        MirageBroadSystemReverb.setWetGain(GFBSClientConfigAPI.get(GFBSClientAudioConfig.BROAD_SYSTEM_REVERB_WET_GAIN).floatValue());
        MirageBroadSystemReverb.setDryGain(GFBSClientConfigAPI.get(GFBSClientAudioConfig.BROAD_SYSTEM_REVERB_DRY_GAIN).floatValue());
        MirageBroadSystemReverb.setStereoWidth(GFBSClientConfigAPI.get(GFBSClientAudioConfig.BROAD_SYSTEM_REVERB_STEREO_WIDTH).floatValue());

        AL11.alSource3i(
                this.source,
                EXTEfx.AL_AUXILIARY_SEND_FILTER,
                auxSlot,
                0,
                EXTEfx.AL_FILTER_NULL
        );

        AL11.alSourcei(this.source, EXTEfx.AL_AUXILIARY_SEND_FILTER_GAIN_AUTO, AL11.AL_TRUE);
        AL11.alSourcei(this.source, EXTEfx.AL_AUXILIARY_SEND_FILTER_GAINHF_AUTO, AL11.AL_TRUE);

        int error = AL11.alGetError();
        if (error != AL11.AL_NO_ERROR) {
            System.err.println("[MirageGFBS] Failed to apply broad system reverb effect: " + error);
        } else {
            System.out.println("[MirageGFBS] Broad system reverb effect applied to broadcast sound (source=" + this.source + ", auxSlot=" + auxSlot + ")");
        }
    }

    private void applyReverbEffect() {
        if (mirage$gfbs_isBroadSystemSound && GFBSClientConfigAPI.get(GFBSClientAudioConfig.ENABLE_BROAD_SYSTEM_REVERB)) {
            return;
        }

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

    private void applyDistortionEffect() {
        if (!mirage$gfbs_isBroadSystemSound) {
            return;
        }

        if (!GFBSClientConfigAPI.get(GFBSClientAudioConfig.ENABLE_DISTORTION)) {
            return;
        }

        MirageDistortion.ensureInit();

        int distortionAuxSlot = MirageDistortion.getAuxSlot();
        if (distortionAuxSlot == 0) {
            return;
        }

        // Determine target send index based on reverb settings
        // If reverb is enabled (either broad system or general), try to use index 2 to coexist
        // Otherwise use index 0
        int targetSendIndex = 0;
        if (GFBSClientConfigAPI.get(GFBSClientAudioConfig.ENABLE_BROAD_SYSTEM_REVERB) || 
            GFBSClientConfigAPI.get(GFBSClientAudioConfig.ENABLE_REVERB)) {
            targetSendIndex = 2;
        }

        // Clear any previous errors
        AL11.alGetError();

        AL11.alSource3i(
                this.source,
                EXTEfx.AL_AUXILIARY_SEND_FILTER,
                distortionAuxSlot,
                targetSendIndex,
                EXTEfx.AL_FILTER_NULL
        );

        int error = AL11.alGetError();
        int actualSendIndex = targetSendIndex;
        
        if (error != AL11.AL_NO_ERROR) {
            // If using index 2 failed (likely due to hardware limits), fallback to index 0
            // This will override reverb effect but ensures distortion is applied as requested
            if (targetSendIndex == 2) {
                System.out.println("[MirageGFBS] Failed to apply distortion on send index 2, falling back to index 0 (overriding reverb)");
                
                AL11.alSource3i(
                        this.source,
                        EXTEfx.AL_AUXILIARY_SEND_FILTER,
                        distortionAuxSlot,
                        0,
                        EXTEfx.AL_FILTER_NULL
                );
                
                error = AL11.alGetError();
                actualSendIndex = 0;
            }
        }
    }

    private void applyEqualizerEffect() {
        if (!mirage$gfbs_isBroadSystemSound) {
            return;
        }

        MirageEqualizer.ensureInit();

        int eqAuxSlot = MirageEqualizer.getAuxSlot();
        if (eqAuxSlot == 0) {
            return;
        }

        AL11.alSource3i(
                this.source,
                EXTEfx.AL_AUXILIARY_SEND_FILTER,
                eqAuxSlot,
                1,
                EXTEfx.AL_FILTER_NULL
        );

        mirage$gfbs_effectsApplied = true;
    }

    @Override
    public void mirage$gfbs_markAsBroadSystemSound() {
        this.mirage$gfbs_isBroadSystemSound = true;
    }
}
