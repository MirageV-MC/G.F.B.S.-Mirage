package org.mirage.Phenomenon.network.Network;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Convex89524
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.network.simple.SimpleChannel;
import org.mirage.Mirage_gfbs;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ClientToServer {

    public static final String MOD_ID = Mirage_gfbs.MODID;

    private static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MOD_ID, "mirage_c2s"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static final Map<ResourceLocation, BiConsumer<ServerPlayer, FriendlyByteBuf>> ACTIONS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Function<FriendlyByteBuf, ?>> DATA_DESERIALIZERS = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<ResourceLocation, Long>> COOLDOWN = new ConcurrentHashMap<>();

    private static volatile long DEFAULT_COOLDOWN_MILLIS = 0L;

    private static boolean REGISTERED = false;

    private ClientToServer() {}

    public static void registerChannel() {
        if (REGISTERED) return;
        REGISTERED = true;

        CHANNEL.registerMessage(
                0,
                Packet.class,
                Packet::encode,
                Packet::decode,
                ClientToServer::handlePacket
        );
    }

    public static void setDefaultCooldownMillis(long millis) {
        DEFAULT_COOLDOWN_MILLIS = Math.max(0L, millis);
    }

    public static void run(String name, Consumer<ServerPlayer> serverAction) {
        ResourceLocation id = new ResourceLocation(MOD_ID, name);

        ACTIONS.putIfAbsent(id, (player, data) -> serverAction.accept(player));

        if (EffectiveSide.get() == LogicalSide.SERVER) {
            return;
        }

        CHANNEL.sendToServer(new Packet(id, null));
    }

    public static <T> void runWithData(String name, T data, BiConsumer<ServerPlayer, T> serverAction,
                                       BiConsumer<FriendlyByteBuf, T> dataSerializer,
                                       Function<FriendlyByteBuf, T> dataDeserializer) {
        ResourceLocation id = new ResourceLocation(MOD_ID, name);

        DATA_DESERIALIZERS.put(id, dataDeserializer);

        ACTIONS.putIfAbsent(id, (player, buf) -> {
            T deserializedData = (T) dataDeserializer.apply(buf);
            serverAction.accept(player, deserializedData);
        });

        if (EffectiveSide.get() == LogicalSide.SERVER) {
            return;
        }

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        dataSerializer.accept(buffer, data);
        CHANNEL.sendToServer(new Packet(id, buffer));
    }

    public static void runWithString(String name, String data, BiConsumer<ServerPlayer, String> serverAction) {
        runWithData(name, data, serverAction,
                (buf, str) -> buf.writeUtf(str),
                FriendlyByteBuf::readUtf
        );
    }

    public static void runWithInt(String name, int data, BiConsumer<ServerPlayer, Integer> serverAction) {
        runWithData(name, data, serverAction,
                (buf, integer) -> buf.writeInt(integer),
                FriendlyByteBuf::readInt
        );
    }

    public static void runWithDouble(String name, double data, BiConsumer<ServerPlayer, Double> serverAction) {
        runWithData(name, data, serverAction,
                (buf, d) -> buf.writeDouble(d),
                FriendlyByteBuf::readDouble
        );
    }

    public static void runWithBoolean(String name, boolean data, BiConsumer<ServerPlayer, Boolean> serverAction) {
        runWithData(name, data, serverAction,
                (buf, b) -> buf.writeBoolean(b),
                FriendlyByteBuf::readBoolean
        );
    }

    public static void runWithPosition(String name, double x, double y, double z,
                                       BiConsumer<ServerPlayer, double[]> serverAction) {
        double[] position = {x, y, z};
        runWithData(name, position, serverAction,
                (buf, pos) -> {
                    buf.writeDouble(pos[0]);
                    buf.writeDouble(pos[1]);
                    buf.writeDouble(pos[2]);
                },
                buf -> new double[]{buf.readDouble(), buf.readDouble(), buf.readDouble()}
        );
    }

    private static void handlePacket(Packet msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            try {
                ServerPlayer player = ctx.getSender();
                if (player == null) return;

                BiConsumer<ServerPlayer, FriendlyByteBuf> action = ACTIONS.get(msg.id());
                if (action == null) return;

                if (!checkCooldown(player, msg.id())) return;

                action.accept(player, msg.data());
            } catch (Exception e) {
                Mirage_gfbs.LOGGER.error("{}{}", e + "\n", Arrays.toString(e.getStackTrace()));
            }
        });
        ctx.setPacketHandled(true);
    }

    private static boolean checkCooldown(ServerPlayer player, ResourceLocation id) {
        long cd = DEFAULT_COOLDOWN_MILLIS;
        if (cd <= 0) return true;

        long now = System.currentTimeMillis();
        UUID uuid = player.getUUID();

        Map<ResourceLocation, Long> map =
                COOLDOWN.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>());

        Long last = map.get(id);
        if (last != null && now - last < cd) {
            return false;
        }

        map.put(id, now);
        return true;
    }

    private record Packet(ResourceLocation id, FriendlyByteBuf data) {
        private static void encode(Packet msg, FriendlyByteBuf buf) {
            buf.writeResourceLocation(msg.id);
            if (msg.data != null) {
                buf.writeByteArray(msg.data.array());
            } else {
                buf.writeByteArray(new byte[0]);
            }
        }

        private static Packet decode(FriendlyByteBuf buf) {
            ResourceLocation id = buf.readResourceLocation();
            byte[] dataArray = buf.readByteArray();
            FriendlyByteBuf dataBuf = null;
            if (dataArray.length > 0) {
                dataBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(dataArray));
            }
            return new Packet(id, dataBuf);
        }
    }
}