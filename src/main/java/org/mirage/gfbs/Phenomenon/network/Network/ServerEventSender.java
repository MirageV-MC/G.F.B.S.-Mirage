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

package org.mirage.gfbs.Phenomenon.network.Network;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.mirage.gfbs.Mirage_gfbs;

import java.util.List;

public class ServerEventSender {
    public static void sendEvent(CommandSourceStack source, String selector, String eventId, CompoundTag data) {
        try {
            if (!source.hasPermission(2)) {
                source.sendFailure(Component.literal("你没有权限发送事件."));
                Mirage_gfbs.LOGGER.warn("Player {} attempted to send an event but does not have permission.", source.getTextName());
                return;
            }

            List<ServerPlayer> targets = resolveSelector(source, selector);
            if (targets.isEmpty()) {
                source.sendFailure(Component.literal("未找到目标玩家."));
                Mirage_gfbs.LOGGER.warn("No target player found using selector {}.", selector);
                return;
            }

            for (ServerPlayer player : targets) {
                NetworkHandler.sendToPlayer(player, eventId, data);
            }

            Mirage_gfbs.LOGGER.info("Successfully sent event: {} to {} players through selector {}.", selector, targets.size(), eventId);
        } catch (Exception e) {
            Mirage_gfbs.LOGGER.error("An error occurred while sending the server event: {}.", eventId, e);
            source.sendFailure(Component.literal("发送事件时发生错误: " + e.getMessage()));
        }
    }

    private static List<ServerPlayer> resolveSelector(CommandSourceStack source, String selector) {
        try {
            if ("@a".equals(selector)) {
                return source.getServer().getPlayerList().getPlayers();
            } else if ("@s".equals(selector) && source.getEntity() instanceof ServerPlayer) {
                return List.of((ServerPlayer) source.getEntity());
            } else if ("@p".equals(selector)) {
                Vec3 sourcePos = source.getPosition();
                ServerPlayer closest = null;
                double minDistance = Double.MAX_VALUE;

                for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                    double distance = player.position().distanceToSqr(sourcePos);
                    if (distance < minDistance) {
                        minDistance = distance;
                        closest = player;
                    }
                }

                return closest != null ? List.of(closest) : List.of();
            } else if ("@r".equals(selector)) {
                List<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
                if (players.isEmpty()) {
                    return List.of();
                }
                return List.of(players.get((int) (Math.random() * players.size())));
            } else {
                ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(selector);
                return player != null ? List.of(player) : List.of();
            }
        } catch (Exception e) {
            Mirage_gfbs.LOGGER.error("An error occurred while parsing the selector: {}", selector, e);
            return List.of();
        }
    }
}