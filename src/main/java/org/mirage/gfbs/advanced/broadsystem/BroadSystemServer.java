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

package org.mirage.gfbs.advanced.broadsystem;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BroadSystemServer {

    private static final BroadSystemServer INSTANCE = new BroadSystemServer();

    private final Set<SpeakerLocation> speakers = ConcurrentHashMap.newKeySet();

    private BroadSystemServer() {
    }

    public static BroadSystemServer getInstance() {
        return INSTANCE;
    }

    /**
     * 注册扬声器
     */
    public void registerSpeaker(ServerLevel level, BlockPos pos) {
        speakers.add(new SpeakerLocation(level.dimension(), pos));
    }

    /**
     * 移除扬声器
     */
    public void removeSpeaker(ResourceKey<Level> dimension, BlockPos pos) {
        speakers.remove(new SpeakerLocation(dimension, pos));
    }

    /**
     * 获取所有扬声器位置
     */
    public Set<BlockPos> getAllSpeakers() {
        return speakers.stream()
                .map(SpeakerLocation::pos)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * 开始广播 - 播放后即释放，无ID系统
     * @param soundEvent 要播放的声音事件
     * @param volume 音量 (0.0 - 1.0)
     * @param pitch 音调
     */
    public void startBroadcast(ResourceLocation soundEvent, float volume, float pitch) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        long startTime = System.currentTimeMillis();

        for (SpeakerLocation location : speakers) {
            ServerLevel level = server.getLevel(location.dimension());
            if (level == null) continue;

            BlockPos pos = location.pos();

            if (!level.isLoaded(pos)) {
                level.getChunk(pos);
            }

            if (level.isLoaded(pos)) {
                BroadSystemNetwork.sendStartBroadcast(level, pos, soundEvent, volume, pitch, startTime);

                if (level.getBlockEntity(pos) instanceof SpeakerBlockEntity entity) {
                    entity.setPlaying(true);
                }
            }
        }
    }

    /**
     * 停止所有广播
     */
    public void stopAllBroadcasts() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        for (SpeakerLocation location : speakers) {
            ServerLevel level = server.getLevel(location.dimension());
            if (level == null) continue;

            BlockPos pos = location.pos();

            if (!level.isLoaded(pos)) {
                level.getChunk(pos);
            }

            if (level.isLoaded(pos)) {
                BroadSystemNetwork.sendStopBroadcast(level, pos);

                if (level.getBlockEntity(pos) instanceof SpeakerBlockEntity entity) {
                    entity.setPlaying(false);
                }
            }
        }
    }

    /**
     * 扬声器位置记录
     */
    private record SpeakerLocation(ResourceKey<Level> dimension, BlockPos pos) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SpeakerLocation that = (SpeakerLocation) o;
            return Objects.equals(dimension, that.dimension) && Objects.equals(pos, that.pos);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dimension, pos);
        }
    }
}
