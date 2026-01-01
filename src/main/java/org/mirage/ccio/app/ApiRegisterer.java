package org.mirage.ccio.app;

import net.minecraft.server.MinecraftServer;
import org.mirage.Objects.blocks.Control.Gate.GateTypes;
import org.mirage.api.GateClientAPI;
import org.mirage.ccio.api.CCIoApiRegistry;

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

import org.mirage.ccio.app.famsApi.FamsApis;

public class ApiRegisterer {
    public static void register(MinecraftServer server) {
        CCIoApiRegistry.register("gfbs.gate.api.open", (level, pos, computer, args) -> {
            GateClientAPI.openAll();
            return 0;
        });

        CCIoApiRegistry.register("gfbs.gate.api.close", (level, pos, computer, args) -> {
            GateClientAPI.closeAll();
            return 0;
        });

        CCIoApiRegistry.register("gfbs.check_point_gate.api.open", (level, pos, computer, args) -> {
            GateClientAPI.openAll(GateTypes.CHECK_POINT);
            return 0;
        });

        CCIoApiRegistry.register("gfbs.check_point_gate.api.close", (level, pos, computer, args) -> {
            GateClientAPI.closeAll(GateTypes.CHECK_POINT);
            return 0;
        });

        FamsApis.register();
    }
}
