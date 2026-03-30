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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import org.mirage.gfbs.MirageGFBS;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Central registry + runtime store for server-side configs.
 * Supports hot-reload and direct value access.
 */
public final class ServerConfigRegistry {

    /** Change record for global listeners. */
    public static final class AnyChange {
        public final ServerConfigKey<?> key;
        public final Object oldValue;
        public final Object newValue;

        AnyChange(ServerConfigKey<?> key, Object oldValue, Object newValue) {
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }

    private static final Map<String, ServerConfigKey<?>> KEYS = new LinkedHashMap<>();
    private static final Map<String, Object> VALUES = new ConcurrentHashMap<>();
    private static final Map<String, List<ServerConfigChangeListener<?>>> LISTENERS = new ConcurrentHashMap<>();
    private static final List<Consumer<AnyChange>> ANY_LISTENERS = Collections.synchronizedList(new ArrayList<>());

    private static final Path FILE = MirageGFBS.CONFIG_DIR.resolve("gfbs-server-config.json");

    private static boolean loadedOnce = false;

    private ServerConfigRegistry() {}

    public static synchronized <T> ServerConfigKey<T> register(ServerConfigKey<T> key) {
        Objects.requireNonNull(key, "key");
        if (KEYS.containsKey(key.id())) {
            throw new IllegalStateException("Duplicate server config key: " + key.id());
        }
        KEYS.put(key.id(), key);

        if (loadedOnce) {
            if (!VALUES.containsKey(key.id())) {
                VALUES.put(key.id(), key.defaultValue());
            }
        }
        return key;
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(ServerConfigKey<T> key) {
        Objects.requireNonNull(key, "key");
        Object v = VALUES.get(key.id());
        if (v == null) return key.defaultValue();
        return (T) v;
    }

    public static synchronized <T> void set(ServerConfigKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        T validated = validateValue(key, value);
        T old = get(key);

        if (Objects.equals(old, validated)) return;

        VALUES.put(key.id(), validated);
        save();

        List<ServerConfigChangeListener<?>> list = LISTENERS.get(key.id());
        if (list != null) {
            for (ServerConfigChangeListener<?> l : new ArrayList<>(list)) {
                @SuppressWarnings("unchecked")
                ServerConfigChangeListener<T> lt = (ServerConfigChangeListener<T>) l;
                try { lt.onChanged(key, old, validated); } catch (Exception ignored) {}
            }
        }

        AnyChange change = new AnyChange(key, old, validated);
        synchronized (ANY_LISTENERS) {
            for (Consumer<AnyChange> l : new ArrayList<>(ANY_LISTENERS)) {
                try { l.accept(change); } catch (Exception ignored) {}
            }
        }
    }

    public static <T> void addListener(ServerConfigKey<T> key, ServerConfigChangeListener<T> listener) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(listener, "listener");
        LISTENERS.computeIfAbsent(key.id(), k -> Collections.synchronizedList(new ArrayList<>())).add(listener);
    }

    public static void addAnyListener(Consumer<AnyChange> listener) {
        ANY_LISTENERS.add(listener);
    }

    public static Collection<ServerConfigKey<?>> allKeys() {
        return Collections.unmodifiableCollection(KEYS.values());
    }

    public static List<String> categories() {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (ServerConfigKey<?> k : KEYS.values()) set.add(k.category());
        return new ArrayList<>(set);
    }

    public static synchronized void reload() {
        load(true);
    }

    public static synchronized void loadIfNeeded() {
        if (!loadedOnce) load(false);
    }

    private static void load(boolean force) {
        if (loadedOnce && !force) return;

        JsonObject root = ServerConfigIO.readOrNull(FILE);
        if (root == null) root = ServerConfigIO.newRoot();
        JsonObject valuesObj = ServerConfigIO.values(root);
        if (valuesObj == null) {
            valuesObj = new JsonObject();
            root.add("values", valuesObj);
        }

        Map<String, Object> oldValues = new HashMap<>(VALUES);

        for (ServerConfigKey<?> key : KEYS.values()) {
            VALUES.put(key.id(), key.defaultValue());
        }

        for (Map.Entry<String, JsonElement> e : valuesObj.entrySet()) {
            ServerConfigKey<?> key = KEYS.get(e.getKey());
            if (key == null) continue;
            Object parsed = parseJsonValue(key, e.getValue());
            if (parsed != null) {
                VALUES.put(key.id(), parsed);
            }
        }

        loadedOnce = true;

        for (ServerConfigKey<?> key : KEYS.values()) {
            Object newValue = VALUES.get(key.id());
            Object oldValue = oldValues.get(key.id());

            if (!Objects.equals(oldValue, newValue)) {
                triggerChangeListeners(key, oldValue, newValue);
            }
        }
    }

    public static synchronized void save() {
        JsonObject root = ServerConfigIO.readOrNull(FILE);
        if (root == null) root = ServerConfigIO.newRoot();

        JsonObject valuesObj = ServerConfigIO.values(root);
        if (valuesObj == null) {
            valuesObj = new JsonObject();
            root.add("values", valuesObj);
        }

        for (ServerConfigKey<?> key : KEYS.values()) {
            Object v = VALUES.getOrDefault(key.id(), key.defaultValue());
            valuesObj.add(key.id(), toJsonValue(key, v));
        }

        ServerConfigIO.write(FILE, root);
    }

