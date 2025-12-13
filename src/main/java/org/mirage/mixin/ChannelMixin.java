package org.mirage.mixin;

import com.mojang.blaze3d.audio.Channel;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.EXTEfx;
import org.mirage.Client.audio.MirageReverb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Channel.class)
public abstract class ChannelMixin {

    @Shadow private int source;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void mirage$attachGlobalReverb(int sourceId, CallbackInfo ci) {
        MirageReverb.ensureInitialized();
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
