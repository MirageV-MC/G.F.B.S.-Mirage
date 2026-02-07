package org.mirage.gfbs.Client.audio;

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

import net.minecraft.client.resources.sounds.SoundInstance;
import org.mirage.gfbs.advanced.broadsystem.BroadSystemSoundInstance;

public final class BroadSystemAudioMarker {
    private static final ThreadLocal<Boolean> IS_BROAD_SYSTEM_SOUND = ThreadLocal.withInitial(() -> false);

    private BroadSystemAudioMarker() {}

    public static void markAsBroadSystemSound() {
        IS_BROAD_SYSTEM_SOUND.set(true);
    }

    public static void clear() {
        IS_BROAD_SYSTEM_SOUND.set(false);
    }

    public static boolean isBroadSystemSound() {
        return IS_BROAD_SYSTEM_SOUND.get();
    }

    public static boolean isBroadSystemSound(SoundInstance soundInstance) {
        return soundInstance instanceof BroadSystemSoundInstance;
    }
}
