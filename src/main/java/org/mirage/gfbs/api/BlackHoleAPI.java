/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>.
 */

package org.mirage.gfbs.api;

import net.minecraft.world.phys.Vec3;
import org.mirage.gfbs.Phenomenon.BlackHole.BlackHole;
import org.mirage.gfbs.Phenomenon.BlackHole.BlackHoleManager;

import java.util.List;

public class BlackHoleAPI {
    public static boolean createBlackHole(String name, double radius, Vec3 position) {
        if (BlackHoleManager.getBlackHole(name) != null) {
            return false;
        }
        BlackHoleManager.createBlackHole(name, radius, 1.0, position);
        return true;
    }

    public static boolean createBlackHole(String name, double radius, double lensing, Vec3 position) {
        if (BlackHoleManager.getBlackHole(name) != null) {
            return false;
        }
        BlackHoleManager.createBlackHole(name, radius, lensing, position);
        return true;
    }

    public static boolean removeBlackHole(String name) {
        if (BlackHoleManager.getBlackHole(name) == null) {
            return false;
        }
        BlackHoleManager.removeBlackHole(name);
        return true;
    }

    public static boolean updateBlackHoleSize(String name, double newRadius) {
        return BlackHoleManager.updateBlackHoleSize(name, newRadius);
    }

    public static boolean updateAccretionDiskOpacity(String name, double opacity) {
        return BlackHoleManager.updateAccretionDiskOpacity(name, opacity);
    }

    public static boolean moveBlackHole(String name, Vec3 newPosition) {
        return BlackHoleManager.moveBlackHole(name, newPosition);
    }

    public static BlackHole getBlackHole(String name) {
        return BlackHoleManager.getBlackHole(name);
    }

    public static List<BlackHole> getAllBlackHoles() {
        return BlackHoleManager.getBlackHoles();
    }

    public static List<String> getAllBlackHoleNames() {
        return BlackHoleManager.getBlackHoleNames();
    }

    public static double getBlackHoleSize(String name) {
        BlackHole blackHole = BlackHoleManager.getBlackHole(name);
        return blackHole != null ? blackHole.getEventHorizonRadius() : -1;
    }

    public static double getAccretionDiskOpacity(String name) {
        BlackHole blackHole = BlackHoleManager.getBlackHole(name);
        return blackHole != null ? blackHole.getAccretionDiskOpacity() : -1;
    }

    public static Vec3 getBlackHolePosition(String name) {
        BlackHole blackHole = BlackHoleManager.getBlackHole(name);
        return blackHole != null ? blackHole.getPosition() : null;
    }
}
