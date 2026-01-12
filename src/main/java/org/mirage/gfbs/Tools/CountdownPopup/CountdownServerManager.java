package org.mirage.gfbs.Tools.CountdownPopup;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

public final class CountdownServerManager {

    private static final Map<UUID, Entry> ACTIVE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> PREPARED_DURATION_MS = new ConcurrentHashMap<>();
    private static volatile boolean REGISTERED = false;

    private CountdownServerManager() {}

    private static void ensureRegistered() {
        if (REGISTERED) return;
        REGISTERED = true;
        MinecraftForge.EVENT_BUS.register(ForgeEvents.class);
    }

    public static void prepare(ServerPlayer player, long durationMs) {
        if (player == null) return;
        ensureRegistered();
        PREPARED_DURATION_MS.put(player.getUUID(), Math.max(0L, durationMs));
        CountdownEndHooks.reset(player);
    }

    public static long start(ServerPlayer player) {
        if (player == null) return -1L;
        ensureRegistered();

        long durationMs = PREPARED_DURATION_MS.getOrDefault(player.getUUID(), 0L);
        ServerLevel level = player.serverLevel();

        long ticks = (durationMs + 49L) / 50L; // ceil
        long endGameTime = level.getGameTime() + ticks;

        ACTIVE.put(player.getUUID(), new Entry(endGameTime));

        ModNetworking.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new CountdownStartS2CPacket(endGameTime)
        );

        return endGameTime;
    }

    public static void stop(ServerPlayer player) {
        if (player == null) return;
        ensureRegistered();
        ACTIVE.remove(player.getUUID());
        PREPARED_DURATION_MS.remove(player.getUUID());
        CountdownEndHooks.reset(player);
    }

    private static void onTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Entry e = ACTIVE.get(player.getUUID());
            if (e == null) continue;

            ServerLevel level = player.serverLevel();
            long now = level.getGameTime();

            if (now >= e.endGameTime) {
                CountdownEndHooks.fire(player);

                ModNetworking.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new CountdownEndS2CPacket()
                );

                ACTIVE.remove(player.getUUID());
                PREPARED_DURATION_MS.remove(player.getUUID());
            }
        }
    }

    private static final class Entry {
        final long endGameTime;
        Entry(long endGameTime) {
            this.endGameTime = endGameTime;
        }
    }

    public static final class ForgeEvents {
        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            if (e.getServer() == null) return;
            onTick(e.getServer());
        }
    }
}
