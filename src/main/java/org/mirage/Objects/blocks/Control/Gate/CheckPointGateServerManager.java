package org.mirage.Objects.blocks.Control.Gate;

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

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CheckPointGateServerManager {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> CHECK_POINT_GATES = new ConcurrentHashMap<>();

    public static void registerCheckPointGate(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        ResourceKey<Level> key = level.dimension();
        CHECK_POINT_GATES
                .computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet())
                .add(pos.immutable());
    }

    public static void unregisterCheckPointGate(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        ResourceKey<Level> key = level.dimension();
        Set<BlockPos> set = CHECK_POINT_GATES.get(key);
        if (set != null) {
            set.remove(pos);
            if (set.isEmpty()) CHECK_POINT_GATES.remove(key);
        }
    }

    public static List<BlockPos> getCheckPointGatesInLevel(Level level) {
        ResourceKey<Level> key = level.dimension();
        Set<BlockPos> set = CHECK_POINT_GATES.get(key);
        return set == null ? Collections.emptyList() : new ArrayList<>(set);
    }
}