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

package org.mirage.gfbs.ClientConfig;

import java.util.Objects;

/**
 * A strongly-typed key describing one client-side configuration entry.
 */
public final class ClientConfigKey<T> {
    private final String id;
    private final String category;
    private final String displayName;
    private final String comment;
    private final ClientConfigType type;
    private final T defaultValue;

    // Optional constraints (used by INT/DOUBLE)
    private final Double min;
    private final Double max;

    // Only for ENUM
    private final Class<? extends Enum<?>> enumClass;

    ClientConfigKey(String id,
                    String category,
                    String displayName,
                    String comment,
                    ClientConfigType type,
                    T defaultValue,
                    Double min,
                    Double max,
                    Class<? extends Enum<?>> enumClass) {
        this.id = Objects.requireNonNull(id, "id");
        this.category = category == null ? "general" : category;
        this.displayName = displayName == null ? id : displayName;
        this.comment = comment == null ? "" : comment;
        this.type = Objects.requireNonNull(type, "type");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        this.min = min;
        this.max = max;
        this.enumClass = enumClass;
    }

    public String id() { return id; }
    public String category() { return category; }
    public String displayName() { return displayName; }
    public String comment() { return comment; }
    public ClientConfigType type() { return type; }
    public T defaultValue() { return defaultValue; }

    public Double min() { return min; }
    public Double max() { return max; }

    @SuppressWarnings("unchecked")
    public Class<? extends Enum<?>> enumClass() { return enumClass; }

    @Override
    public String toString() {
        return "ClientConfigKey{" + id + ", type=" + type + "}";
    }
}
