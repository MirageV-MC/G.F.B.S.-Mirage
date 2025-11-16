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

package org.mirage.Phenomenon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.mirage.Mirage_gfbs;
import org.mirage.Tools.HexCrackerUI;

import java.util.function.Supplier;

public class HexCrackerNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Mirage_gfbs.MODID, "mirage_hexcracker"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(
                packetId++,
                HexCrackerStartMessage.class,
                HexCrackerStartMessage::encode,
                HexCrackerStartMessage::decode,
                HexCrackerStartMessage::handle
        );

        CHANNEL.registerMessage(
                packetId++,
                StopHexCrackerMessage.class,
                StopHexCrackerMessage::encode,
                StopHexCrackerMessage::decode,
                StopHexCrackerMessage::handle
        );
    }

    public static void triggerOnAll(MinecraftServer server) {
        if (server == null) return;
        CHANNEL.send(PacketDistributor.ALL.noArg(), new HexCrackerStartMessage());
    }

    public static void stopOnAll(MinecraftServer server) {
        if (server != null)
            CHANNEL.send(PacketDistributor.ALL.noArg(), new StopHexCrackerMessage());
    }

    public static class HexCrackerStartMessage {

        public HexCrackerStartMessage() {
        }

        public static void encode(HexCrackerStartMessage msg, FriendlyByteBuf buf) {
        }

        public static HexCrackerStartMessage decode(FriendlyByteBuf buf) {
            return new HexCrackerStartMessage();
        }

        public static void handle(HexCrackerStartMessage msg,
                                  Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();

            if (ctx.getDirection().getReceptionSide().isClient()) {
                ctx.enqueueWork(() -> {
                    HexCrackerUI.start();
                });
            }

            ctx.setPacketHandled(true);
        }
    }

    public static class StopHexCrackerMessage {

        public StopHexCrackerMessage() {}

        public static void encode(StopHexCrackerMessage msg, FriendlyByteBuf buf) {}

        public static StopHexCrackerMessage decode(FriendlyByteBuf buf) {
            return new StopHexCrackerMessage();
        }

        public static void handle(StopHexCrackerMessage msg,
                                  Supplier<NetworkEvent.Context> ctxSupplier) {

            NetworkEvent.Context ctx = ctxSupplier.get();

            if (ctx.getDirection().getReceptionSide().isClient()) {
                ctx.enqueueWork(() -> HexCrackerUI.stop());
            }

            ctx.setPacketHandled(true);
        }
    }

}