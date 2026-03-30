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

package org.mirage.gfbs.ServerConfig;

import net.minecraft.core.BlockPos;

import java.util.Objects;

/**
 * A strongly-typed key describing one server-side configuration entry.
 */
public final class ServerConfigKey<T> {
    private final String id;
    private final String category;
    private final String displayName;
    private final String comment;
    private final ServerConfigType type;
    private final T defaultValue;

    private final Double min;
    private final Double max;

    private final Class<? extends Enum<?>> enumClass;

    ServerConfigKey(String id,
                    String category,
                    String displayName,
                    String comment,
                    ServerConfigType type,
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
    public ServerConfigType type() { return type; }
    public T defaultValue() { return defaultValue; }

    public Double min() { return min; }
    public Double max() { return max; }

    @SuppressWarnings("unchecked")
    public Class<? extends Enum<?>> enumClass() { return enumClass; }

    @Override
    public String toString() {
        return "ServerConfigKey{" + id + ", type=" + type + "}";
    }
}
