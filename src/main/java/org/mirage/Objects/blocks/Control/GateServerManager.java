package org.mirage.Objects.blocks.Control;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

public final class GateServerManager {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> GATES =
            new ConcurrentHashMap<>();

    private GateServerManager() {
    }

    public static void registerGate(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        ResourceKey<Level> key = level.dimension();
        GATES
                .computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet())
                .add(pos.immutable());
    }

    public static void unregisterGate(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        ResourceKey<Level> key = level.dimension();
        Set<BlockPos> set = GATES.get(key);
        if (set != null) {
            set.remove(pos);
            if (set.isEmpty()) {
                GATES.remove(key);
            }
        }
    }

    public static List<BlockPos> getGatesInLevel(Level level) {
        ResourceKey<Level> key = level.dimension();
        Set<BlockPos> set = GATES.get(key);
        if (set == null || set.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(set);
    }

    public static void clearLevel(Level level) {
        if (level.isClientSide) {
            return;
        }
        GATES.remove(level.dimension());
    }
}
