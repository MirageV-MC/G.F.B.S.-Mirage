package org.mirage.gfbs.mixin;

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
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import com.mojang.blaze3d.audio.Channel;
import org.mirage.gfbs.accessor.ChannelMixinAccessor;
import org.mirage.gfbs.advanced.broadsystem.BroadSystemSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {

    private static final int MAX_SOURCES_LIMIT = 4096;

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 255), require = 0)
    private int mirage$gfbs_modifyMaxSources255(int original) {
        return MAX_SOURCES_LIMIT;
    }

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 8), require = 0)
    private int mirage$gfbs_modifyMaxSources8(int original) {
        return MAX_SOURCES_LIMIT;
    }

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 247), require = 0)
    private int mirage$gfbs_modifyMaxSources247(int original) {
        return MAX_SOURCES_LIMIT;
    }

    @Redirect(method = "play", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/ChannelAccess$ChannelHandle;execute(Ljava/util/function/Consumer;)V"))
    private void mirage$gfbs_redirectExecute(ChannelAccess.ChannelHandle instance, Consumer<Channel> action, SoundInstance sound) {
        if (sound instanceof BroadSystemSoundInstance) {
            instance.execute(channel -> {
                ((ChannelMixinAccessor) channel).mirage$gfbs_markAsBroadSystemSound();
            });
        }
        instance.execute(action);
    }
}
