package org.mirage.Encapsulation.MirageObject;

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

import java.util.Objects;

public final class ObjectStateKey<T> {
    private final String name;
    private final Class<T> type;

    private ObjectStateKey(String name, Class<T> type) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
    }

    public static ObjectStateKey<Boolean> bool(String name) {
        return new ObjectStateKey<>(name, Boolean.class);
    }

    public static ObjectStateKey<Integer> intKey(String name) {
        return new ObjectStateKey<>(name, Integer.class);
    }

    public static ObjectStateKey<Float> floatKey(String name) {
        return new ObjectStateKey<>(name, Float.class);
    }

    public static <T> ObjectStateKey<T> of(String name, Class<T> type) {
        return new ObjectStateKey<>(name, type);
    }

    public String getName() {
        return name;
    }

    public Class<T> getType() {
        return type;
    }

    @Override
    public String toString() {
        return "ObjectStateKey[" + name + "]";
    }
}