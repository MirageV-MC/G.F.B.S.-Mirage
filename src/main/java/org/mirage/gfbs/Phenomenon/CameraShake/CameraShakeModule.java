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

package org.mirage.gfbs.Phenomenon.CameraShake;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.mirage.gfbs.Client.ClientShake.ClientShakeHandler;
import org.mirage.gfbs.Client.ClientShake.ShakeManager;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = "mirage_gfbs", bus = Mod.EventBusSubscriber.Bus.MOD)
public class CameraShakeModule {
    private static final String PROTOCOL_VERSION = "1";
    private static SimpleChannel CHANNEL;

    // 网络包注册
    public static void registerNetwork(FMLCommonSetupEvent event) {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation("mirage_gfbs", "camera_shake"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );

        CHANNEL.registerMessage(0, ShakePacket.class,
                ShakePacket::encode,
                ShakePacket::new,
                CameraShakeModule::handleShakePacket);

        CHANNEL.registerMessage(1, ShakeStopPacket.class,
                ShakeStopPacket::encode,
                ShakeStopPacket::new,
                CameraShakeModule::handleShakeStopPacket);
    }

    public static void sendShakeCommand(ServerPlayer player, float speed, float maxAmplitude, int duration, int riseTime, int fallTime) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ShakePacket(speed, maxAmplitude, duration, riseTime, fallTime));
    }

    private static void handleShakePacket(ShakePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ShakeManager.startShake(packet.speed, packet.maxAmplitude,
                    packet.duration, packet.riseTime, packet.fallTime);
        });
        ctx.get().setPacketHandled(true);
    }

    public static void sendShakeStopCommand(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ShakeStopPacket());
    }
    private static void handleShakeStopPacket(ShakeStopPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientShakeHandler.resetShake();
        });
        ctx.get().setPacketHandled(true);
    }

    public static class ShakeStopPacket {
        public ShakeStopPacket() {}
        public ShakeStopPacket(FriendlyByteBuf buf) {}
        public void encode(FriendlyByteBuf buf) {}
    }

    public static class ShakePacket {
        public final float speed;
        public final float maxAmplitude;
        public final int duration;
        public final int riseTime;
        public final int fallTime;

        public ShakePacket(float speed, float maxAmplitude, int duration, int riseTime, int fallTime) {
            this.speed = speed;
            this.maxAmplitude = maxAmplitude;
            this.duration = duration;
            this.riseTime = riseTime;
            this.fallTime = fallTime;
        }

        public ShakePacket(FriendlyByteBuf buf) {
            this.speed = buf.readFloat();
            this.maxAmplitude = buf.readFloat();
            this.duration = buf.readInt();
            this.riseTime = buf.readInt();
            this.fallTime = buf.readInt();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeFloat(speed);
            buf.writeFloat(maxAmplitude);
            buf.writeInt(duration);
            buf.writeInt(riseTime);
            buf.writeInt(fallTime);
        }
    }
}