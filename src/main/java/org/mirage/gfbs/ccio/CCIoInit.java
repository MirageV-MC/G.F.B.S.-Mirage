package org.mirage.gfbs.ccio;

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

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import org.mirage.gfbs.ccio.api.CCIoApiRegistry;
import org.mirage.gfbs.ccio.peripheral.PeripheralCapabilityAttacher;

public final class CCIoInit {
    private CCIoInit() {}

    private static boolean registered = false;

    public static void init(IEventBus modBus) {
        if(registered)return;
        registered = true;
        MinecraftForge.EVENT_BUS.register(PeripheralCapabilityAttacher.class);
        CCIoApiRegistry.registerDefaults();

        registerApi();
    }

    private static void registerApi() {
        org.mirage.gfbs.ccio.app.ollama.OllamaApis.register();
        org.mirage.gfbs.ccio.app.famsApi.FamsApis.register();
        org.mirage.gfbs.ccio.event.CCIoEventApis.register();
    }
}
