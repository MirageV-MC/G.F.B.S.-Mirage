package org.mirage.Command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import org.mirage.Sound.ModNetwork;
import org.mirage.Sound.StopLoopSoundPacket;

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
