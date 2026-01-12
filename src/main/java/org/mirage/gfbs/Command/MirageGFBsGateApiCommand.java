package org.mirage.gfbs.Command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import org.mirage.gfbs.api.GateClientAPI;
import org.mirage.gfbs.Objects.blocks.Control.Gate.GateTypes;


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

public class MirageGFBsGateApiCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("MirageGFBsGateApi")
                        .requires(source -> source.hasPermission(2))

                        .then(Commands.literal("on")
                                .then(Commands.argument("GateType", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String gateType = StringArgumentType.getString(ctx, "GateType");
                                            exec(ctx.getSource().getLevel(), true, gateType);
                                            return 1;
                                        })
                                )
                        )

                        .then(Commands.literal("off")
                                .then(Commands.argument("GateType", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String gateType = StringArgumentType.getString(ctx, "GateType");
                                            exec(ctx.getSource().getLevel(), false, gateType);
                                            return 1;
                                        })
                                )
                        )
        );
    }

    public static void exec(ServerLevel level, boolean isOpen, String type){
        // accepts: "gate", "check_point_gate", or any GateType id registered in GateTypes
        GateClientAPI.setAllServer(level, GateTypes.get(type), isOpen);
    }
}
