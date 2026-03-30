/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package org.mirage.gfbs.Event;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.mirage.gfbs.Client.ExposureController;
import org.mirage.gfbs.Command.CameraShakeCommand;
import org.mirage.gfbs.Command.FluorescentTubeCommandRegistry;
import org.mirage.gfbs.Command.MirageGFBsEventCommand;
import org.mirage.gfbs.Command.NotificationCommand;
import org.mirage.gfbs.Phenomenon.network.Network.ClientEventHandler;
import org.mirage.gfbs.Phenomenon.network.Network.NetworkHandler;
import org.mirage.gfbs.Tools.Task;
import org.mirage.gfbs.auralis.api.AuralisServerApi;

import java.awt.*;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.mirage.gfbs.CommandExecutor.executeCommandAsync;

public class BlackHole {
    private static ServerLevel _serverLevel;

    public static final String MUSIC_ID = "blackhole_e_music";
    public static final String BROADCAST_ID = "blackhole_e_playing_broadcast";
    public static final String ALARM2_ID = "blackhole_e_playing_alarm2";

    private static final AtomicLong SFX_SEQUENCE = new AtomicLong();

    public static void execute(MirageGFBsEventCommand.CommandContext context) {
        _serverLevel = context.getSource().getLevel();

        Task.spawn(() -> {
            execute_s(context);
        });
    }

    public static void execute_s(MirageGFBsEventCommand.CommandContext context) {
        CommandSourceStack source = context.getSource();
        Collection<ServerPlayer> allPlayers = source.getServer().getPlayerList().getPlayers();

        FluorescentTubeCommandRegistry.turnOnAllTubes(_serverLevel);
        FluorescentTubeCommandRegistry.setInstabilityMode(_serverLevel, FluorescentTubeCommandRegistry.InstabilityMode.LOW);

        music("mirage_gfbs:misc.blackhole.music.blackhole_p", allPlayers);

        for (ServerPlayer player : allPlayers) {
            CameraShakeCommand.triggerCameraShake(player, 12, 0.07f, 10000, 1000, 7000);
        }

        notification_cfoai("检测到反应堆核心异常, 反应抑制系统将在30秒后启动.", allPlayers, 200);

        sfx("mirage_gfbs:misc.blackhole.facility.quake1", 1.2f, allPlayers);
        sfx("mirage_gfbs:misc.blackhole.facility.quake2", 1.0f, allPlayers);

        Task.delay(() -> {
            sfx("mirage_gfbs:misc.global.alarm.overheat", 1.0f, allPlayers);
        }, 3000, TimeUnit.MILLISECONDS);

        Task.delay(() -> {
            notification_cfoai("检测到强引力异常, 启动反应抑制系统.", allPlayers, 200);

            AuralisServerApi.playSound(
                    ALARM2_ID,
                    ResourceLocation.parse("mirage_gfbs:misc.global.alarm.overload"),
                    1.2f,
                    1,
                    1,
                    true,
                    new Vec3(0, 0, 0),
                    true,
                    10,
                    10,
                    10,
                    allPlayers
            );
        }, 30000, TimeUnit.MILLISECONDS);

        Task.delay(() -> {
            sfx("mirage_gfbs:misc.blackhole.reactor.repture", 1.0f, allPlayers);
            sfx("mirage_gfbs:misc.blackhole.reactor.overload", 1.0f, allPlayers);

            FluorescentTubeCommandRegistry.turnOffAllTubes(_serverLevel);
            FluorescentTubeCommandRegistry.setInstabilityMode(_serverLevel, FluorescentTubeCommandRegistry.InstabilityMode.NONE);
        }, 31677, TimeUnit.MILLISECONDS);

        Task.delay(() -> {
            sfx("mirage_gfbs:misc.blackhole.reactor.explosion1", 1.0f, allPlayers);
            sfx("mirage_gfbs:misc.blackhole.reactor.reactor", 1.2f, allPlayers);

            for (ServerPlayer player : allPlayers) {
                CameraShakeCommand.triggerCameraShake(player, 20, 0.5f, 1000000, 10, 10000);
            }
        }, 32461, TimeUnit.MILLISECONDS);

        Task.delay(() -> {
            broadcast("mirage_gfbs:misc.blackhole.broadcast.core_explosion", allPlayers);
        }, 43449, TimeUnit.MILLISECONDS);

        Task.delay(() -> {
            sfx("mirage_gfbs:misc.blackhole.reactor.move", 1.35f, allPlayers);

            AuralisServerApi.stopSound(ALARM2_ID, allPlayers);
            explosion(1, true);

            notification_cfoai("反应抑制系统控制节点离线.", allPlayers, 150);
        }, 72000, TimeUnit.MILLISECONDS);

        Task.delay(() -> {
            sfx("mirage_gfbs:misc.blackhole.reactor.explosion2", 1.35f, allPlayers);

            for (ServerPlayer player : allPlayers) {
                CameraShakeCommand.triggerCameraShake(player, 35, 2.7f, 5600, 2, 1029);
            }

            fl(allPlayers);
        }, 105467, TimeUnit.MILLISECONDS);
    }

