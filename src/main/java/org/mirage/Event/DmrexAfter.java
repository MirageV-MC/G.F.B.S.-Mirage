package org.mirage.Event;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import org.mirage.Client.ExposureController;
import org.mirage.Command.CameraShakeCommand;
import org.mirage.Phenomenon.network.Network.ClientEventHandler;
import org.mirage.Phenomenon.network.Network.NetworkHandler;
import org.mirage.Tools.Task;

import java.awt.*;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static org.mirage.CommandExecutor.executeCommandAsync;

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

public class DmrexAfter {
    public static void exec(Collection<ServerPlayer> allPlayers) {
        executeCommandAsync("playsound mirage_gfbs:boom.boom2_b voice @a ~ ~ ~ 1 1 1");

        Task.delay(()->{
            executeCommandAsync("playsound mirage_gfbs:boom.boom2_s voice @a ~ ~ ~ 1 1 1");
            NetworkHandler.sendToAll("mirage_dmr_boom_h_event_client_a2");
            for (ServerPlayer player : allPlayers) {
                CameraShakeCommand.triggerCameraShake(player, 35, 2.7f, 5600, 2, 1029);
            }

            Task.sleep(2000);

            executeCommandAsync("playsound mirage_gfbs:human.eas voice @a ~ ~ ~ 1 1 1");

            Task.sleep(33460);

            NetworkHandler.sendToAll("mirage_dmr_boom_h_event_client_a3");
        }, 2584, TimeUnit.MILLISECONDS);

        Task.sleep(970);

        for (ServerPlayer player : allPlayers) {
            CameraShakeCommand.triggerCameraShake(player, 30, 0.1f, 5600, 120, 1029);
        }

        NetworkHandler.sendToAll("mirage_dmr_boom_h_event_client_a1");
    }

    public static void clientExec(){
        ClientEventHandler.registerEvent("mirage_dmr_boom_h_event_client_a1", (ct)->{
            ExposureController.COLOR_LERP_TIME_SEC = 1f;
            ExposureController.setExposure(0.5f, Color.white);
        });
        ClientEventHandler.registerEvent("mirage_dmr_boom_h_event_client_a2", (ct)->{
            ExposureController.setExposure(1.0f, Color.white);

            ExposureController.COLOR_LERP_TIME_SEC = 5f;

            Task.delay(()-> ExposureController.setExposure(1f, Color.black), 5000, TimeUnit.MILLISECONDS);
        });
        ClientEventHandler.registerEvent("mirage_dmr_boom_h_event_client_a3", (ct)->{
            ExposureController.COLOR_LERP_TIME_SEC = 1f;
            ExposureController.setExposure(0f, Color.black);
        });
    }
}
