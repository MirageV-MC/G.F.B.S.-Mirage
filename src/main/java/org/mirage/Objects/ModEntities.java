package org.mirage.Objects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.mirage.Encapsulation.MirageObject.MirageObjectEntity;

import static org.mirage.Mirage_gfbs.MODID;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Convex89524
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

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

    public static final RegistryObject<EntityType<MirageObjectEntity>> MIRAGE_OBJECT =
            ENTITIES.register("mirage_object", () ->
                    EntityType.Builder.<MirageObjectEntity>of(MirageObjectEntity::new, MobCategory.MISC)
                            .sized(0.8F, 1.8F)
                            .build(new ResourceLocation(MODID, "mirage_object").toString())
            );

    public static void init(){
        var a = MIRAGE_OBJECT;
    }
}
