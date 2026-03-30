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

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Public API for server-side config management.
 * Provides easy access to read and modify server configurations.
 * 
 * Config key format example: "server.gfbs.reactor.startuped"
 */
public final class ServerConfigApi {

    private ServerConfigApi() {}

    // ==================== Registration Methods ====================

    public static ServerConfigKey<Boolean> registerBool(String id, String category, String displayName, String comment, boolean def) {
        return ServerConfigRegistry.register(ServerConfigBuilder.bool(id, category, displayName, comment, def));
    }

    public static ServerConfigKey<Integer> registerInt(String id, String category, String displayName, String comment, int def, int min, int max) {
        return ServerConfigRegistry.register(ServerConfigBuilder.integer(id, category, displayName, comment, def, min, max));
    }

    public static ServerConfigKey<Float> registerFloat(String id, String category, String displayName, String comment, float def, float min, float max) {
        return ServerConfigRegistry.register(ServerConfigBuilder.flt(id, category, displayName, comment, def, min, max));
    }

    public static ServerConfigKey<Double> registerDouble(String id, String category, String displayName, String comment, double def, double min, double max) {
        return ServerConfigRegistry.register(ServerConfigBuilder.dbl(id, category, displayName, comment, def, min, max));
    }

    public static ServerConfigKey<String> registerString(String id, String category, String displayName, String comment, String def, int maxLen) {
        return ServerConfigRegistry.register(ServerConfigBuilder.str(id, category, displayName, comment, def, maxLen));
    }

    public static ServerConfigKey<BlockPos> registerBlockPos(String id, String category, String displayName, String comment, BlockPos def) {
        return ServerConfigRegistry.register(ServerConfigBuilder.blockPos(id, category, displayName, comment, def));
    }

    public static <E extends Enum<E>> ServerConfigKey<E> registerEnum(String id, String category, String displayName, String comment, Class<E> enumClass, E def) {
        return ServerConfigRegistry.register(ServerConfigBuilder.enm(id, category, displayName, comment, enumClass, def));
    }

    // ==================== Generic Get/Set by Key ====================

    public static <T> T get(ServerConfigKey<T> key) {
        return ServerConfigRegistry.get(key);
    }

    public static <T> void set(ServerConfigKey<T> key, T value) {
        ServerConfigRegistry.set(key, value);
    }

    // ==================== Direct Value Access by Config ID ====================

    /**
     * Get boolean value by config ID.
     * @param configId The config key ID (e.g., "server.gfbs.reactor.startuped")
     * @param defaultValue Default value if key not found
     * @return The config value or default
     */
    public static boolean getBoolean(String configId, boolean defaultValue) {
        ServerConfigKey<?> key = getKey(configId);
        if (key == null || key.type() != ServerConfigType.BOOLEAN) return defaultValue;
        return ServerConfigRegistry.get((ServerConfigKey<Boolean>) key);
    }

    /**
     * Get integer value by config ID.
     * @param configId The config key ID
     * @param defaultValue Default value if key not found
     * @return The config value or default
     */
    public static int getInt(String configId, int defaultValue) {
        ServerConfigKey<?> key = getKey(configId);
        if (key == null || key.type() != ServerConfigType.INT) return defaultValue;
        return ServerConfigRegistry.get((ServerConfigKey<Integer>) key);
    }

    /**
     * Get float value by config ID.
     * @param configId The config key ID
     * @param defaultValue Default value if key not found
     * @return The config value or default
     */
    public static float getFloat(String configId, float defaultValue) {
        ServerConfigKey<?> key = getKey(configId);
        if (key == null || key.type() != ServerConfigType.FLOAT) return defaultValue;
        return ServerConfigRegistry.get((ServerConfigKey<Float>) key);
    }

    /**
     * Get double value by config ID.
     * @param configId The config key ID
     * @param defaultValue Default value if key not found
     * @return The config value or default
     */
    public static double getDouble(String configId, double defaultValue) {
        ServerConfigKey<?> key = getKey(configId);
        if (key == null || key.type() != ServerConfigType.DOUBLE) return defaultValue;
        return ServerConfigRegistry.get((ServerConfigKey<Double>) key);
    }

    /**
     * Get string value by config ID.
     * @param configId The config key ID
     * @param defaultValue Default value if key not found
     * @return The config value or default
     */
    public static String getString(String configId, String defaultValue) {
        ServerConfigKey<?> key = getKey(configId);
        if (key == null || key.type() != ServerConfigType.STRING) return defaultValue;
        return ServerConfigRegistry.get((ServerConfigKey<String>) key);
    }

    /**
     * Get BlockPos value by config ID.
     * @param configId The config key ID
     * @param defaultValue Default value if key not found
     * @return The config value or default
     */
    public static BlockPos getBlockPos(String configId, BlockPos defaultValue) {
        ServerConfigKey<?> key = getKey(configId);
        if (key == null || key.type() != ServerConfigType.BLOCK_POS) return defaultValue;
        return ServerConfigRegistry.get((ServerConfigKey<BlockPos>) key);
    }

