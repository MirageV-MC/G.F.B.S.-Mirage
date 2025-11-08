package org.mirage.Command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import org.mirage.Sound.ModNetwork;
import org.mirage.Sound.StartLoopSoundPacket;

import java.util.Collection;

public class MiragePlaysoundCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("mirageplaysound")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("sound_id", ResourceLocationArgument.id())
                                .then(Commands.argument("volume", FloatArgumentType.floatArg(0.0F))
                                        .then(Commands.argument("pitch", FloatArgumentType.floatArg(0.0F))
                                                .then(Commands.argument("min_volume", FloatArgumentType.floatArg(0.0F))
                                                        .then(Commands.argument("targets", EntityArgument.players())
                                                                .executes(ctx -> {
                                                                    ResourceLocation soundId = ResourceLocationArgument.getId(ctx, "sound_id");
                                                                    float volume = FloatArgumentType.getFloat(ctx, "volume");
                                                                    float pitch = FloatArgumentType.getFloat(ctx, "pitch");
                                                                    float minVolume = FloatArgumentType.getFloat(ctx, "min_volume");
                                                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");

                                                                    for (ServerPlayer target : targets) {
                                                                        ModNetwork.sendStartLoopSound(
                                                                                new StartLoopSoundPacket(
                                                                                        soundId,
                                                                                        SoundSource.MASTER,
                                                                                        volume,
                                                                                        pitch,
                                                                                        minVolume
                                                                                ),
                                                                                target
                                                                        );
                                                                    }

                                                                    return 1;
                                                                }))
                                                        .executes(ctx -> {
                                                            ResourceLocation soundId = ResourceLocationArgument.getId(ctx, "sound_id");
                                                            float volume = FloatArgumentType.getFloat(ctx, "volume");
                                                            float pitch = FloatArgumentType.getFloat(ctx, "pitch");
                                                            float minVolume = FloatArgumentType.getFloat(ctx, "min_volume");
                                                            ServerPlayer player = ctx.getSource().getPlayerOrException();

                                                            ModNetwork.sendStartLoopSound(
                                                                    new StartLoopSoundPacket(soundId, SoundSource.MASTER, volume, pitch, minVolume),
                                                                    player
                                                            );
                                                            return 1;
                                                        }))))
                        ));
    }
}
