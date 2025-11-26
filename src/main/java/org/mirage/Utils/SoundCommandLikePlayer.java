package org.mirage.Utils;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Convex89524
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SoundCommandLikePlayer {

    private static final CommandSyntaxException ERROR_TOO_FAR =
            new CommandSyntaxException(null, Component.literal("Too far to hear sound"));

    public static int playSound(CommandSourceStack source,
                                Collection<ServerPlayer> targets,
                                ResourceLocation soundId,
                                SoundSource soundSource,
                                Vec3 pos,
                                float volume,
                                float pitch,
                                float minVolume) throws CommandSyntaxException {

        Holder<SoundEvent> holder = Holder.direct(SoundEvent.createVariableRangeEvent(soundId));
        double maxDistSq = (double) Mth.square(holder.value().getRange(volume));
        int count = 0;
        long seed = source.getLevel().getRandom().nextLong();

        for (ServerPlayer player : targets) {
            double dx = pos.x - player.getX();
            double dy = pos.y - player.getY();
            double dz = pos.z - player.getZ();
            double distSq = dx * dx + dy * dy + dz * dz;

            Vec3 sendPos = pos;
            float sendVolume = volume;

            if (distSq > maxDistSq) {
                if (minVolume <= 0.0F) {
                    continue;
                }

                double dist = Math.sqrt(distSq);
                sendPos = new Vec3(
                        player.getX() + dx / dist * 2.0D,
                        player.getY() + dy / dist * 2.0D,
                        player.getZ() + dz / dist * 2.0D
                );
                sendVolume = minVolume;
            }

            player.connection.send(
                    new ClientboundSoundPacket(
                            holder,
                            soundSource,
                            sendPos.x(),
                            sendPos.y(),
                            sendPos.z(),
                            sendVolume,
                            pitch,
                            seed
                    )
            );
            ++count;
        }

        if (count == 0) {
            throw ERROR_TOO_FAR;
        } else {
            if (targets.size() == 1) {
                source.sendSuccess(
                        () -> Component.translatable(
                                "commands.playsound.success.single",
                                soundId,
                                targets.iterator().next().getDisplayName()
                        ),
                        true
                );
            } else {
                source.sendSuccess(
                        () -> Component.translatable(
                                "commands.playsound.success.multiple",
                                soundId,
                                targets.size()
                        ),
                        true
                );
            }
            return count;
        }
    }

    public static int execute(CommandSourceStack source,
                              SoundSource soundSource,
                              String soundIdString,
                              String args) {

        args = args.trim();
        if (args.isEmpty()) {
            source.sendFailure(Component.literal("用法: <targets> [<x> <y> <z>] [<volume>] [<pitch>] [<minVolume>]"));
            return 0;
        }

        String[] parts = args.split("\\s+");
        int len = parts.length;

        if (len < 1) {
            source.sendFailure(Component.literal("缺少 targets 参数。"));
            return 0;
        }

        String targetsToken = parts[0];
        Collection<ServerPlayer> targets = resolveTargets(source, targetsToken);
        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("找不到任何匹配的玩家: " + targetsToken));
            return 0;
        }

        Vec3 sourcePos = source.getPosition();
        Vec3 pos = sourcePos;
        int index = 1;

        if (len - index >= 3) {
            try {
                double x = parseCoord(parts[index++], sourcePos.x);
                double y = parseCoord(parts[index++], sourcePos.y);
                double z = parseCoord(parts[index++], sourcePos.z);
                pos = new Vec3(x, y, z);
            } catch (NumberFormatException e) {
                source.sendFailure(Component.literal("坐标解析失败: " + e.getMessage()));
                return 0;
            }
        }

        float volume = 1.0F;
        float pitch = 1.0F;
        float minVolume = 0.0F;

        try {
            if (len > index) {
                volume = Float.parseFloat(parts[index++]);
            }
            if (len > index) {
                pitch = Float.parseFloat(parts[index++]);
            }
            if (len > index) {
                minVolume = Float.parseFloat(parts[index++]);
            }
        } catch (NumberFormatException e) {
            source.sendFailure(Component.literal("音量/音调参数解析失败: " + e.getMessage()));
            return 0;
        }

        ResourceLocation soundId;
        try {
            soundId = new ResourceLocation(soundIdString);
        } catch (Exception e) {
            source.sendFailure(Component.literal("无效的声音 ID: " + soundIdString));
            return 0;
        }

        try {
            return playSound(source, targets, soundId, soundSource, pos, volume, pitch, minVolume);
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static double parseCoord(String token, double base) throws NumberFormatException {
        if (token.startsWith("~")) {
            if (token.length() == 1) {
                return base;
            } else {
                double offset = Double.parseDouble(token.substring(1));
                return base + offset;
            }
        } else {
            return Double.parseDouble(token);
        }
    }

    private static Collection<ServerPlayer> resolveTargets(CommandSourceStack source, String token) {
        ServerLevel level = source.getLevel();
        List<ServerPlayer> result = new ArrayList<>();

        if ("@a".equals(token)) {
            result.addAll(level.players());
        } else {
            ServerPlayer player = level.getServer().getPlayerList().getPlayerByName(token);
            if (player != null) {
                result.add(player);
            }
        }

        return result;
    }
}
