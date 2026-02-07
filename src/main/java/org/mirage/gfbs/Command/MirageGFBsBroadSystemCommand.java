/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.mirage.gfbs.Command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.mirage.gfbs.api.BroadSystemAPI;

public class MirageGFBsBroadSystemCommand {

    private static final DynamicCommandExceptionType ERROR_UNKNOWN_SOUND =
            new DynamicCommandExceptionType(id -> Component.literal("未知声音ID: " + id));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("MirageGFBsBroadSystem")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("play")
                        .then(Commands.argument("sound", ResourceLocationArgument.id())
                                .suggests((context, builder) ->
                                        SharedSuggestionProvider.suggestResource(
                                                BuiltInRegistries.SOUND_EVENT.keySet(), builder
                                        )
                                )
                                .executes(context -> {
                                    ResourceLocation soundId = ResourceLocationArgument.getId(context, "sound");

                                    if (!BuiltInRegistries.SOUND_EVENT.containsKey(soundId)) {
                                        throw ERROR_UNKNOWN_SOUND.create(soundId.toString());
                                    }

                                    String sound = soundId.toString();
                                    BroadSystemAPI.startBroadcast(sound, 1.0f, 1.0f);
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("开始广播: " + sound), true);
                                    return 1;
                                })
                                .then(Commands.argument("volume", FloatArgumentType.floatArg(0.0f, 114514.0f))
                                        .then(Commands.argument("pitch", FloatArgumentType.floatArg(0.5f, 200.0f))
                                                .executes(context -> {
                                                    ResourceLocation soundId = ResourceLocationArgument.getId(context, "sound");

                                                    if (!BuiltInRegistries.SOUND_EVENT.containsKey(soundId)) {
                                                        throw ERROR_UNKNOWN_SOUND.create(soundId.toString());
                                                    }

                                                    String sound = soundId.toString();
                                                    float volume = FloatArgumentType.getFloat(context, "volume");
                                                    float pitch = FloatArgumentType.getFloat(context, "pitch");
                                                    BroadSystemAPI.startBroadcast(sound, volume, pitch);
                                                    context.getSource().sendSuccess(() ->
                                                            Component.literal("开始广播: " + sound +
                                                                    " (音量: " + volume + ", 音调: " + pitch + ")"), true);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                )
                .then(Commands.literal("stop")
                        .executes(context -> {
                            BroadSystemAPI.stopAllBroadcasts();
                            context.getSource().sendSuccess(() ->
                                    Component.literal("停止所有广播"), true);
                            return 1;
                        })
                )
                .then(Commands.literal("list")
                        .executes(context -> {
                            var speakers = BroadSystemAPI.getAllSpeakers();

                            context.getSource().sendSuccess(() ->
                                    Component.literal("= 广播系统状态 ="), false);
                            context.getSource().sendSuccess(() ->
                                    Component.literal("扬声器数量: " + speakers.size()), false);
                            context.getSource().sendSuccess(() ->
                                    Component.literal("注意: 以及木盒大巴有!"), false);

                            return 1;
                        })
                )
        );
    }
}
