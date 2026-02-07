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

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.registries.ForgeRegistries;
import org.mirage.gfbs.Client.audio.BroadSystemAudioMarker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BroadSystemClient {
    private static final Map<BlockPos, SoundInstance> activeSounds = new ConcurrentHashMap<>();

    public static void handleStartBroadcast(BroadSystemNetwork.StartBroadcastPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        BlockPos pos = packet.getPos();
        ResourceLocation soundEventId = packet.getSoundEvent();
        float volume = packet.getVolume();
        float pitch = packet.getPitch();
        long serverStartTime = packet.getServerStartTime();

        SoundEvent soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(soundEventId);
        if (soundEvent == null) {
            System.err.println("[BroadSystem] Unknown sound event: " + soundEventId);
            return;
        }

        SoundInstance oldSound = activeSounds.remove(pos);
        if (oldSound != null) {
            minecraft.getSoundManager().stop(oldSound);
        }

        long clientTime = System.currentTimeMillis();
        long timeOffset = clientTime - serverStartTime;

        SoundInstance soundInstance = new BroadSystemSoundInstance(
                soundEvent,
                SoundSource.BLOCKS,
                volume,
                pitch,
                pos,
                timeOffset
        );

        activeSounds.put(pos, soundInstance);

        BroadSystemAudioMarker.markAsBroadSystemSound();
        minecraft.getSoundManager().play(soundInstance);
    }

    public static void handleStopBroadcast(BroadSystemNetwork.StopBroadcastPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        BlockPos pos = packet.getPos();

        SoundInstance soundInstance = activeSounds.remove(pos);
        if (soundInstance != null) {
            minecraft.getSoundManager().stop(soundInstance);
        }
    }

    public static void clearAllSounds() {
        Minecraft minecraft = Minecraft.getInstance();
        for (SoundInstance soundInstance : activeSounds.values()) {
            minecraft.getSoundManager().stop(soundInstance);
        }
        activeSounds.clear();
    }
}
