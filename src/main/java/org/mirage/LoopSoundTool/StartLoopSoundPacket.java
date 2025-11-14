/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Convex89524

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

package org.mirage.LoopSoundTool;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.mirage.Client.ClientSoundHandler;

import java.util.function.Supplier;

public class StartLoopSoundPacket {

    private final ResourceLocation soundId;
    private final SoundSource source;
    private final float volume;
    private final float pitch;
    private final float minVolume;

    public StartLoopSoundPacket(ResourceLocation soundId,
                                SoundSource source,
                                float volume,
                                float pitch,
                                float minVolume) {
        this.soundId = soundId;
        this.source = source;
        this.volume = volume;
        this.pitch = pitch;
        this.minVolume = minVolume;
    }

    public static void encode(StartLoopSoundPacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.soundId);
        buf.writeEnum(msg.source);
        buf.writeFloat(msg.volume);
        buf.writeFloat(msg.pitch);
        buf.writeFloat(msg.minVolume);
    }

    public static StartLoopSoundPacket decode(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        SoundSource source = buf.readEnum(SoundSource.class);
        float volume = buf.readFloat();
        float pitch = buf.readFloat();
        float minVolume = buf.readFloat();
        return new StartLoopSoundPacket(id, source, volume, pitch, minVolume);
    }

    public static void handle(StartLoopSoundPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () ->
                () -> ClientSoundHandler.startLoopSound(msg.soundId, msg.source, msg.volume, msg.pitch, msg.minVolume)
        ));
        ctx.get().setPacketHandled(true);
    }
}