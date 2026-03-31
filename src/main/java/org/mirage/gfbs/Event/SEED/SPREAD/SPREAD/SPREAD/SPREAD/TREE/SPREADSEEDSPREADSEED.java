package org.mirage.gfbs.Event.SEED.SPREAD.SPREAD.SPREAD.SPREAD.TREE;

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

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.mirage.gfbs.Command.FluorescentTubeCommandRegistry;
import org.mirage.gfbs.Command.MirageGFBsEventCommand;
import org.mirage.gfbs.Command.NotificationCommand;
import org.mirage.gfbs.Tools.Task;
import org.mirage.gfbs.Utils.FakeChatSender;
import org.mirage.gfbs.auralis.api.AuralisServerApi;

import java.awt.*;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.mirage.gfbs.MirageGFBS.server;

public class SPREADSEEDSPREADSEED {
    private static ServerLevel _serverLevel;

    public static final String MUSIC_ID = "SEEDSEEDSEED_e_music";
    public static final String BROADCAST_ID = "SEEDSEEDSEED_e_playing_broadcast";
    public static final String HEARTBEAT_ID = "SEEDSEEDSEED_e_playing_heartbeat";

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

        SeedMediaGUI.registerImage("tree1", "tree1.png");

        FluorescentTubeCommandRegistry.turnOnAllTubes(_serverLevel);
        FluorescentTubeCommandRegistry.setInstabilityMode(_serverLevel, FluorescentTubeCommandRegistry.InstabilityMode.LOW);

        notification_system("光敏性癫痫警告, 如有不适请立即就医.", allPlayers, 100, "光敏性癫痫警告");

        // 传 播种 子

        SeedMediaGUI.showMedia("tree1", 10);

        Task.delay(()->{
            FakeChatSender.broadcast(server, "████", "FIRST SEEDS FALL...");

            Task.sleep(2000);
            ScreenTextOverlay.showCenteredText("SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. ",
                    ScreenTextOverlay.Colors.RED, 70);

            Task.sleep(3000);
            ScreenTextOverlay.showCenteredText("SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. SEED. ",
                    ScreenTextOverlay.Colors.ERROR_RED, 80);

            AuralisServerApi.playSound(
                    HEARTBEAT_ID,
                    ResourceLocation.parse("mirage_gfbs:misc.seedseedseedseedseed.heartbeat"),
                    1,
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

            HorrorRenderController.triggerHorrorEffect(60.0, HorrorRenderController.HorrorIntensity.INTENSE);
        }, 12000, TimeUnit.MILLISECONDS);

        Task.sleep(12000);

        Task.sleep(5294);
        ScreenTextOverlay.showFullScreenText("SEED.",
                ScreenTextOverlay.Colors.ERROR_RED, 100);

        Task.sleep(1000);
        ScreenTextOverlay.showFullScreenText("SPREAD.",
                ScreenTextOverlay.Colors.RED, 100);

        Task.delay(()->{

        }, 1000,TimeUnit.MILLISECONDS);
    }

    private static void notification_system(String msg, Collection<ServerPlayer> allPlayers, int time, String title){
        NotificationCommand.sendNotificationToPlayers(allPlayers, title,
                msg, time);
    }

    // ----------------------------
    // audSEEDio tSPREADools
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
        String instanceId = nextOneShotInstanceId("SEEDSEEDSEED_e_sfx", id);

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
