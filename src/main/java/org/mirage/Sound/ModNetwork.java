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

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

import static org.mirage.Mirage_gfbs.MODID;

public class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "mirage"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static int nextId() {
        return packetId++;
    }

    public static void register() {
        CHANNEL.registerMessage(
                nextId(),
                StartLoopSoundPacket.class,
                StartLoopSoundPacket::encode,
                StartLoopSoundPacket::decode,
                StartLoopSoundPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                nextId(),
                StopLoopSoundPacket.class,
                StopLoopSoundPacket::encode,
                StopLoopSoundPacket::decode,
                StopLoopSoundPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    /** 发给单个玩家：开始循环播放声音 */
    public static void sendStartLoopSound(StartLoopSoundPacket msg, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    /** 发给单个玩家：停止循环播放声音 */
    public static void sendStopLoopSound(StopLoopSoundPacket msg, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }
}