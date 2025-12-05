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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Mirage_gfbs.MODID);

    private static final Map<String, RegistryObject<SoundEvent>> SOUND_EVENT_MAP = new HashMap<>();

    static {
        loadAndRegisterFromSoundsJson();
    }

    private static void loadAndRegisterFromSoundsJson() {
        String path = "/assets/" + Mirage_gfbs.MODID + "/sounds.json";

        try (InputStream is = ModSoundEvents.class.getResourceAsStream(path)) {
            if (is == null) {
                Mirage_gfbs.LOGGER.warn("Cannot find " + path + ", will not register automatically.");
                return;
            }

            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    return;
                }

                JsonObject obj = root.getAsJsonObject();

                for (String key : obj.keySet()) {
                    registerSound(key);
                }

                Mirage_gfbs.LOGGER.info("Automatically registered " + SOUND_EVENT_MAP.size() + " SoundEvents from sounds.json.");
            }
        } catch (Exception e) {
            Mirage_gfbs.LOGGER.error("Failed to automatically register SoundEvent from sounds.json:");
            e.printStackTrace();
        }
    }

    private static void registerSound(String name) {
        RegistryObject<SoundEvent> reg = SOUND_EVENTS.register(
                name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Mirage_gfbs.MODID, name))
        );
        SOUND_EVENT_MAP.put(name, reg);
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }

    public static SoundEvent getSoundOrNull(String name) {
        RegistryObject<SoundEvent> ro = SOUND_EVENT_MAP.get(name);
        return ro != null ? ro.get() : null;
    }

    public static SoundEvent getSoundOrThrow(String name) {
        RegistryObject<SoundEvent> ro = SOUND_EVENT_MAP.get(name);
        if (ro == null) {
            throw new IllegalArgumentException("未知 SoundEvent key: " + name);
        }
        return ro.get();
    }

    public static Collection<String> getAllKeys() {
        return Collections.unmodifiableSet(SOUND_EVENT_MAP.keySet());
    }
}