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

package org.mirage.ClientConfig;

import java.util.Objects;

/**
 * Small helpers to build keys with validation metadata.
 */
public final class ClientConfigBuilder {
    private ClientConfigBuilder() {}

    public static ClientConfigKey<Boolean> bool(String id, String category, String displayName, String comment, boolean def) {
        return new ClientConfigKey<>(id, category, displayName, comment, ClientConfigType.BOOLEAN, def, null, null, null);
    }

    public static ClientConfigKey<Integer> integer(String id, String category, String displayName, String comment, int def, int min, int max) {
        return new ClientConfigKey<>(id, category, displayName, comment, ClientConfigType.INT, def, (double)min, (double)max, null);
    }

    public static ClientConfigKey<Double> dbl(String id, String category, String displayName, String comment, double def, double min, double max) {
        return new ClientConfigKey<>(id, category, displayName, comment, ClientConfigType.DOUBLE, def, min, max, null);
    }

    public static ClientConfigKey<String> str(String id, String category, String displayName, String comment, String def, int maxLen) {
        Objects.requireNonNull(def, "def");
        // maxLen is enforced in UI and in validation; store it in max as Double for simplicity.
        return new ClientConfigKey<>(id, category, displayName, comment, ClientConfigType.STRING, def, null, (double)Math.max(1, maxLen), null);
    }

    public static <E extends Enum<E>> ClientConfigKey<E> enm(String id, String category, String displayName, String comment, Class<E> enumClass, E def) {
        Objects.requireNonNull(enumClass, "enumClass");
        Objects.requireNonNull(def, "def");
        return new ClientConfigKey<>(id, category, displayName, comment, ClientConfigType.ENUM, def, null, null, enumClass);
    }
}