    /**
     * Get enum value by config ID.
     * @param configId The config key ID
     * @param enumClass The enum class
     * @param defaultValue Default value if key not found
     * @return The config value or default
     */
    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> E getEnum(String configId, Class<E> enumClass, E defaultValue) {
        ServerConfigKey<?> key = getKey(configId);
        if (key == null || key.type() != ServerConfigType.ENUM) return defaultValue;
        try {
            return ServerConfigRegistry.get((ServerConfigKey<E>) key);
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    // ==================== Set Value by Config ID ====================

    /**
     * Set boolean value by config ID.
     * @param configId The config key ID
     * @param value The new value
     * @return true if successful, false if key not found or type mismatch
     */
    public static boolean setBoolean(String configId, boolean value) {
        ServerConfigKey<?> key = getKey(configId);
        if (key == null || key.type() != ServerConfigType.BOOLEAN) return false;
        ServerConfigRegistry.set((ServerConfigKey<Boolean>) key, value);
        return true;
    }

    /**
     * Set integer value by config ID.
     * @param configId The config key ID
     * @param value The new value
     * @return true if successful, false if key not found or type mismatch
     */
    public static boolean setInt(String configId, int value) {
        ServerConfigKey<?> key = getKey(configId);
        if (key == null || key.type() != ServerConfigType.INT) return false;
        ServerConfigRegistry.set((ServerConfigKey<Integer>) key, value);
        return true;
    }

    /**
     * Set float value by config ID.
     * @param configId The config key ID
     * @param value The new value
     * @return true if successful, false if key not found or type mismatch
     */
    public static boolean setFloat(String configId, float value) {
        ServerConfigKey<?> key = getKey(configId);
        if (key == null || key.type() != ServerConfigType.FLOAT) return false;
        ServerConfigRegistry.set((ServerConfigKey<Float>) key, value);
        return true;
    }

    /**
     * Set double value by config ID.
     * @param configId The config key ID
     * @param value The new value
     * @return true if successful, false if key not found or type mismatch
     */
    public static boolean setDouble(String configId, double value) {
        ServerConfigKey<?> key = getKey(configId);
        if (key == null || key.type() != ServerConfigType.DOUBLE) return false;
        ServerConfigRegistry.set((ServerConfigKey<Double>) key, value);
        return true;
    }

    /**
     * Set string value by config ID.
     * @param configId The config key ID
     * @param value The new value
     * @return true if successful, false if key not found or type mismatch
     */
    public static boolean setString(String configId, String value) {
        ServerConfigKey<?> key = getKey(configId);
        if (key == null || key.type() != ServerConfigType.STRING) return false;
        ServerConfigRegistry.set((ServerConfigKey<String>) key, value);
        return true;
    }

    /**
     * Set BlockPos value by config ID.
     * @param configId The config key ID
     * @param value The new value
     * @return true if successful, false if key not found or type mismatch
     */
    public static boolean setBlockPos(String configId, BlockPos value) {
        ServerConfigKey<?> key = getKey(configId);
        if (key == null || key.type() != ServerConfigType.BLOCK_POS) return false;
        ServerConfigRegistry.set((ServerConfigKey<BlockPos>) key, value);
        return true;
    }

    /**
     * Set enum value by config ID.
     * @param configId The config key ID
     * @param value The new value
     * @return true if successful, false if key not found or type mismatch
     */
    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> boolean setEnum(String configId, E value) {
        ServerConfigKey<?> key = getKey(configId);
        if (key == null || key.type() != ServerConfigType.ENUM) return false;
        ServerConfigRegistry.set((ServerConfigKey<E>) key, value);
        return true;
    }

    // ==================== Utility Methods ====================

    /**
     * Get a config key by ID.
     * @param configId The config key ID
     * @return The key or null if not found
     */
    public static ServerConfigKey<?> getKey(String configId) {
        Objects.requireNonNull(configId, "configId");
        for (ServerConfigKey<?> key : ServerConfigRegistry.allKeys()) {
            if (key.id().equals(configId)) {
                return key;
            }
        }
        return null;
    }

    /**
     * Check if a config key exists.
     * @param configId The config key ID
     * @return true if exists
     */
    public static boolean hasKey(String configId) {
        return getKey(configId) != null;
    }

    /**
     * Get the type of a config key.
     * @param configId The config key ID
     * @return The type or null if not found
     */
    public static ServerConfigType getType(String configId) {
        ServerConfigKey<?> key = getKey(configId);
        return key != null ? key.type() : null;
    }

    /**
     * Add a listener for a specific config key.
     */
    public static <T> void onChange(ServerConfigKey<T> key, ServerConfigChangeListener<T> listener) {
        ServerConfigRegistry.addListener(key, listener);
    }

    /**
     * Add a global listener that sees all key changes.
     */
    public static void onAnyChange(Consumer<ServerConfigRegistry.AnyChange> listener) {
        Objects.requireNonNull(listener, "listener");
        ServerConfigRegistry.addAnyListener(listener);
    }

    /**
     * Force reload from disk.
     */
    public static void reloadFromDisk() {
        ServerConfigRegistry.reload();
    }

    /**
     * Force save to disk.
     */
    public static void saveToDisk() {
        ServerConfigRegistry.save();
    }

    /**
     * Initialize/load config if needed.
     */
    public static void init() {
        ServerConfigRegistry.loadIfNeeded();
    }

    /**
     * Get all registered config keys.
     * @return Collection of all config keys
     */
    public static Collection<ServerConfigKey<?>> allKeys() {
        return ServerConfigRegistry.allKeys();
    }

    /**
     * Get all config categories.
     * @return List of category names
     */
    public static List<String> categories() {
        return ServerConfigRegistry.categories();
    }
}
