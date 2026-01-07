package org.mirage.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.mirage.Tools.CountdownPopup.CountdownPopupS2CPacket;
import org.mirage.Tools.CountdownPopup.CountdownServerManager;
import org.mirage.Tools.CountdownPopup.CountdownStopS2CPacket;
import org.mirage.Tools.CountdownPopup.ModNetworking;

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
public final class CountdownAPI {

    private CountdownAPI() {}

    public static void popup(ServerPlayer player, String title, int min, int sec, int ms) {
        if (player == null) return;

        int m = Math.max(0, min);
        int s = Math.max(0, Math.min(59, sec));
        int cs = Math.max(0, Math.min(99, ms));

        long durationMs = (m * 60L + s) * 1000L + (cs * 10L);

        CountdownServerManager.prepare(player, durationMs);

        ModNetworking.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new CountdownPopupS2CPacket(title, m, s, cs)
        );
    }

    public static void startCountdown(ServerPlayer player) {
        CountdownServerManager.start(player);
    }

    public static void stop(ServerPlayer player) {
        CountdownServerManager.stop(player);

        if (player == null) return;
        ModNetworking.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new CountdownStopS2CPacket()
        );
    }
}
