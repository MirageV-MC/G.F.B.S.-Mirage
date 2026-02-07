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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import org.mirage.gfbs.MirageGFBS;
import org.mirage.gfbs.Phenomenon.CameraShake.CameraShakeModule;
import org.mirage.gfbs.PrivilegeManager;

import java.util.Collection;

public class CameraShakeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // 创建基础命令
        LiteralArgumentBuilder<CommandSourceStack> cameraShakeCommand = Commands.literal("CameraShake")
                .requires(source -> source.hasPermission(2) || PrivilegeManager.hasPrivilege(source))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("speed", FloatArgumentType.floatArg(0.01f, 100.0f))
                                .then(Commands.argument("maxAmplitude", FloatArgumentType.floatArg(0.01f, 10.0f))
                                        .then(Commands.argument("duration", IntegerArgumentType.integer(1, 1200000))
                                                .then(Commands.argument("riseTime", IntegerArgumentType.integer(0, 50000))
                                                        .then(Commands.argument("fallTime", IntegerArgumentType.integer(0, 50000))
                                                                .executes(CameraShakeCommand::executeShakeCommand)
                                                        )
                                                )
                                        )
                                )
                        )
                );

        dispatcher.register(cameraShakeCommand);

        LiteralArgumentBuilder<CommandSourceStack> cameraShakeStopCommand = Commands.literal("CameraShakeStop")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(CameraShakeCommand::executeStopCommand)
                );

        dispatcher.register(cameraShakeStopCommand);
    }

    private static int executeShakeCommand(CommandContext<CommandSourceStack> context) {
        try {
            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
            float speed = FloatArgumentType.getFloat(context, "speed");
            float maxAmplitude = FloatArgumentType.getFloat(context, "maxAmplitude");
            int duration = IntegerArgumentType.getInteger(context, "duration");
            int riseTime = IntegerArgumentType.getInteger(context, "riseTime");
            int fallTime = IntegerArgumentType.getInteger(context, "fallTime");

            if (riseTime + fallTime > duration) {
                context.getSource().sendFailure(
                        net.minecraft.network.chat.Component.literal("上升时间和下降时间之和不能大于总持续时间")
                );
                return 0;
            }

            for (ServerPlayer player : targets) {
                CameraShakeModule.sendShakeCommand(player, speed, maxAmplitude, duration, riseTime, fallTime);
            }

            if (targets.size() == 1) {
                MirageGFBS.LOGGER.debug("已向 " + targets.iterator().next().getDisplayName().getString() + " 发送相机震动指令");
            } else {
                MirageGFBS.LOGGER.debug("已向 " + targets.size() + " 名玩家发送相机震动指令");
            }

            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            MirageGFBS.LOGGER.debug("执行命令时发生错误: " + e.getMessage());
            return 0;
        }
    }

    private static int executeStopCommand(CommandContext<CommandSourceStack> context) {
        try {
            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
            for (ServerPlayer player : targets) {
                CameraShakeModule.sendShakeStopCommand(player);
            }

            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            context.getSource().sendFailure(
                    net.minecraft.network.chat.Component.literal("停止命令执行错误: " + e.getMessage())
            );
            return 0;
        }
    }

    public static boolean triggerCameraShake(ServerPlayer player, float speed, float maxAmplitude,
                                             int duration, int riseTime, int fallTime) {
        try {
            if (riseTime + fallTime > duration) {
                return false;
            }

            CameraShakeModule.sendShakeCommand(player, speed, maxAmplitude, duration, riseTime, fallTime);
            return true;
        } catch (Exception e) {
            MirageGFBS.LOGGER.error("触发相机震动失败", e);
            return false;
        }
    }

    public static void stopCameraShake(ServerPlayer player) {
        try {
            CameraShakeModule.sendShakeStopCommand(player);
        } catch (Exception e) {
            MirageGFBS.LOGGER.error("停止相机震动失败", e);
        }
    }
}