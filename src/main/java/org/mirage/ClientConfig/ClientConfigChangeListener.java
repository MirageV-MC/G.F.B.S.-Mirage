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

@FunctionalInterface
public interface ClientConfigChangeListener<T> {
    /**
     * Called when a config value changes (after validation and persistence).
     *
     * @param key      the changed key
     * @param oldValue previous value (never null)
     * @param newValue new value (never null)
     */
    void onChanged(ClientConfigKey<T> key, T oldValue, T newValue);
}