    private static void explosion(int b, boolean autoShake) {
        if (b == 1) {
            executeCommandAsync("playsound mirage_gfbs:surroundings.s2.dmr_m_p2_b1 voice @a ~ ~ ~ 1 1 1");
        }
        if (b == 2) {
            executeCommandAsync("playsound mirage_gfbs:surroundings.s2.dmr_m_p2_b2 voice @a ~ ~ ~ 1 1 1");
        }
        if (b == 3) {
            executeCommandAsync("playsound mirage_gfbs:surroundings.s2.dmr_m_p2_b3 voice @a ~ ~ ~ 1 1 1");
        }

        if (autoShake) {
            for (ServerPlayer player : _serverLevel.players()) {
                CameraShakeCommand.triggerCameraShake(player, 26, 0.09f, 4800, 10, 4290);
            }
        }
    }

    private static void notification_cfoai(String msg, Collection<ServerPlayer> allPlayers, int time) {
        NotificationCommand.sendNotificationToPlayers(allPlayers, "CFOAI", msg, time);
    }

    public static void fl(Collection<ServerPlayer> allPlayers) {
        NetworkHandler.sendToAll("mirage_blackhole_e_boom_h_event_client_a1");

        Task.sleep(14000);

        for (ServerPlayer player : allPlayers) {
            CameraShakeCommand.stopCameraShake(player);
        }

        NetworkHandler.sendToAll("mirage_blackhole_e_boom_h_event_client_a2");
        FluorescentTubeCommandRegistry.turnOnAllTubes(_serverLevel);
        FluorescentTubeCommandRegistry.setInstabilityMode(_serverLevel, FluorescentTubeCommandRegistry.InstabilityMode.NONE);
    }

    public static void clientExec() {
        ClientEventHandler.registerEvent("mirage_blackhole_e_boom_h_event_client_a1", (ct) -> {
            ExposureController.COLOR_LERP_TIME_SEC = 0f;
            ExposureController.setExposure(0f, Color.WHITE);

            Task.delay(() -> {
                ExposureController.COLOR_LERP_TIME_SEC = 2f;
                ExposureController.setExposure(1f, Color.WHITE);
            }, 50, TimeUnit.MILLISECONDS);
        });

        ClientEventHandler.registerEvent("mirage_blackhole_e_boom_h_event_client_a2", (ct) -> {
            ExposureController.COLOR_LERP_TIME_SEC = 3f;
            ExposureController.setExposure(0f, Color.BLACK);
        });
    }

    // ----------------------------
    // audio tools
    // ----------------------------

    private static void music(String id, Collection<ServerPlayer> allPlayers) {
        AuralisServerApi.playStreamedSound(
                MUSIC_ID,
                ResourceLocation.parse(id),
                1,
                1,
                1,
                true,
                new Vec3(0, 0, 0),
                false,
                10,
                10,
                10,
                allPlayers
        );
    }

    private static void broadcast(String id, Collection<ServerPlayer> allPlayers) {
        AuralisServerApi.playSound(
                BROADCAST_ID,
                ResourceLocation.parse(id),
                1.2f,
                1,
                1,
                true,
                new Vec3(0, 0, 0),
                false,
                10,
                10,
                10,
                allPlayers
        );
    }

    private static void sfx(String id, float volume, Collection<ServerPlayer> allPlayers) {
        String instanceId = nextOneShotInstanceId("blackhole_e_sfx", id);

        AuralisServerApi.playSound(
                instanceId,
                ResourceLocation.parse(id),
                volume,
                1,
                1,
                true,
                new Vec3(0, 0, 0),
                false,
                10,
                10,
                10,
                allPlayers
        );
    }

    private static String nextOneShotInstanceId(String prefix, String soundId) {
        long seq = SFX_SEQUENCE.incrementAndGet();
        long nano = System.nanoTime();
        return prefix + "_" + sanitizeSoundId(soundId) + "_" + seq + "_" + nano;
    }

    private static String sanitizeSoundId(String soundId) {
        return soundId.replace(':', '_').replace('.', '_').replace('/', '_');
    }
}