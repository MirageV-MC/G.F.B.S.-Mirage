/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.mirage.gfbs.advanced.broadsystem;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

import static org.mirage.gfbs.MirageGFBS.MODID;

public class BroadSystemNetwork {

    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "broadsystem"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(packetId++, StartBroadcastPacket.class,
                StartBroadcastPacket::encode, StartBroadcastPacket::decode,
                StartBroadcastPacket::handle);

        CHANNEL.registerMessage(packetId++, StopBroadcastPacket.class,
                StopBroadcastPacket::encode, StopBroadcastPacket::decode,
                StopBroadcastPacket::handle);
    }

    /**
     * 发送开始广播包到客户端
     */
    public static void sendStartBroadcast(ServerLevel level, BlockPos pos, ResourceLocation soundEvent,
                                          float volume, float pitch, long startTime) {
        StartBroadcastPacket packet = new StartBroadcastPacket(pos, soundEvent, volume, pitch, startTime);
        CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), packet);
    }

    /**
     * 发送停止广播包到客户端
     */
    public static void sendStopBroadcast(ServerLevel level, BlockPos pos) {
        StopBroadcastPacket packet = new StopBroadcastPacket(pos);
        CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), packet);
    }

    /**
     * 开始广播包
     */
    public static class StartBroadcastPacket {
        private final BlockPos pos;
        private final ResourceLocation soundEvent;
        private final float volume;
        private final float pitch;
        private final long serverStartTime;

        public StartBroadcastPacket(BlockPos pos, ResourceLocation soundEvent, float volume,
                                    float pitch, long serverStartTime) {
            this.pos = pos;
            this.soundEvent = soundEvent;
            this.volume = volume;
            this.pitch = pitch;
            this.serverStartTime = serverStartTime;
        }

        public static void encode(StartBroadcastPacket packet, FriendlyByteBuf buf) {
            buf.writeBlockPos(packet.pos);
            buf.writeResourceLocation(packet.soundEvent);
            buf.writeFloat(packet.volume);
            buf.writeFloat(packet.pitch);
            buf.writeLong(packet.serverStartTime);
        }

        public static StartBroadcastPacket decode(FriendlyByteBuf buf) {
            return new StartBroadcastPacket(
                    buf.readBlockPos(),
                    buf.readResourceLocation(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readLong()
            );
        }

        public static void handle(StartBroadcastPacket packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                // 客户端处理 - 播放声音
                BroadSystemClient.handleStartBroadcast(packet);
            });
            ctx.get().setPacketHandled(true);
        }

        public BlockPos getPos() {
            return pos;
        }

        public ResourceLocation getSoundEvent() {
            return soundEvent;
        }

        public float getVolume() {
            return volume;
        }

        public float getPitch() {
            return pitch;
        }

        public long getServerStartTime() {
            return serverStartTime;
        }
    }

    /**
     * 停止广播包
     */
    public static class StopBroadcastPacket {
        private final BlockPos pos;

        public StopBroadcastPacket(BlockPos pos) {
            this.pos = pos;
        }

        public static void encode(StopBroadcastPacket packet, FriendlyByteBuf buf) {
            buf.writeBlockPos(packet.pos);
        }

        public static StopBroadcastPacket decode(FriendlyByteBuf buf) {
            return new StopBroadcastPacket(buf.readBlockPos());
        }

        public static void handle(StopBroadcastPacket packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                // 客户端处理 - 停止声音
                BroadSystemClient.handleStopBroadcast(packet);
            });
            ctx.get().setPacketHandled(true);
        }

        public BlockPos getPos() {
            return pos;
        }
    }
}
