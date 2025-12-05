package org.mirage.Command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.PlayerMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import org.mirage.Phenomenon.network.Network.NetworkHandler;
import org.mirage.Phenomenon.network.Network.ServerEventSender;

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

public class MirageGFBsGateApiCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("MirageGFBsGateApi")
                        .requires(source -> source.hasPermission(2))

                        .then(Commands.literal("on")
                                .executes(ctx -> {
                                    NetworkHandler.sendToAll("open_all_gate");
                                    return 1;
                                })
                        )

                        .then(Commands.literal("off")
                                .executes(ctx -> {
                                    NetworkHandler.sendToAll("close_all_gate");
                                    return 1;
                                })
                        )
        );
    }
}
