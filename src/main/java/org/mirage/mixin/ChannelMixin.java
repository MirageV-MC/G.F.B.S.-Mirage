package org.mirage.mixin;

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
import org.mirage.Client.audio.MirageReverb;
import org.mirage.ClientConfig.GFBSClientConfigAPI;
import org.mirage.ClientConfig.instance.GFBSClientAudioConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Channel.class)
public abstract class ChannelMixin {

    @Shadow private int source;

    @Inject(method = "play", at = @At("HEAD"))
    private void mirage$gfbs_applyReverb(CallbackInfo ci) {
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
    }
}
