package org.mirage.gfbs.objects.blocks.Control.Gate;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

/**
 * Stores loaded gate positions per-dimension and per-gate-type.
 *
 * <p>This replaces the old "GateServerManager" + "CheckPointGateServerManager" split.</p>
 */
public class GateServerManager {

    /**
     * dimension -> (gateTypeId -> positions)
     *
     * <p>Note: positions only represent LOADED gate block entities (registered in {@code BlockEntity#onLoad}).</p>
     */
    private static final Map<ResourceKey<Level>, Map<String, Set<BlockPos>>> GATES_BY_TYPE = new ConcurrentHashMap<>();

    private static Set<BlockPos> getOrCreateSet(Level level, GateType type) {
        ResourceKey<Level> key = level.dimension();
        Map<String, Set<BlockPos>> perType = GATES_BY_TYPE.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        return perType.computeIfAbsent(type.id(), k -> ConcurrentHashMap.newKeySet());
    }

    public static void registerGate(Level level, GateType type, BlockPos pos) {
        if (level.isClientSide) return;
        getOrCreateSet(level, type).add(pos.immutable());
    }

    public static void unregisterGate(Level level, GateType type, BlockPos pos) {
        if (level.isClientSide) return;

        ResourceKey<Level> key = level.dimension();
        Map<String, Set<BlockPos>> perType = GATES_BY_TYPE.get(key);
        if (perType == null) return;

        Set<BlockPos> set = perType.get(type.id());
        if (set == null) return;

        set.remove(pos);
        if (set.isEmpty()) {
            perType.remove(type.id());
        }
        if (perType.isEmpty()) {
            GATES_BY_TYPE.remove(key);
        }
    }

    public static List<BlockPos> getGatesInLevel(Level level, GateType type) {
        ResourceKey<Level> key = level.dimension();
        Map<String, Set<BlockPos>> perType = GATES_BY_TYPE.get(key);
        if (perType == null) return Collections.emptyList();

        Set<BlockPos> set = perType.get(type.id());
        if (set == null || set.isEmpty()) return Collections.emptyList();

        return new ArrayList<>(set);
    }

    public static void clearLevel(Level level) {
        if (level.isClientSide) return;
        GATES_BY_TYPE.remove(level.dimension());
    }

    // ---------------------------
    // Backward-compat wrappers
    // ---------------------------

    /** Old behavior: standard gate only. */
    public static void registerGate(Level level, BlockPos pos) {
        registerGate(level, GateTypes.STANDARD, pos);
    }

    /** Old behavior: standard gate only. */
    public static void unregisterGate(Level level, BlockPos pos) {
        unregisterGate(level, GateTypes.STANDARD, pos);
    }

    /** Old behavior: standard gate only. */
    public static List<BlockPos> getGatesInLevel(Level level) {
        return getGatesInLevel(level, GateTypes.STANDARD);
    }
}
