package org.mirage.Objects.blocks.Control;

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

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import org.mirage.Mirage_gfbs;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class FluorescentTubeRegistry {

    private static final Map<ResourceKey<Level>, Set<BlockPos>> REGISTRY = new ConcurrentHashMap<>();

    private FluorescentTubeRegistry() {}

    private static Set<BlockPos> getSet(ResourceKey<Level> dimension) {
        return REGISTRY.computeIfAbsent(
                dimension,
                k -> ConcurrentHashMap.newKeySet()
        );
    }

    public static void register(ServerLevel level, BlockPos pos) {
        if (pos == null || level == null) return;
        getSet(level.dimension()).add(pos.immutable());
    }

    public static void unregister(ServerLevel level, BlockPos pos) {
        if (pos == null || level == null) return;
        getSet(level.dimension()).remove(pos);
    }

    public static Set<BlockPos> getAll(ServerLevel level) {
        if (level == null) return Collections.emptySet();
        return Collections.unmodifiableSet(getSet(level.dimension()));
    }
}
