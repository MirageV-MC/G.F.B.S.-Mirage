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

package org.mirage.Phenomenon.network.Notification;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.mirage.Mirage_gfbs;

public final class PacketHandler {

    private static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel INSTANCE = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(Mirage_gfbs.MODID, "notifications"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static boolean registered = false;
    private static int packetId = 0;

    private PacketHandler() {}

    public static void register() {
        if (registered) return;
        registered = true;

        INSTANCE.messageBuilder(NotificationPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(NotificationPacket::toBytes)
                .decoder(NotificationPacket::new)
                .consumerMainThread(NotificationPacket::handle)
                .add();

        Mirage_gfbs.LOGGER.info("Registered notification network channel: {}",
                new ResourceLocation(Mirage_gfbs.MODID, "notifications"));
    }

    public static void sendToPlayer(NotificationPacket packet, ServerPlayer player) {
        INSTANCE.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
}
