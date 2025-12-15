package org.mirage.Command;

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

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import org.mirage.LoopSoundTool.ModNetwork;
import org.mirage.LoopSoundTool.StopLoopSoundPacket;

import java.util.Collection;

public class MirageStopsoundCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("miragestopsound")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("sound_id", ResourceLocationArgument.id())
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> {
                                            ResourceLocation soundId = ResourceLocationArgument.getId(ctx, "sound_id");
                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");

                                            for (ServerPlayer target : targets) {
                                                ModNetwork.sendStopLoopSound(
                                                        new StopLoopSoundPacket(soundId, SoundSource.MASTER),
                                                        target
                                                );
                                            }

                                            return 1;
                                        }))
                                .executes(ctx -> {
                                    ResourceLocation soundId = ResourceLocationArgument.getId(ctx, "sound_id");
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ModNetwork.sendStopLoopSound(new StopLoopSoundPacket(soundId, SoundSource.MASTER), player);
                                    return 1;
                                }))
        );
    }
}
