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

package org.mirage.Phenomenon.network.Network;

import net.minecraft.nbt.CompoundTag;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ClientEventHandler {
    private static final Map<String, Consumer<CompoundTag>> EVENT_REGISTRY = new ConcurrentHashMap<>();

    public static void registerEvent(String eventId, Consumer<CompoundTag> handler) {
        EVENT_REGISTRY.put(eventId, handler);
    }

    public static void handleEvent(String eventId, CompoundTag data) {
        Consumer<CompoundTag> handler = EVENT_REGISTRY.get(eventId);
        if (handler != null) {
            handler.accept(data);
        }
    }
}