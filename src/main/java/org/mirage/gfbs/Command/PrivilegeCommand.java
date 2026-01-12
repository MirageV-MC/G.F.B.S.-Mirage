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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.mirage.gfbs.PrivilegeManager;

import java.util.Collection;

/**
 * 特权玩家管理命令
 */
public class PrivilegeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mirageprivilege")
                .requires(source -> source.hasPermission(3)) // 只有OP等级3+可以管理特权
                .then(Commands.literal("add")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                                    int count = (int) targets.stream().filter(PrivilegeManager::addToWhitelist).count();

                                    context.getSource().sendSuccess(() ->
                                            Component.literal("已添加 " + count + " 名玩家到特权白名单"), true);
                                    return count;
                                })
                        )
                        .then(Commands.argument("username", StringArgumentType.string())
                                .executes(context -> {
                                    String username = StringArgumentType.getString(context, "username");
                                    if (PrivilegeManager.addToWhitelist(username)) {
                                        context.getSource().sendSuccess(() ->
                                                Component.literal("已添加玩家 " + username + " 到特权白名单"), true);
                                        return 1;
                                    } else {
                                        context.getSource().sendFailure(
                                                Component.literal("添加玩家到白名单失败")
                                        );
                                        return 0;
                                    }
                                })
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> {
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                                    int count = (int) targets.stream().filter(PrivilegeManager::removeFromWhitelist).count();

                                    context.getSource().sendSuccess(() ->
                                            Component.literal("已从特权白名单移除 " + count + " 名玩家"), true);
                                    return count;
                                })
                        )
                        .then(Commands.argument("username", StringArgumentType.string())
                                .executes(context -> {
                                    String username = StringArgumentType.getString(context, "username");
                                    if (PrivilegeManager.removeFromWhitelist(username)) {
                                        context.getSource().sendSuccess(() ->
                                                Component.literal("已从特权白名单移除玩家 " + username), true);
                                        return 1;
                                    } else {
                                        context.getSource().sendFailure(
                                                Component.literal("从白名单移除玩家失败")
                                        );
                                        return 0;
                                    }
                                })
                        )
                )
                .then(Commands.literal("list")
                        .executes(context -> {
                            var players = PrivilegeManager.getPrivilegedPlayers();
                            context.getSource().sendSuccess(() ->
                                    Component.literal("特权玩家列表 (" + players.size() + "): " + String.join(", ", players)), false);
                            return players.size();
                        })
                )
                .then(Commands.literal("autoop")
                        .then(Commands.literal("enable")
                                .executes(context -> {
                                    PrivilegeManager.setAutoOpEnabled(true);
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("已启用特权玩家自动OP权限"), true);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("disable")
                                .executes(context -> {
                                    PrivilegeManager.setAutoOpEnabled(false);
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("已禁用特权玩家自动OP权限"), true);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("status")
                                .executes(context -> {
                                    boolean status = PrivilegeManager.isAutoOpEnabled();
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("特权玩家自动OP权限状态: " + (status ? "已启用" : "已禁用")), false);
                                    return status ? 1 : 0;
                                })
                        )
                        .then(Commands.literal("setlevel")
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, 4))
                                        .executes(context -> {
                                            int level = IntegerArgumentType.getInteger(context, "level");
                                            PrivilegeManager.setOpLevel(level);
                                            context.getSource().sendSuccess(() ->
                                                    Component.literal("已设置特权玩家OP等级为: " + level), true);
                                            return 1;
                                        })
                                )
                        )
                )
        );
    }

    /**
     * 直接添加特权玩家的通用方法
     * @param username 玩家用户名
     * @return 添加是否成功
     */
    public static boolean addPrivilegedPlayer(String username) {
        return PrivilegeManager.addToWhitelist(username);
    }

    /**
     * 直接移除特权玩家的通用方法
     * @param username 玩家用户名
     * @return 移除是否成功
     */
    public static boolean removePrivilegedPlayer(String username) {
        return PrivilegeManager.removeFromWhitelist(username);
    }

    /**
     * 获取所有特权玩家的通用方法
     * @return 特权玩家用户名集合
     */
    public static Collection<String> getPrivilegedPlayers() {
        return PrivilegeManager.getPrivilegedPlayers();
    }

    /**
     * 设置自动OP功能的通用方法
     * @param enabled 是否启用
     * @param level OP等级(0-4)
     */
    public static void setAutoOpSettings(boolean enabled, int level) {
        PrivilegeManager.setAutoOpEnabled(enabled);
        PrivilegeManager.setOpLevel(level);
    }
}