    @SuppressWarnings("unchecked")
    private static <T> void triggerChangeListeners(ServerConfigKey<T> key, Object oldValue, Object newValue) {
        List<ServerConfigChangeListener<?>> list = LISTENERS.get(key.id());
        if (list != null) {
            for (ServerConfigChangeListener<?> l : new ArrayList<>(list)) {
                ServerConfigChangeListener<T> lt = (ServerConfigChangeListener<T>) l;
                try {
                    lt.onChanged(key, (T) oldValue, (T) newValue);
                } catch (Exception ignored) {}
            }
        }

        AnyChange change = new AnyChange(key, oldValue, newValue);
        synchronized (ANY_LISTENERS) {
            for (Consumer<AnyChange> l : new ArrayList<>(ANY_LISTENERS)) {
                try { l.accept(change); } catch (Exception ignored) {}
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T validateValue(ServerConfigKey<T> key, T value) {
        switch (key.type()) {
            case BOOLEAN -> {
                if (!(value instanceof Boolean)) return key.defaultValue();
                return value;
            }
            case INT -> {
                if (!(value instanceof Integer)) return key.defaultValue();
                int i = (Integer) value;
                int min = key.min() == null ? Integer.MIN_VALUE : key.min().intValue();
                int max = key.max() == null ? Integer.MAX_VALUE : key.max().intValue();
                if (i < min) i = min;
                if (i > max) i = max;
                return (T) Integer.valueOf(i);
            }
            case FLOAT -> {
                if (!(value instanceof Float)) return key.defaultValue();
                float f = (Float) value;
                float min = key.min() == null ? -Float.MAX_VALUE : key.min().floatValue();
                float max = key.max() == null ? Float.MAX_VALUE : key.max().floatValue();
                if (f < min) f = min;
                if (f > max) f = max;
                return (T) Float.valueOf(f);
            }
            case DOUBLE -> {
                if (!(value instanceof Double)) return key.defaultValue();
                double d = (Double) value;
                double min = key.min() == null ? -Double.MAX_VALUE : key.min();
                double max = key.max() == null ? Double.MAX_VALUE : key.max();
                if (d < min) d = min;
                if (d > max) d = max;
                return (T) Double.valueOf(d);
            }
            case STRING -> {
                if (!(value instanceof String)) return key.defaultValue();
                String s = (String) value;
                int maxLen = key.max() == null ? 1024 : key.max().intValue();
                if (s.length() > maxLen) s = s.substring(0, maxLen);
                return (T) s;
            }
            case BLOCK_POS -> {
                if (!(value instanceof BlockPos)) return key.defaultValue();
                return value;
            }
            case ENUM -> {
                if (key.enumClass() == null) return key.defaultValue();
                Class<? extends Enum<?>> cls = key.enumClass();
                if (cls.isInstance(value)) return value;

                if (value instanceof String str) {
                    for (Enum<?> c : cls.getEnumConstants()) {
                        if (c.name().equalsIgnoreCase(str)) {
                            return (T) c;
                        }
                    }
                }
                return key.defaultValue();
            }
        }
        return key.defaultValue();
    }

    private static Object parseJsonValue(ServerConfigKey<?> key, JsonElement el) {
        try {
            return switch (key.type()) {
                case BOOLEAN -> el.isJsonPrimitive() ? el.getAsBoolean() : null;
                case INT -> el.isJsonPrimitive() ? el.getAsInt() : null;
                case FLOAT -> el.isJsonPrimitive() ? el.getAsFloat() : null;
                case DOUBLE -> el.isJsonPrimitive() ? el.getAsDouble() : null;
                case STRING -> el.isJsonPrimitive() ? el.getAsString() : null;
                case BLOCK_POS -> {
                    if (!el.isJsonPrimitive()) yield null;
                    String posStr = el.getAsString();
                    String[] parts = posStr.split(",");
                    if (parts.length != 3) yield null;
                    yield new BlockPos(
                            Integer.parseInt(parts[0].trim()),
                            Integer.parseInt(parts[1].trim()),
                            Integer.parseInt(parts[2].trim())
                    );
                }
                case ENUM -> {
                    if (!el.isJsonPrimitive() || key.enumClass() == null) yield null;
                    String name = el.getAsString();
                    for (Enum<?> c : key.enumClass().getEnumConstants()) {
                        if (c.name().equalsIgnoreCase(name)) yield c;
                    }
                    yield null;
                }
            };
        } catch (Exception ignored) {
            return null;
        }
    }

    private static com.google.gson.JsonElement toJsonValue(ServerConfigKey<?> key, Object value) {
        return switch (key.type()) {
            case BOOLEAN -> new com.google.gson.JsonPrimitive(Boolean.TRUE.equals(value));
            case INT -> new com.google.gson.JsonPrimitive(((Number) value).intValue());
            case FLOAT -> new com.google.gson.JsonPrimitive(((Number) value).floatValue());
            case DOUBLE -> new com.google.gson.JsonPrimitive(((Number) value).doubleValue());
            case STRING -> new com.google.gson.JsonPrimitive(String.valueOf(value));
            case BLOCK_POS -> {
                BlockPos pos = (BlockPos) value;
                yield new com.google.gson.JsonPrimitive(pos.getX() + "," + pos.getY() + "," + pos.getZ());
            }
            case ENUM -> new com.google.gson.JsonPrimitive(value instanceof Enum<?> e ? e.name() : String.valueOf(value));
        };
    }
}
