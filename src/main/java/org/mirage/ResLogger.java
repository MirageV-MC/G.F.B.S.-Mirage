/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Convex89524

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

package org.mirage;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = Mirage_gfbs.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ResLogger {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String TARGET_MOD_ID = Mirage_gfbs.MODID;

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ResourceManagerReloadListener() {
            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                logSoundFiles(manager);
                logTextureFiles(manager);
            }
        });
    }

    private static void logSoundFiles(ResourceManager manager) {
        String soundPath = "sounds";

        Collection<ResourceLocation> resources = manager.listResources(
                soundPath,
                location -> location.getNamespace().equals(TARGET_MOD_ID) &&
                        location.getPath().endsWith(".ogg")
        ).keySet();

        resources.forEach(loc -> {
            String path = loc.getPath();
            LOGGER.info("Found .ogg file: {}:{}", loc.getNamespace(), path);
        });
    }

    private static void logTextureFiles(ResourceManager manager) {
        String soundPath = "textures";

        Collection<ResourceLocation> resources = manager.listResources(
                soundPath,
                location -> location.getNamespace().equals(TARGET_MOD_ID) &&
                        location.getPath().endsWith(".png")
        ).keySet();

        resources.forEach(loc -> {
            String path = loc.getPath();
            LOGGER.info("Found .png file: {}:{}", loc.getNamespace(), path);
        });
    }
}