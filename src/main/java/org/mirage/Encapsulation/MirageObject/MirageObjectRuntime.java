package org.mirage.Encapsulation.MirageObject;

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

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public final class MirageObjectRuntime {

    private static final List<MirageObjectEntity> ALL = new CopyOnWriteArrayList<>();

    private MirageObjectRuntime() {}

    static void track(MirageObjectEntity e) {
        ALL.add(e);
    }

    static void untrack(MirageObjectEntity e) {
        ALL.remove(e);
    }

    public static <T extends MirageObject> List<MirageObjectEntity> getObjects(Class<T> defClass) {
        List<MirageObjectEntity> result = new ArrayList<>();
        for (MirageObjectEntity e : ALL) {
            if (e.getDefinition() != null && defClass.isInstance(e.getDefinition())) {
                result.add(e);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static <T extends MirageObject> Optional<MirageObjectEntity> getObject(
            Class<T> defClass, Predicate<MirageObjectEntity> filter) {

        for (MirageObjectEntity e : getObjects(defClass)) {
            if (filter.test(e)) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    public static <T extends MirageObject> Optional<MirageObjectEntity> getNearest(
            Class<T> defClass, Level level, Vec3 pos, double radius) {

        double r2 = radius * radius;
        MirageObjectEntity best = null;
        double bestDist = Double.MAX_VALUE;

        for (MirageObjectEntity e : getObjects(defClass)) {
            if (e.level() != level) continue;
            double d2 = e.position().distanceToSqr(pos);
            if (d2 <= r2 && d2 < bestDist) {
                bestDist = d2;
                best = e;
            }
        }
        return Optional.ofNullable(best);
    }
}