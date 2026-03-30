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

package org.mirage.gfbs.ServerConfig.instance;

import net.minecraft.core.BlockPos;
import org.mirage.gfbs.ServerConfig.ServerConfigApi;
import org.mirage.gfbs.ServerConfig.ServerConfigKey;

public final class GFBSServerConfig {

    private GFBSServerConfig() {}

    public static final ServerConfigKey<BlockPos> REACTOR_CORE_POSITION = 
        ServerConfigApi.registerBlockPos(
            "server.gfbs.reactor.core_position",
            "reactor",
            "反应堆核心位置",
            "反应堆核心的坐标位置",
            BlockPos.ZERO
        );

    // ==================== Convenience Methods ====================

    public static BlockPos getCorePosition() {
        return ServerConfigApi.get(REACTOR_CORE_POSITION);
    }

    public static void setCorePosition(BlockPos value) {
        ServerConfigApi.set(REACTOR_CORE_POSITION, value);
    }

    /**
     * Initialize this config instance. Call during server startup.
     */
    public static void init() {}
}
