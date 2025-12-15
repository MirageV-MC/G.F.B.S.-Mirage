package org.mirage.Encapsulation.MirageObject;

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

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MirageObjectRegistry {

    private static final Map<ResourceLocation, MirageObject> BY_ID = new LinkedHashMap<>();

    private MirageObjectRegistry() {}

    public static <T extends MirageObject> T register(T object) {
        ResourceLocation id = object.getId();
        if (BY_ID.containsKey(id)) {
            throw new IllegalStateException("Duplicate MirageObject id: " + id);
        }
        BY_ID.put(id, object);
        return object;
    }

    @Nullable
    public static MirageObject get(ResourceLocation id) {
        return BY_ID.get(id);
    }

    public static Collection<MirageObject> values() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }
}
