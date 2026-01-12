package org.mirage.gfbs.ccio.app.ollama;

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

import com.google.gson.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public final class OllamaClient {
    private static final Gson GSON = new Gson();

    private final String baseUrl; // e.g. http://127.0.0.1:11434

    public OllamaClient(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) throw new IllegalArgumentException("baseUrl");
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public String chatOnce(String model, JsonArray messages, JsonObject options, String format) throws IOException {
        JsonObject req = new JsonObject();
        req.addProperty("model", model);
        req.add("messages", messages);
        req.addProperty("stream", false);
        if (options != null) req.add("options", options);
        if (format != null && !format.isBlank()) req.addProperty("format", format);

        JsonObject resp = postJson(baseUrl + "/api/chat", req);
        // chat non-stream response: { message: { role, content }, done: true, ... } :contentReference[oaicite:1]{index=1}
        JsonObject msg = resp.getAsJsonObject("message");
        if (msg == null || !msg.has("content")) return "";
        return safeGetString(msg, "content");
    }

    public void chatStream(String model, JsonArray messages, JsonObject options, String format, Consumer<String> onChunk) throws IOException {
        JsonObject req = new JsonObject();
        req.addProperty("model", model);
        req.add("messages", messages);
        req.addProperty("stream", true);
        if (options != null) req.add("options", options);
        if (format != null && !format.isBlank()) req.addProperty("format", format);

        postJsonStream(baseUrl + "/api/chat", req, (lineObj) -> {
            // stream chunk format: each line is JSON; partial content in message.content :contentReference[oaicite:2]{index=2}
            JsonObject msg = lineObj.getAsJsonObject("message");
            if (msg != null && msg.has("content")) {
                String part = safeGetString(msg, "content");
                if (!part.isEmpty()) onChunk.accept(part);
            }
        });
    }

    private static String safeGetString(JsonObject obj, String key) {
        JsonElement e = obj.get(key);
        return (e != null && e.isJsonPrimitive()) ? e.getAsString() : "";
    }

    private static JsonObject postJson(String url, JsonObject body) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(300_000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        byte[] payload = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload);
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        String text = readAll(is);

        if (code < 200 || code >= 300) {
            throw new IOException("Ollama HTTP " + code + ": " + text);
        }

        JsonElement root = JsonParser.parseString(text);
        if (!root.isJsonObject()) throw new IOException("Invalid JSON: " + text);
        return root.getAsJsonObject();
    }

    private static void postJsonStream(String url, JsonObject body, Consumer<JsonObject> onLineObject) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(0); // streaming
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        byte[] payload = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload);
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (code < 200 || code >= 300) {
            String err = readAll(is);
            throw new IOException("Ollama HTTP " + code + ": " + err);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                JsonElement el;
                try {
                    el = JsonParser.parseString(line);
                } catch (Exception ignored) {
                    continue;
                }
                if (!el.isJsonObject()) continue;

                JsonObject obj = el.getAsJsonObject();
                onLineObject.accept(obj);

                if (obj.has("done") && obj.get("done").isJsonPrimitive() && obj.get("done").getAsBoolean()) {
                    break;
                }
            }
        }
    }

    private static String readAll(InputStream is) throws IOException {
        if (is == null) return "";
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) >= 0) baos.write(buf, 0, n);
            return baos.toString(StandardCharsets.UTF_8);
        }
    }
}
