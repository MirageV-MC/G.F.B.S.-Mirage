package org.mirage.Client.ClientShake;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC
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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Camera;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.mirage.Mirage_gfbs.MODID;

public class ShakeQsClient {
    private static final String PROTOCOL = "1";
    private static SimpleChannel CHANNEL = null; // 延迟初始化
    private static final AtomicInteger PACKET_ID = new AtomicInteger(0);

    private static volatile float currentVal = 0f;

    private static volatile Tween tween = null;

    private static final RandomSource RAND = RandomSource.create();

    private static final Set<String> SCREEN_NAME_BLOCKLIST = Set.of(
            "CameraGui", "CameraHint", "NPCDiag", "Term"
    );

    private static final RandomSource FRAME_RAND = RandomSource.createNewThreadLocalInstance();

    public ShakeQsClient() {
        // 初始化网络通道
        initializeNetwork();
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientOnly::init);
    }

    private static void initializeNetwork() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(MODID, "shake_qs_net"),
                () -> PROTOCOL,
                PROTOCOL::equals,
                PROTOCOL::equals
        );

        CHANNEL.registerMessage(
                PACKET_ID.getAndIncrement(),
                ShakePacket.class,
                ShakePacket::encode,
                ShakePacket::decode,
                ShakePacket::handle
        );
    }

    public static void sendShake(ServerPlayer player, float value, float timeSec) {
        float t = timeSec;
        if (t <= 0f) t = -1f;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ShakePacket(value, t));
    }

    private static final class ClientOnly {
        static void init() {
            MinecraftForge.EVENT_BUS.register(ClientOnly.class);
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;

            updateTween();
        }

        private static void updateTween() {
            Tween t = tween;
            if (t == null) return;

            long now = System.nanoTime();
            float v = t.sample(now);

            currentVal = v;

            if (t.isFinished(now)) {
                Tween next = t.next;
                tween = next;
            }
        }
    }

    public static void applyShake(Camera camera, float partialTick) {
        float val = currentVal;
        if (val <= 0f) return;

        if (isBlockedByGui()) return;

        float rxDeg = ((FRAME_RAND.nextFloat() - 0.5f) * val / 200.0f);
        float ryDeg = ((FRAME_RAND.nextFloat() - 0.5f) * val / 200.0f);
        float rzDeg = ((FRAME_RAND.nextFloat() - 0.5f) * val / 100.0f);

        float dx = ((FRAME_RAND.nextFloat() - 0.5f) * val / 300.0f);
        float dy = ((FRAME_RAND.nextFloat() - 0.5f) * val / 300.0f);
        float dz = ((FRAME_RAND.nextFloat() - 0.5f) * val / 300.0f);

        float newPitch = camera.getXRot() + rxDeg;
        float newYaw   = camera.getYRot() + ryDeg;

        Vec3 p = camera.getPosition();
        Vec3 newPos = p.add(dx, dy, dz);

        ICameraPublicAccess cam_mixin = (ICameraPublicAccess) camera;

        cam_mixin.mirage$setRotationPublic(newYaw, newPitch);
        cam_mixin.mirage$setPositionPublic(newPos.x, newPos.y, newPos.z);

        applyRoll(camera, rzDeg);
    }

    public static void applyRoll(Camera camera, float rollDeltaDeg) {
    }

    private static boolean isBlockedByGui() {
        Minecraft mc = Minecraft.getInstance();
        Screen s = mc.screen;
        if (s == null) return false;

        String simpleName = s.getClass().getSimpleName();
        return SCREEN_NAME_BLOCKLIST.contains(simpleName);
    }

    private static void triggerShakeClient(float value, float timeSecOrNeg1ForRandom) {
        float time = timeSecOrNeg1ForRandom;
        if (time <= 0f) {
            time = 2.0f + RAND.nextFloat();
        }

        long now = System.nanoTime();
        Tween a = new Tween(currentVal, value, now, 0.2f);
        Tween b = new Tween(value, 0f, now + secondsToNanos(0.2f), time);
        a.next = b;
        tween = a;
    }

    private static long secondsToNanos(float sec) {
        return (long) (sec * 1_000_000_000L);
    }

    private static final class Tween {
        final float from;
        final float to;
        final long startNs;
        final long durationNs;
        volatile Tween next;

        Tween(float from, float to, long startNs, float durationSec) {
            this.from = from;
            this.to = to;
            this.startNs = startNs;
            this.durationNs = Math.max(1L, secondsToNanos(durationSec));
        }

        float sample(long nowNs) {
            float t = (nowNs - startNs) / (float) durationNs;
            t = Mth.clamp(t, 0f, 1f);
            return Mth.lerp(t, from, to);
        }

        boolean isFinished(long nowNs) {
            return nowNs >= (startNs + durationNs);
        }
    }

    private record ShakePacket(float value, float timeSecOrNeg1) {
        static void encode(ShakePacket msg, FriendlyByteBuf buf) {
            buf.writeFloat(msg.value);
            buf.writeFloat(msg.timeSecOrNeg1);
        }

        static ShakePacket decode(FriendlyByteBuf buf) {
            float v = buf.readFloat();
            float t = buf.readFloat();
            return new ShakePacket(v, t);
        }

        static void handle(ShakePacket msg, Supplier<NetworkEvent.Context> ctxSup) {
            NetworkEvent.Context ctx = ctxSup.get();
            if (ctx.getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
                ctx.setPacketHandled(true);
                return;
            }

            ctx.enqueueWork(() -> {
                triggerShakeClient(msg.value, msg.timeSecOrNeg1);
            });
            ctx.setPacketHandled(true);
        }
    }

    public static final class ShakeFrame {
        public final float rxDeg, ryDeg;
        public final float rzRad;
        public final double dx, dy, dz;

        public ShakeFrame(float rxDeg, float ryDeg, float rzRad, double dx, double dy, double dz) {
            this.rxDeg = rxDeg;
            this.ryDeg = ryDeg;
            this.rzRad = rzRad;
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
        }
    }

    public static ShakeFrame computeShakeFrame() {
        float val = currentVal;
        if (val <= 0f) return null;
        if (isBlockedByGui()) return null;

        float rxDeg = ((FRAME_RAND.nextFloat() - 0.5f) * val / 200.0f);
        float ryDeg = ((FRAME_RAND.nextFloat() - 0.5f) * val / 200.0f);
        float rzDeg = ((FRAME_RAND.nextFloat() - 0.5f) * val / 100.0f);

        double dx = ((FRAME_RAND.nextFloat() - 0.5f) * val / 300.0f);
        double dy = ((FRAME_RAND.nextFloat() - 0.5f) * val / 300.0f);
        double dz = ((FRAME_RAND.nextFloat() - 0.5f) * val / 300.0f);

        float rzRad = (float) Math.toRadians(rzDeg);

        return new ShakeFrame(rxDeg, ryDeg, rzRad, dx, dy, dz);
    }
}
