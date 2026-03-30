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
 * Small helpers to build keys with validation metadata.
 */
public final class ServerConfigBuilder {
    private ServerConfigBuilder() {}

    public static ServerConfigKey<Boolean> bool(String id, String category, String displayName, String comment, boolean def) {
        return new ServerConfigKey<>(id, category, displayName, comment, ServerConfigType.BOOLEAN, def, null, null, null);
    }

    public static ServerConfigKey<Integer> integer(String id, String category, String displayName, String comment, int def, int min, int max) {
        return new ServerConfigKey<>(id, category, displayName, comment, ServerConfigType.INT, def, (double)min, (double)max, null);
    }

    public static ServerConfigKey<Float> flt(String id, String category, String displayName, String comment, float def, float min, float max) {
        return new ServerConfigKey<>(id, category, displayName, comment, ServerConfigType.FLOAT, def, (double)min, (double)max, null);
    }

    public static ServerConfigKey<Double> dbl(String id, String category, String displayName, String comment, double def, double min, double max) {
        return new ServerConfigKey<>(id, category, displayName, comment, ServerConfigType.DOUBLE, def, min, max, null);
    }

    public static ServerConfigKey<String> str(String id, String category, String displayName, String comment, String def, int maxLen) {
        Objects.requireNonNull(def, "def");
        return new ServerConfigKey<>(id, category, displayName, comment, ServerConfigType.STRING, def, null, (double)Math.max(1, maxLen), null);
    }

    public static ServerConfigKey<BlockPos> blockPos(String id, String category, String displayName, String comment, BlockPos def) {
        Objects.requireNonNull(def, "def");
        return new ServerConfigKey<>(id, category, displayName, comment, ServerConfigType.BLOCK_POS, def, null, null, null);
    }

    public static <E extends Enum<E>> ServerConfigKey<E> enm(String id, String category, String displayName, String comment, Class<E> enumClass, E def) {
        Objects.requireNonNull(enumClass, "enumClass");
        Objects.requireNonNull(def, "def");
        return new ServerConfigKey<>(id, category, displayName, comment, ServerConfigType.ENUM, def, null, null, enumClass);
    }
}
