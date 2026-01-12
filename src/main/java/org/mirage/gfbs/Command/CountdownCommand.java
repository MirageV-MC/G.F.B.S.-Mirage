package org.mirage.gfbs.Command;

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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.mirage.gfbs.api.CountdownAPI;

public final class CountdownCommand {
    private CountdownCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("MirageGFBsCountdown")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("popup")
                                .then(Commands.argument("min", IntegerArgumentType.integer(0, 9999))
                                        .then(Commands.argument("sec", IntegerArgumentType.integer(0, 59))
                                                .then(Commands.argument("ms", IntegerArgumentType.integer(0, 99))
                                                        .then(Commands.argument("title", StringArgumentType.greedyString())
                                                                .executes(ctx -> {
                                                                    String title = StringArgumentType.getString(ctx, "title");
                                                                    int min = IntegerArgumentType.getInteger(ctx, "min");
                                                                    int sec = IntegerArgumentType.getInteger(ctx, "sec");
                                                                    int ms = IntegerArgumentType.getInteger(ctx, "ms");

                                                                    for (ServerPlayer player : ctx.getSource().getServer().getPlayerList().getPlayers()){
                                                                        CountdownAPI.popup(player, title, min, sec, ms);
                                                                    }
                                                                    
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("startCountdown")
                                .executes(ctx -> {
                                    var players = ctx.getSource().getLevel().players();
                                    for (ServerPlayer player : players){
                                        CountdownAPI.startCountdown(player);
                                    }
                                    return 1;
                                })
                        )
                        .then(Commands.literal("stop")
                                .executes(ctx -> {
                                    var players = ctx.getSource().getLevel().players();
                                    for (ServerPlayer player : players){
                                        CountdownAPI.stop(player);
                                    }
                                    return 1;
                                })
                        )
        );
    }
}