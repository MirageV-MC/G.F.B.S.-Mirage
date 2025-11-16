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

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;
import org.mirage.Mirage_gfbs;

import java.util.function.Supplier;

public class VersionRequestPacket {

    public VersionRequestPacket() {
    }

    public static void encode(VersionRequestPacket msg, FriendlyByteBuf buf) {
    }

    public static VersionRequestPacket decode(FriendlyByteBuf buf) {
        return new VersionRequestPacket();
    }

    public static void handle(VersionRequestPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            String clientVersion = ModList.get()
                    .getModContainerById(Mirage_gfbs.MODID)
                    .map(c -> c.getModInfo().getVersion().toString())
                    .orElse("unknown");

            NetworkHandler.CHANNEL.sendToServer(new VersionResponsePacket(clientVersion));
        });

        ctx.setPacketHandled(true);
    }
}