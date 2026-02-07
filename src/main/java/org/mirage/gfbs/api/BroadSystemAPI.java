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

package org.mirage.gfbs.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.mirage.gfbs.advanced.broadsystem.BroadSystemServer;

import java.util.Set;

/**
 * 广播系统公共API
 */
public class BroadSystemAPI {

    /**
     * 开始广播
     * 所有已放置的扬声器将同时播放指定的声音
     *
     * @param soundEvent 声音事件ID (例如 "mirage_gfbs:alarm/emergency_a")
     * @param volume 音量 (0.0 - 1.0)
     * @param pitch 音调 (0.5 - 2.0)
     */
    public static void startBroadcast(ResourceLocation soundEvent, float volume, float pitch) {
        BroadSystemServer.getInstance().startBroadcast(soundEvent, volume, pitch);
    }

    /**
     * 开始广播（使用字符串ID）
     *
     * @param soundEventId 声音事件ID字符串 (例如 "mirage_gfbs:alarm/emergency_a")
     * @param volume 音量 (0.0 - 1.0)
     * @param pitch 音调 (0.5 - 2.0)
     */
    public static void startBroadcast(String soundEventId, float volume, float pitch) {
        ResourceLocation soundEvent = new ResourceLocation(soundEventId);
        startBroadcast(soundEvent, volume, pitch);
    }

    /**
     * 开始广播（使用SoundEvent对象）
     *
     * @param soundEvent 声音事件对象
     * @param volume 音量 (0.0 - 1.0)
     * @param pitch 音调 (0.5 - 2.0)
     */
    public static void startBroadcast(SoundEvent soundEvent, float volume, float pitch) {
        ResourceLocation location = ForgeRegistries.SOUND_EVENTS.getKey(soundEvent);
        if (location == null) {
            throw new IllegalArgumentException("SoundEvent not registered: " + soundEvent);
        }
        startBroadcast(location, volume, pitch);
    }

    /**
     * 使用默认参数开始广播
     * 音量: 1.0, 音调: 1.0
     *
     * @param soundEventId 声音事件ID字符串
     */
    public static void startBroadcast(String soundEventId) {
        startBroadcast(soundEventId, 1.0f, 1.0f);
    }

    /**
     * 停止所有广播
     */
    public static void stopAllBroadcasts() {
        BroadSystemServer.getInstance().stopAllBroadcasts();
    }

    /**
     * 获取所有已放置的扬声器位置
     *
     * @return 扬声器位置集合
     */
    public static Set<BlockPos> getAllSpeakers() {
        return BroadSystemServer.getInstance().getAllSpeakers();
    }

    /**
     * 获取扬声器数量
     *
     * @return 已放置的扬声器数量
     */
    public static int getSpeakerCount() {
        return BroadSystemServer.getInstance().getAllSpeakers().size();
    }
}
