package org.mirage.ccio.api;

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

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CCIoApiRegistry {
    private static final Map<String, ICCIoFunction> APIS = new ConcurrentHashMap<>();

    private CCIoApiRegistry() {}

    public static void register(String name, ICCIoFunction fn) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name");
        if (fn == null) throw new IllegalArgumentException("fn");
        APIS.put(name, fn);
    }

    public static Object invoke(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, String name, Object[] args) throws LuaException {
        ICCIoFunction fn = APIS.get(name);
        if (fn == null) throw new LuaException("Unknown API: " + name);
        return fn.call(level, bridgePos, computer, args == null ? new Object[0] : args);
    }

    public static void registerDefaults() {
            org.mirage.ccio.app.famsApi.FamsApis.register();
    }
}
