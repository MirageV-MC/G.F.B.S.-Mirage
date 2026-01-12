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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Pure JSON persistence.
 *
 * File format:
 * {
 *   "version": 1,
 *   "values": {
 *     "some.key": true,
 *     "other.key": 12
 *   }
 * }
 */
final class ClientConfigIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ClientConfigIO() {}

    static JsonObject readOrNull(Path file) {
        Objects.requireNonNull(file, "file");
        if (!Files.exists(file)) return null;

        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement el = JsonParser.parseReader(br);
            if (el != null && el.isJsonObject()) return el.getAsJsonObject();
        } catch (Exception ignored) {
        }
        return null;
    }

    static void write(Path file, JsonObject root) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(root, "root");
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(root, bw);
            }
        } catch (Exception ignored) {
        }
    }

    static JsonObject newRoot() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.add("values", new JsonObject());
        return root;
    }

    static JsonObject values(JsonObject root) {
        if (root == null) return null;
        JsonElement v = root.get("values");
        return (v != null && v.isJsonObject()) ? v.getAsJsonObject() : null;
    }
}
