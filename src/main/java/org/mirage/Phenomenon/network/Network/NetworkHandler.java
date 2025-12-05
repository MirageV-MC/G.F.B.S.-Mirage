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

package org.mirage.Phenomenon.network.Network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.mirage.Mirage_gfbs;

import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static SimpleChannel CHANNEL;
    private static int packetId = 0;

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(Mirage_gfbs.MODID, "network_system_miragev"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );

        CHANNEL.registerMessage(packetId++, EventPacket.class,
                EventPacket::encode,
                EventPacket::new,
                EventPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        Mirage_gfbs.LOGGER.info("Successfully registered network channel");
    }

    public static void sendToPlayer(ServerPlayer player, String eventId, CompoundTag data) {
        if (CHANNEL == null) {
            return;
        }

        try {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EventPacket(eventId, data));
        } catch (Exception e) {
            Mirage_gfbs.LOGGER.error("Error occurred while sending event to player: {}", eventId, e);
        }
    }

    public static void sendToAll(String eventId, CompoundTag data) {
        if (CHANNEL == null) {
            Mirage_gfbs.LOGGER.error("Network channel not initialized. Cannot send event: {}", eventId);
            return;
        }

        try {
            CHANNEL.send(PacketDistributor.ALL.noArg(), new EventPacket(eventId, data));
        } catch (Exception e) {
            Mirage_gfbs.LOGGER.error("Error occurred while sending event to all players: {}", eventId, e);
        }
    }

    public static void sendToAll(String eventId) {
        if (CHANNEL == null) {
            Mirage_gfbs.LOGGER.error("Network channel not initialized. Cannot send event: {}", eventId);
            return;
        }

        try {
            CHANNEL.send(PacketDistributor.ALL.noArg(), new EventPacket(eventId, new CompoundTag()));
        } catch (Exception e) {
            Mirage_gfbs.LOGGER.error("Error occurred while sending event to all players: {}", eventId, e);
        }
    }
}