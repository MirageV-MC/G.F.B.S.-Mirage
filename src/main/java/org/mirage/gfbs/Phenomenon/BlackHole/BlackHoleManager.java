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

package org.mirage.gfbs.Phenomenon.BlackHole;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

public class BlackHoleManager {
    private static final Map<String, BlackHole> blackHoles = new HashMap<>();
    private static ServerLevel currentLevel = null;

    public static void setLevel(ServerLevel level) {
        currentLevel = level;
    }

    public static void createBlackHole(String name, double radius, double lensing, Vec3 pos) {
        synchronized (blackHoles) {
            if (blackHoles.containsKey(name)) {
                return;
            }
            blackHoles.put(name, new BlackHole(name, radius, lensing, pos));
        }
    }

    public static boolean updateBlackHoleSize(String name, double newRadius) {
        synchronized (blackHoles) {
            BlackHole blackHole = blackHoles.get(name);
            if (blackHole != null) {
                blackHole.setEventHorizonRadius(newRadius);
                return true;
            }
            return false;
        }
    }

    public static boolean updateAccretionDiskOpacity(String name, double opacity) {
        synchronized (blackHoles) {
            BlackHole blackHole = blackHoles.get(name);
            if (blackHole != null) {
                blackHole.setAccretionDiskOpacity(opacity);
                return true;
            }
            return false;
        }
    }

    public static void removeBlackHole(String name) {
        synchronized (blackHoles) {
            BlackHole blackHole = blackHoles.get(name);
            if (blackHole != null) {
                blackHole.startDespawnAnimation();
            }
        }
    }

    public static void tick() {
        synchronized (blackHoles) {
            Iterator<Map.Entry<String, BlackHole>> iterator = blackHoles.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, BlackHole> entry = iterator.next();
                BlackHole blackHole = entry.getValue();

                blackHole.updateAnimation();

                if (blackHole.isReadyForRemoval()) {
                    if (blackHole.isBlockBased() && currentLevel != null) {
                        Vec3 pos = blackHole.getPosition();
                        BlockPos blockPos = new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z));
                        currentLevel.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                    iterator.remove();
                }
            }
        }
    }

    public static List<BlackHole> getBlackHoles() {
        synchronized (blackHoles) {
            return Collections.unmodifiableList(new ArrayList<>(blackHoles.values()));
        }
    }

    public static void createBlackHole(double radius, double lensing, Vec3 pos) {
        createBlackHole("BlackHole_" + System.currentTimeMillis(), radius, lensing, pos);
    }

    public static BlackHole getBlackHole(String name) {
        synchronized (blackHoles) {
            return blackHoles.get(name);
        }
    }

    public static boolean moveBlackHole(String name, Vec3 newPosition) {
        synchronized (blackHoles) {
            BlackHole blackHole = blackHoles.get(name);
            if (blackHole != null) {
                blackHole.setPosition(newPosition);
                return true;
            }
            return false;
        }
    }

    public static List<String> getBlackHoleNames() {
        synchronized (blackHoles) {
            return Collections.unmodifiableList(new ArrayList<>(blackHoles.keySet()));
        }
    }
}
