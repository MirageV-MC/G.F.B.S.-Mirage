package org.mirage.ccio.app.ollama;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.mirage.ccio.api.CCIoApiRegistry;
import org.mirage.ccio.api.ICCIoStreamResult;

public final class OllamaApis {
    private OllamaApis() {}

    private static String defaultBaseUrl() { return "http://127.0.0.1:11434"; } // :contentReference[oaicite:4]{index=4}
    private static String defaultModel() { return "llama3.2"; }

    public static void register() {
        CCIoApiRegistry.register("ollama.chat", OllamaApis::chatOnce);
        CCIoApiRegistry.register("ollama.chat_stream", OllamaApis::chatStream);
    }

    /**
     * args:
     * 0: prompt (string) OR messages(table array)
     * 1: model? (string)
     * 2: options? (table/map) -> will be forwarded to ollama "options"
     * 3: format? (string) -> e.g. "json"
     */
    private static Object chatOnce(ServerLevel level, BlockPos pos, IComputerAccess computer, Object[] args) throws LuaException {
        String prompt = getStringArg(args, 0, null);
        String model = getStringArg(args, 1, defaultModel());
        JsonObject options = getOptionsArg(args, 2);
        String format = getStringArg(args, 3, null);

        if (prompt == null || prompt.isBlank()) throw new LuaException("ollama.chat: prompt is empty");

        JsonArray messages = new JsonArray();
        JsonObject m = new JsonObject();
        m.addProperty("role", "user");
        m.addProperty("content", prompt);
        messages.add(m);

        try {
            String baseUrl = resolveBaseUrl(args, 4);
            OllamaClient client = new OllamaClient(baseUrl);
            return client.chatOnce(model, messages, options, format);
        } catch (Exception e) {
            throw new LuaException("ollama.chat failed: " + e.getMessage());
        }
    }

    private static Object chatStream(ServerLevel level, BlockPos pos, IComputerAccess computer, Object[] args) throws LuaException {
        String prompt = getStringArg(args, 0, null);
        String model = getStringArg(args, 1, defaultModel());
        JsonObject options = getOptionsArg(args, 2);
        String format = getStringArg(args, 3, null);

        if (prompt == null || prompt.isBlank()) throw new LuaException("ollama.chat_stream: prompt is empty");

        JsonArray messages = new JsonArray();
        JsonObject m = new JsonObject();
        m.addProperty("role", "user");
        m.addProperty("content", prompt);
        messages.add(m);

        return (ICCIoStreamResult) (consumer) -> {
            String baseUrl = resolveBaseUrl(args, 4);
            OllamaClient client = new OllamaClient(baseUrl);
            client.chatStream(model, messages, options, format, consumer);
        };
    }

    private static String getStringArg(Object[] args, int idx, String def) {
        if (args == null || idx < 0 || idx >= args.length) return def;
        Object v = args[idx];
        if (v == null) return def;
        return String.valueOf(v);
    }

    @SuppressWarnings("unchecked")
    private static JsonObject getOptionsArg(Object[] args, int idx) {
        if (args == null || idx < 0 || idx >= args.length) return null;
        Object v = args[idx];
        if (!(v instanceof java.util.Map<?, ?> map)) return null;

        JsonObject obj = new JsonObject();
        for (var e : map.entrySet()) {
            if (e.getKey() == null) continue;
            String k = String.valueOf(e.getKey());
            Object val = e.getValue();
            if (val instanceof Number n) obj.addProperty(k, n);
            else if (val instanceof Boolean b) obj.addProperty(k, b);
            else if (val != null) obj.addProperty(k, String.valueOf(val));
        }
        return obj;
    }

    private static String resolveBaseUrl(Object[] args, int idx) {
        if (args == null || idx < 0 || idx >= args.length) {
            return defaultBaseUrl();
        }
        Object v = args[idx];
        if (v == null) return defaultBaseUrl();

        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return defaultBaseUrl();

        if (s.startsWith("http://") || s.startsWith("https://")) {
            return s;
        }

        return "http://" + s;
    }
}
