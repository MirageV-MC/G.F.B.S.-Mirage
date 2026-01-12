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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.mirage.gfbs.Mirage_gfbs;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Central registry + runtime store for client-only configs.
 */
public final class ClientConfigRegistry {

    /** Change record for global listeners. */
    public static final class AnyChange {
        public final ClientConfigKey<?> key;
        public final Object oldValue;
        public final Object newValue;

        AnyChange(ClientConfigKey<?> key, Object oldValue, Object newValue) {
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }

    private static final Map<String, ClientConfigKey<?>> KEYS = new LinkedHashMap<>();
    private static final Map<String, Object> VALUES = new ConcurrentHashMap<>();
    private static final Map<String, List<ClientConfigChangeListener<?>>> LISTENERS = new ConcurrentHashMap<>();
    private static final List<Consumer<AnyChange>> ANY_LISTENERS = Collections.synchronizedList(new ArrayList<>());

    private static final Path FILE = Mirage_gfbs.CONFIG_DIR.resolve("gfbs-client-config.json");

    private static boolean loadedOnce = false;

    private ClientConfigRegistry() {}

    public static synchronized <T> ClientConfigKey<T> register(ClientConfigKey<T> key) {
        Objects.requireNonNull(key, "key");
        if (KEYS.containsKey(key.id())) {
            throw new IllegalStateException("Duplicate client config key: " + key.id());
        }
        KEYS.put(key.id(), key);

        // If we already loaded file once, try hydrate this new key from disk snapshot (if present).
        if (loadedOnce) {
            // keep current value if already set
            if (!VALUES.containsKey(key.id())) {
                VALUES.put(key.id(), key.defaultValue());
            }
        }
        return key;
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(ClientConfigKey<T> key) {
        Objects.requireNonNull(key, "key");
        Object v = VALUES.get(key.id());
        if (v == null) return key.defaultValue();
        return (T) v;
    }

    public static synchronized <T> void set(ClientConfigKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        T validated = validateValue(key, value);
        T old = get(key);

        if (Objects.equals(old, validated)) return;

        VALUES.put(key.id(), validated);
        save(); // persist immediately (client-only; small file)

        // notify per-key listeners
        List<ClientConfigChangeListener<?>> list = LISTENERS.get(key.id());
        if (list != null) {
            for (ClientConfigChangeListener<?> l : new ArrayList<>(list)) {
                @SuppressWarnings("unchecked")
                ClientConfigChangeListener<T> lt = (ClientConfigChangeListener<T>) l;
                try { lt.onChanged(key, old, validated); } catch (Exception ignored) {}
            }
        }

        // notify global listeners
        AnyChange change = new AnyChange(key, old, validated);
        synchronized (ANY_LISTENERS) {
            for (Consumer<AnyChange> l : new ArrayList<>(ANY_LISTENERS)) {
                try { l.accept(change); } catch (Exception ignored) {}
            }
        }
    }

    public static <T> void addListener(ClientConfigKey<T> key, ClientConfigChangeListener<T> listener) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(listener, "listener");
        LISTENERS.computeIfAbsent(key.id(), k -> Collections.synchronizedList(new ArrayList<>())).add(listener);
    }

    public static void addAnyListener(Consumer<AnyChange> listener) {
        ANY_LISTENERS.add(listener);
    }

    public static Collection<ClientConfigKey<?>> allKeys() {
        return Collections.unmodifiableCollection(KEYS.values());
    }

    public static List<String> categories() {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (ClientConfigKey<?> k : KEYS.values()) set.add(k.category());
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

        JsonObject root = ClientConfigIO.readOrNull(FILE);
        if (root == null) root = ClientConfigIO.newRoot();
        JsonObject valuesObj = ClientConfigIO.values(root);
        if (valuesObj == null) {
            valuesObj = new JsonObject();
            root.add("values", valuesObj);
        }

        Map<String, Object> oldValues = new HashMap<>(VALUES);

        for (ClientConfigKey<?> key : KEYS.values()) {
            VALUES.put(key.id(), key.defaultValue());
        }

        for (Map.Entry<String, JsonElement> e : valuesObj.entrySet()) {
            ClientConfigKey<?> key = KEYS.get(e.getKey());
            if (key == null) continue;
            Object parsed = parseJsonValue(key, e.getValue());
            if (parsed != null) {
                VALUES.put(key.id(), parsed);
            }
        }

        loadedOnce = true;

        for (ClientConfigKey<?> key : KEYS.values()) {
            Object newValue = VALUES.get(key.id());
            Object oldValue = oldValues.get(key.id());

            if (!Objects.equals(oldValue, newValue)) {
                triggerChangeListeners(key, oldValue, newValue);
            }
        }
    }

    public static synchronized void save() {
        JsonObject root = ClientConfigIO.readOrNull(FILE);
        if (root == null) root = ClientConfigIO.newRoot();

        JsonObject valuesObj = ClientConfigIO.values(root);
        if (valuesObj == null) {
            valuesObj = new JsonObject();
            root.add("values", valuesObj);
        }

        // write all known keys (do not delete unknown keys)
        for (ClientConfigKey<?> key : KEYS.values()) {
            Object v = VALUES.getOrDefault(key.id(), key.defaultValue());
            valuesObj.add(key.id(), toJsonValue(key, v));
        }

        ClientConfigIO.write(FILE, root);
    }

    @SuppressWarnings("unchecked")
    private static <T> void triggerChangeListeners(ClientConfigKey<T> key, Object oldValue, Object newValue) {
        List<ClientConfigChangeListener<?>> list = LISTENERS.get(key.id());
        if (list != null) {
            for (ClientConfigChangeListener<?> l : new ArrayList<>(list)) {
                ClientConfigChangeListener<T> lt = (ClientConfigChangeListener<T>) l;
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

    // ---------------- validation + json conversion ----------------

    @SuppressWarnings("unchecked")
    private static <T> T validateValue(ClientConfigKey<T> key, T value) {
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
            case ENUM -> {
                if (key.enumClass() == null) return key.defaultValue();
                Class<? extends Enum<?>> cls = key.enumClass();
                if (cls.isInstance(value)) return value;

                // allow setting by name string
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

    private static Object parseJsonValue(ClientConfigKey<?> key, JsonElement el) {
        try {
            return switch (key.type()) {
                case BOOLEAN -> el.isJsonPrimitive() ? el.getAsBoolean() : null;
                case INT -> el.isJsonPrimitive() ? el.getAsInt() : null;
                case DOUBLE -> el.isJsonPrimitive() ? el.getAsDouble() : null;
                case STRING -> el.isJsonPrimitive() ? el.getAsString() : null;
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

    private static com.google.gson.JsonElement toJsonValue(ClientConfigKey<?> key, Object value) {
        com.google.gson.JsonPrimitive p;
        return switch (key.type()) {
            case BOOLEAN -> new com.google.gson.JsonPrimitive(Boolean.TRUE.equals(value));
            case INT -> new com.google.gson.JsonPrimitive(((Number) value).intValue());
            case DOUBLE -> new com.google.gson.JsonPrimitive(((Number) value).doubleValue());
            case STRING -> new com.google.gson.JsonPrimitive(String.valueOf(value));
            case ENUM -> new com.google.gson.JsonPrimitive(value instanceof Enum<?> e ? e.name() : String.valueOf(value));
        };
    }
}
