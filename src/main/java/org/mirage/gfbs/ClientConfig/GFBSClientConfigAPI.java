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
import java.util.function.Consumer;

/**
 * Public internal API for other GFBS client modules to:
 * - register new client configs
 * - get/set configs
 * - listen for config changes
 *
 * This is NOT Forge's ForgeConfigSpec system; it's a lightweight client-only JSON system.
 */
public final class GFBSClientConfigAPI {

    private GFBSClientConfigAPI() {}

    /** Register a config key. Must be called before the config screen is opened (recommended during client setup). */
    public static <T> ClientConfigKey<T> register(ClientConfigKey<T> key) {
        return ClientConfigRegistry.register(key);
    }

    public static ClientConfigKey<Boolean> bool(String id, String category, String displayName, String comment, boolean def) {
        return register(ClientConfigBuilder.bool(id, category, displayName, comment, def));
    }

    public static ClientConfigKey<Integer> integer(String id, String category, String displayName, String comment, int def, int min, int max) {
        return register(ClientConfigBuilder.integer(id, category, displayName, comment, def, min, max));
    }

    public static ClientConfigKey<Double> dbl(String id, String category, String displayName, String comment, double def, double min, double max) {
        return register(ClientConfigBuilder.dbl(id, category, displayName, comment, def, min, max));
    }

    public static ClientConfigKey<String> str(String id, String category, String displayName, String comment, String def, int maxLen) {
        return register(ClientConfigBuilder.str(id, category, displayName, comment, def, maxLen));
    }

    public static <E extends Enum<E>> ClientConfigKey<E> enm(String id, String category, String displayName, String comment, Class<E> enumClass, E def) {
        return register(ClientConfigBuilder.enm(id, category, displayName, comment, enumClass, def));
    }

    /** Get current value (falls back to default if absent). */
    public static <T> T get(ClientConfigKey<T> key) {
        return ClientConfigRegistry.get(key);
    }

    /** Set new value (validated, persisted, and listeners notified). */
    public static <T> void set(ClientConfigKey<T> key, T value) {
        ClientConfigRegistry.set(key, value);
    }

    /** Add per-key listener. */
    public static <T> void onChange(ClientConfigKey<T> key, ClientConfigChangeListener<T> listener) {
        ClientConfigRegistry.addListener(key, listener);
    }

    /**
     * Add a global listener that sees all key changes.
     * Receives (key, oldValue, newValue) as an Object payload.
     */
    public static void onAnyChange(Consumer<ClientConfigRegistry.AnyChange> listener) {
        Objects.requireNonNull(listener, "listener");
        ClientConfigRegistry.addAnyListener(listener);
    }

    /** Force reload from disk (keeps already-registered keys). */
    public static void reloadFromDisk() {
        ClientConfigRegistry.reload();
    }

    /** Force save to disk. */
    public static void saveToDisk() {
        ClientConfigRegistry.save();
    }
}
