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
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import org.mirage.gfbs.Mirage_gfbs;
import org.mirage.gfbs.Phenomenon.network.Notification.NotificationPacket;
import org.mirage.gfbs.Phenomenon.network.Notification.PacketHandler;
import org.mirage.gfbs.PrivilegeManager;

import java.util.Collection;

public class NotificationCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("Notification")
                .requires(source -> source.hasPermission(2) || PrivilegeManager.hasPrivilege(source))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("tick", IntegerArgumentType.integer(1))
                                .then(Commands.argument("title", StringArgumentType.word())
                                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                                                    String title = StringArgumentType.getString(context, "title");
                                                    String message = StringArgumentType.getString(context, "message");

                                                    int displayTime = IntegerArgumentType.getInteger(context, "tick");

                                                    for (ServerPlayer player : targets) {
                                                        PacketHandler.sendToPlayer(
                                                                new NotificationPacket(title, message, displayTime),
                                                                player
                                                        );
                                                    }

                                                    return targets.size();
                                                })
                                        )
                                )
                        )
                )
        );
    }

    /**
     * 直接发送通知的通用方法
     * @param player 目标玩家
     * @param title 标题
     * @param message 消息内容
     * @param displayTime 显示时间(ticks)
     */
    public static void sendNotification(ServerPlayer player, String title, String message, int displayTime) {
        try {
            PacketHandler.sendToPlayer(
                    new NotificationPacket(title, message, displayTime),
                    player
            );
        } catch (Exception e) {
            Mirage_gfbs.LOGGER.error("发送通知失败", e);
        }
    }

    /**
     * 向多个玩家发送通知的通用方法
     * @param players 目标玩家集合
     * @param title 标题
     * @param message 消息内容
     * @param displayTime 显示时间(ticks)
     */
    public static void sendNotificationToPlayers(Collection<ServerPlayer> players, String title,
                                                 String message, int displayTime) {
        for (ServerPlayer player : players) {
            sendNotification(player, title, message, displayTime);
        }
    }
}