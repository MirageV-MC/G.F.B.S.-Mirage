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

package org.mirage.Phenomenon.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.mirage.Mirage_gfbs;

import java.util.function.Supplier;

public class GlobalSoundPlayer {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Mirage_gfbs.MODID, "global_sound"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void registerNetworkMessages() {
        int packetId = 0;
        CHANNEL.registerMessage(packetId++, SoundPacket.class,
                SoundPacket::encode,
                SoundPacket::decode,
                GlobalSoundPlayer::handleSoundPacket
        );
    }

    private static void handleSoundPacket(SoundPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(packet.soundId);
            if (sound != null) {
                if (ctx.get().getDirection().getReceptionSide().isClient()) {
                    LocalPlayer player = Minecraft.getInstance().player;
                    if (player != null) {
                        Vec3 pos = player.position();
                        player.level().playLocalSound(
                                pos.x, pos.y, pos.z,
                                sound, packet.soundSource,
                                packet.volume, 1.0f, false
                        );
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void playToAllClients(ServerPlayer sender, ResourceLocation soundId, SoundSource soundSource, float volume) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new SoundPacket(soundId, soundSource, volume));
    }

    public static class SoundPacket {
        public final ResourceLocation soundId;
        public final SoundSource soundSource;  // 新增字段
        public final float volume;

        public SoundPacket(ResourceLocation soundId, SoundSource soundSource, float volume) {
            this.soundId = soundId;
            this.soundSource = soundSource;
            this.volume = volume;
        }

        public void encode(FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(soundId);
            buffer.writeEnum(soundSource);  // 写入枚举值
            buffer.writeFloat(volume);
        }

        public static SoundPacket decode(FriendlyByteBuf buffer) {
            return new SoundPacket(
                    buffer.readResourceLocation(),
                    buffer.readEnum(SoundSource.class),  // 读取枚举值
                    buffer.readFloat()
            );
        }
    }
}