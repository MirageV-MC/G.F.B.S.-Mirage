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

package org.mirage.Command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.mirage.Mirage_gfbs;
import org.mirage.Phenomenon.network.Network.NetworkHandler;
import org.mirage.PrivilegeManager;

public class FogCommand {
    private static final CompoundTag currentFogSettings = new CompoundTag();

    private static int transitionDuration = 1000;

    static {
        currentFogSettings.putBoolean("active", false);
        currentFogSettings.putFloat("red", 0.5f);
        currentFogSettings.putFloat("green", 0.5f);
        currentFogSettings.putFloat("blue", 0.5f);
        currentFogSettings.putFloat("start", 0.0f);
        currentFogSettings.putFloat("end", 1.0f);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> fogCommand = Commands.literal("miragefog")
                .requires(source -> source.hasPermission(2) || PrivilegeManager.hasPrivilege(source))
                .then(Commands.literal("toggle")
                        .then(Commands.argument("state", BoolArgumentType.bool())
                                .executes(context -> toggleFog(context, BoolArgumentType.getBool(context, "state"))))
                        .executes(context -> toggleFog(context, !currentFogSettings.getBoolean("active"))))
                .then(Commands.literal("color")
                        .then(Commands.argument("red", FloatArgumentType.floatArg(0, 1))
                                .then(Commands.argument("green", FloatArgumentType.floatArg(0, 1))
                                        .then(Commands.argument("blue", FloatArgumentType.floatArg(0, 1))
                                                .executes(context -> setFogColor(
                                                        context,
                                                        FloatArgumentType.getFloat(context, "red"),
                                                        FloatArgumentType.getFloat(context, "green"),
                                                        FloatArgumentType.getFloat(context, "blue")))))))
                .then(Commands.literal("range")
                        .then(Commands.argument("start", FloatArgumentType.floatArg(0))
                                .then(Commands.argument("end", FloatArgumentType.floatArg(0))
                                        .executes(context -> setFogRange(
                                                context,
                                                FloatArgumentType.getFloat(context, "start"),
                                                FloatArgumentType.getFloat(context, "end"))))))
                .then(Commands.literal("status")
                        .executes(FogCommand::getFogStatus))
                .then(Commands.literal("transition")
                        .then(Commands.argument("duration", IntegerArgumentType.integer(0, 10000))
                                .executes(context -> setTransitionDuration(
                                        context, IntegerArgumentType.getInteger(context, "duration")))))
                .then(Commands.literal("sync")
                        .executes(FogCommand::syncFogToAllClients));

        dispatcher.register(fogCommand);
    }

    private static int toggleFog(CommandContext<CommandSourceStack> context, boolean state) {
        currentFogSettings.putBoolean("active", state);
        syncFogToAllClients(context);
        return Command.SINGLE_SUCCESS;
    }

    private static int setFogColor(CommandContext<CommandSourceStack> context, float red, float green, float blue) {
        currentFogSettings.putFloat("red", red);
        currentFogSettings.putFloat("green", green);
        currentFogSettings.putFloat("blue", blue);
        syncFogToAllClients(context);
        return Command.SINGLE_SUCCESS;
    }

    private static int setFogRange(CommandContext<CommandSourceStack> context, float start, float end) {
        currentFogSettings.putFloat("start", start);
        currentFogSettings.putFloat("end", end);
        syncFogToAllClients(context);
        return Command.SINGLE_SUCCESS;
    }

    private static int getFogStatus(CommandContext<CommandSourceStack> context) {
        boolean active = currentFogSettings.getBoolean("active");
        float red = currentFogSettings.getFloat("red");
        float green = currentFogSettings.getFloat("green");
        float blue = currentFogSettings.getFloat("blue");
        float start = currentFogSettings.getFloat("start");
        float end = currentFogSettings.getFloat("end");

        String status = active ? "启用" : "禁用";
        context.getSource().sendSuccess(() -> Component.literal(
                String.format("雾效果状态: %s, 颜色: R=%.2f G=%.2f B=%.2f, 范围: %.1f-%.1f, 渐变时间: %d毫秒",
                        status, red, green, blue, start, end, transitionDuration)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int syncFogToAllClients(CommandContext<CommandSourceStack> context) {
        CompoundTag fogData = currentFogSettings.copy();
        fogData.putInt("transitionDuration", transitionDuration);

        NetworkHandler.sendToAll("fog_settings", fogData);
        return Command.SINGLE_SUCCESS;
    }

    public static CompoundTag getCurrentFogSettings() {
        return currentFogSettings.copy();
    }

    private static int setTransitionDuration(CommandContext<CommandSourceStack> context, int duration) {
        transitionDuration = Math.max(0, Math.min(10000, duration));
        context.getSource().sendSuccess(() -> Component.literal(
                "雾效果渐变时间设置为: " + transitionDuration + "毫秒"), false);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * 直接设置雾效果的通用方法
     * @param active 是否启用
     * @param red 红色分量(0-1)
     * @param green 绿色分量(0-1)
     * @param blue 蓝色分量(0-1)
     * @param start 起始距离
     * @param end 结束距离
     * @param transitionDuration 渐变时间(毫秒)
     */
    public static void setFogSettings(boolean active, float red, float green, float blue,
                                      float start, float end, int transitionDuration) {
        currentFogSettings.putBoolean("active", active);
        currentFogSettings.putFloat("red", red);
        currentFogSettings.putFloat("green", green);
        currentFogSettings.putFloat("blue", blue);
        currentFogSettings.putFloat("start", start);
        currentFogSettings.putFloat("end", end);

        FogCommand.transitionDuration = Math.max(0, Math.min(10000, transitionDuration));

        // 同步到所有客户端
        CompoundTag fogData = currentFogSettings.copy();
        fogData.putInt("transitionDuration", transitionDuration);
        NetworkHandler.sendToAll("fog_settings", fogData);
    }

    /**
     * 获取当前雾效果设置
     * @return 雾效果设置的副本
     */
    public static CompoundTag getFogSettings() {
        return currentFogSettings.copy();
    }
}