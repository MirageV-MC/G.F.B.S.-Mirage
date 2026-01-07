package org.mirage.Command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.mirage.Phenomenon.network.Notification.NotificationPacket;
import org.mirage.Phenomenon.network.Notification.PacketHandler;
import org.mirage.PrivilegeManager;

import java.util.Collection;

import static org.mirage.CommandExecutor.executeCommandAsync;

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

public class MirageGFBsEnvExplosionCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("MirageGFBsEnvExplosion")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.argument("b", IntegerArgumentType.integer())
                                        .executes(context -> {
                                            CommandSourceStack source = context.getSource();
                                            int env = IntegerArgumentType.getInteger(context, "b");
                                            explosion(env, source.getLevel());

                                            return 1;
                                        })
                        )
        );
    }

    public static void explosion(int b, ServerLevel _serverLevel) {
        if (b == 1){
            executeCommandAsync("playsound mirage_gfbs:surroundings.s2.dmr_m_p2_b1 voice @a ~ ~ ~ 1 1 1");
        }
        if (b == 2){
            executeCommandAsync("playsound mirage_gfbs:surroundings.s2.dmr_m_p2_b2 voice @a ~ ~ ~ 1 1 1");
        }
        if (b == 3){
            executeCommandAsync("playsound mirage_gfbs:surroundings.s2.dmr_m_p2_b3 voice @a ~ ~ ~ 1 1 1");
        }
        for (ServerPlayer player : _serverLevel.players()) {
            CameraShakeCommand.triggerCameraShake(player, 26, 0.09f, 4800, 10, 4290);
        }
        FluorescentTubeCommandRegistry.flashAllTubes(_serverLevel, 75, 3.0D);
    }
}
