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

package org.mirage.Sound;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.mirage.Client.ClientSoundHandler;

import java.util.function.Supplier;

public class StopLoopSoundPacket {

    private final ResourceLocation soundId;
    private final SoundSource source;

    public StopLoopSoundPacket(ResourceLocation soundId, SoundSource source) {
        this.soundId = soundId;
        this.source = source;
    }

    public static void encode(StopLoopSoundPacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.soundId);
        buf.writeEnum(msg.source);
    }

    public static StopLoopSoundPacket decode(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        SoundSource source = buf.readEnum(SoundSource.class);
        return new StopLoopSoundPacket(id, source);
    }

    public static void handle(StopLoopSoundPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () ->
                () -> ClientSoundHandler.stopLoopSound(msg.soundId, msg.source)
        ));
        ctx.get().setPacketHandled(true);
    }
}