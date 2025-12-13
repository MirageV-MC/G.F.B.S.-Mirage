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

import java.util.HashMap;
import java.util.Map;

public final class ObjectStateContainer {
    private final Map<ObjectStateKey<?>, Object> values = new HashMap<>();

    public <T> void set(ObjectStateKey<T> key, T value) {
        if (value != null && !key.getType().isInstance(value)) {
            throw new IllegalArgumentException("Wrong value type for key " + key + ": " + value.getClass());
        }
        values.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(ObjectStateKey<T> key) {
        return (T) values.get(key);
    }

    public <T> T getOrDefault(ObjectStateKey<T> key, T defaultValue) {
        T v = get(key);
        return v != null ? v : defaultValue;
    }

    public boolean has(ObjectStateKey<?> key) {
        return values.containsKey(key);
    }

    public void remove(ObjectStateKey<?> key) {
        values.remove(key);
    }

    public void clear() {
        values.clear();
    }
}