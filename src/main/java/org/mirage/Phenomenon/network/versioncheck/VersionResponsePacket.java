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

package org.mirage.Phenomenon.network.versioncheck;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.mirage.ServerVersionCheckManager;

import java.util.function.Supplier;

public class VersionResponsePacket {

    private final String clientVersion;

    public VersionResponsePacket(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public static void encode(VersionResponsePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.clientVersion);
    }

    public static VersionResponsePacket decode(FriendlyByteBuf buf) {
        String version = buf.readUtf();
        return new VersionResponsePacket(version);
    }

    public static void handle(VersionResponsePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }

            ServerVersionCheckManager.onVersionResponse(player, msg.clientVersion);
        });

        ctx.setPacketHandled(true);
    }
}