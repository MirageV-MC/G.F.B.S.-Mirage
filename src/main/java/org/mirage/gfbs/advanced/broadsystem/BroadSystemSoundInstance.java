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

package org.mirage.gfbs.advanced.broadsystem;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class BroadSystemSoundInstance extends AbstractSoundInstance {

    private final long timeOffset;

    public BroadSystemSoundInstance(SoundEvent soundEvent, SoundSource source, float volume, float pitch,
                                    BlockPos pos, long timeOffset) {
        super(soundEvent, source, RandomSource.create());
        this.volume = volume;
        this.pitch = pitch;
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
        this.timeOffset = timeOffset;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
    }

    public long getTimeOffset() {
        return timeOffset;
    }

    public static boolean isBroadSystemSound(SoundInstance soundInstance) {
        return soundInstance instanceof BroadSystemSoundInstance;
    }
}
