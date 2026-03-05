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

package org.mirage.gfbs.Command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mirage.gfbs.MirageGFBS;
import org.mirage.gfbs.Objects.blocks.Control.FluorescentTubeRegistry;
import org.mirage.gfbs.Objects.blocks.Control.FluorescentTubeSavedData;
import org.mirage.gfbs.Phenomenon.network.Network.NetworkHandler;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.mirage.gfbs.Objects.blocks.Bases.FlBlock.AbstractFluorescentLampBlock;

@Mod.EventBusSubscriber(modid = MirageGFBS.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FluorescentTubeCommandRegistry {

    private FluorescentTubeCommandRegistry() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("fl_tube")
                        // 需要 OP2
                        .requires(source -> source.hasPermission(2))

                        // ========= /fl_tube flash <target> <duration> <frequency> =========
                        .then(Commands.literal("flash")
                                .then(Commands.argument("target", EntityArgument.players())
                                        .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                                .then(Commands.argument("frequency", DoubleArgumentType.doubleArg(0.1D))
                                                        .executes(ctx -> {
                                                            CommandSourceStack source = ctx.getSource();
                                                            Collection<ServerPlayer> targets =
                                                                    EntityArgument.getPlayers(ctx, "target");
                                                            int duration = IntegerArgumentType.getInteger(ctx, "duration");
                                                            double frequency = DoubleArgumentType.getDouble(ctx, "frequency");

                                                            CompoundTag data = new CompoundTag();
                                                            data.putInt("duration", duration);
                                                            data.putDouble("frequency", frequency);

                                                            attachAllTubes(source, data);

                                                            for (ServerPlayer player : targets) {
                                                                NetworkHandler.sendToPlayer(
                                                                        player,
                                                                        "fluorescent_tube_flash_all",
                                                                        data
                                                                );
                                                            }
                                                            return targets.size();
                                                        })
                                                )
                                        )
                                )
                        )

                        // ========= /fl_tube on <target> =========
                        .then(Commands.literal("on")
                                .then(Commands.argument("target", EntityArgument.players())
                                        .executes(ctx -> {
                                            CommandSourceStack source = ctx.getSource();
                                            Collection<ServerPlayer> targets =
                                                    EntityArgument.getPlayers(ctx, "target");

                                            ServerLevel level = source.getLevel();
                                            FluorescentTubeSavedData.get(level).setGlobalState(true);
                                            updateLoadedTubesState(level, true);

                                            CompoundTag data = new CompoundTag();
                                            attachAllTubes(source, data);
                                            for (ServerPlayer player : targets) {
                                                NetworkHandler.sendToPlayer(
                                                        player,
                                                        "fluorescent_tube_turn_on_all",
                                                        data
                                                );
                                            }
                                            return targets.size();
                                        })
                                )
                        )

                        // ========= /fl_tube off <target> =========
                        .then(Commands.literal("off")
                                .then(Commands.argument("target", EntityArgument.players())
                                        .executes(ctx -> {
                                            CommandSourceStack source = ctx.getSource();
                                            Collection<ServerPlayer> targets =
                                                    EntityArgument.getPlayers(ctx, "target");

                                            ServerLevel level = source.getLevel();
                                            FluorescentTubeSavedData.get(level).setGlobalState(false);
                                            updateLoadedTubesState(level, false);

                                            CompoundTag data = new CompoundTag();
                                            attachAllTubes(source, data);
                                            for (ServerPlayer player : targets) {
                                                NetworkHandler.sendToPlayer(
                                                        player,
                                                        "fluorescent_tube_turn_off_all",
                                                        data
                                                );
                                            }
                                            return targets.size();
                                        })
                                )
                        )

                        // ========= /fl_tube instability <mode> <target> =========
                        .then(Commands.literal("instability")
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("none");
                                            builder.suggest("low");
                                            builder.suggest("high");
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("target", EntityArgument.players())
                                                .executes(ctx -> {
                                                    CommandSourceStack source = ctx.getSource();
                                                    String modeStr = StringArgumentType.getString(ctx, "mode");
                                                    Collection<ServerPlayer> targets =
                                                            EntityArgument.getPlayers(ctx, "target");

                                                    ServerLevel level = source.getLevel();
                                                    FluorescentTubeSavedData.get(level).setInstabilityMode(modeStr.toUpperCase());

                                                    CompoundTag data = new CompoundTag();
                                                    data.putString("mode", modeStr.toUpperCase());

                                                    for (ServerPlayer player : targets) {
                                                        NetworkHandler.sendToPlayer(
                                                                player,
                                                                "fluorescent_tube_set_instability",
                                                                data
                                                        );
                                                    }
                                                    return targets.size();
                                                })
                                        )
                                )
                        )
        );
    }

    private static void attachAllTubes(CommandSourceStack source, CompoundTag data) {
        attachAllTubes(source.getLevel(), data);
    }

    public static void attachAllTubes(ServerLevel level, CompoundTag data) {
        ListTag list = new ListTag();

        for (BlockPos pos : FluorescentTubeRegistry.getAll(level)) {
            list.add(blockPosToTag(pos));
        }

        FluorescentTubeSavedData saved = FluorescentTubeSavedData.get(level);
        for (BlockPos pos : saved.getAll()) {
            list.add(blockPosToTag(pos));
        }

        data.put("tubes", list);
    }
    private static CompoundTag blockPosToTag(BlockPos pos) {
        CompoundTag t = new CompoundTag();
        t.putInt("x", pos.getX());
        t.putInt("y", pos.getY());
        t.putInt("z", pos.getZ());
        return t;
    }


    public static void flashAllTubes(ServerLevel level,
                                     Collection<ServerPlayer> targets,
                                     int durationTicks,
                                     double averageFrequency) {
        CompoundTag data = new CompoundTag();
        data.putInt("duration", durationTicks);
        data.putDouble("frequency", averageFrequency);

        attachAllTubes(level, data);

        for (ServerPlayer player : targets) {
            NetworkHandler.sendToPlayer(
                    player,
                    "fluorescent_tube_flash_all",
                    data
            );
        }
    }

    public static void flashAllTubes(ServerLevel level,
                                     int durationTicks,
                                     double averageFrequency) {
        flashAllTubes(level, level.players(), durationTicks, averageFrequency);
    }

    private static void refreshAllTubes(ServerLevel level) {
        FluorescentTubeSavedData saved = FluorescentTubeSavedData.get(level);
        for (BlockPos pos : saved.getAll()) {
            if (level.hasChunkAt(pos)) {
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }
        for (BlockPos pos : FluorescentTubeRegistry.getAll(level)) {
            if (level.hasChunkAt(pos)) {
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }
    }

    private static void updateLoadedTubesState(ServerLevel level, boolean lit) {
        FluorescentTubeSavedData saved = FluorescentTubeSavedData.get(level);
        Set<BlockPos> allTubes = new HashSet<>(saved.getAll());
        allTubes.addAll(FluorescentTubeRegistry.getAll(level));

        for (BlockPos pos : allTubes) {
            if (level.hasChunkAt(pos)) {
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                if (state.hasProperty(AbstractFluorescentLampBlock.LIT)) {
                    boolean shouldBeLit = lit;
                    if (!lit) {
                        shouldBeLit = level.hasNeighborSignal(pos);
                    }
                    if (state.getValue(AbstractFluorescentLampBlock.LIT) != shouldBeLit) {
                        level.setBlock(pos, state.setValue(AbstractFluorescentLampBlock.LIT, shouldBeLit), 3);
                    }
                }
            }
        }
    }

    public static void turnOnAllTubes(ServerLevel level,
                                      Collection<ServerPlayer> targets) {
        FluorescentTubeSavedData.get(level).setGlobalState(true);
        updateLoadedTubesState(level, true);
        FluorescentTubeSavedData.get(level).setInstabilityMode("NONE");

        CompoundTag data = new CompoundTag();
        data.putString("mode", "NONE");
        attachAllTubes(level, data);

        for (ServerPlayer player : targets) {
            NetworkHandler.sendToPlayer(
                    player,
                    "fluorescent_tube_turn_on_all",
                    data
            );
            NetworkHandler.sendToPlayer(
                    player,
                    "fluorescent_tube_set_instability",
                    data
            );
        }
        refreshAllTubes(level);
    }

    public static void turnOnAllTubes(ServerLevel level) {
        turnOnAllTubes(level, level.players());
    }

    public static void turnOffAllTubes(ServerLevel level,
                                       Collection<ServerPlayer> targets) {
        FluorescentTubeSavedData.get(level).setGlobalState(false);
        updateLoadedTubesState(level, false);

        CompoundTag data = new CompoundTag();
        attachAllTubes(level, data);

        for (ServerPlayer player : targets) {
            NetworkHandler.sendToPlayer(
                    player,
                    "fluorescent_tube_turn_off_all",
                    data
            );
        }
        refreshAllTubes(level);
    }

    public static void turnOffAllTubes(ServerLevel level) {
        turnOffAllTubes(level, level.players());
    }

    public static void setInstabilityMode(ServerLevel level,
                                          Collection<ServerPlayer> targets,
                                          InstabilityMode mode) {
        FluorescentTubeSavedData.get(level).setInstabilityMode(mode.name().toUpperCase());

        CompoundTag data = new CompoundTag();
        data.putString("mode", mode.name().toUpperCase());

        for (ServerPlayer player : targets) {
            NetworkHandler.sendToPlayer(
                    player,
                    "fluorescent_tube_set_instability",
                    data
            );
        }
        if (mode == InstabilityMode.NONE) {
            refreshAllTubes(level);
        }
    }

    public static void setInstabilityMode(ServerLevel level,
                                          InstabilityMode mode) {
        setInstabilityMode(level, level.players(), mode);
    }

    public static enum InstabilityMode {
        NONE,
        LOW,
        HIGH
    }
}