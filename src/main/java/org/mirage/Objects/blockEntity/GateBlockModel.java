package org.mirage.Objects.blockEntity;

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

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.mirage.Mirage_gfbs;
import software.bernie.geckolib.model.GeoModel;

public class GateBlockModel extends GeoModel<GateBlockEntity> {

    @Override
    public @NotNull ResourceLocation getModelResource(GateBlockEntity animatable) {
        return new ResourceLocation(Mirage_gfbs.MODID, "geo/gate.geo.json");
    }

    @Override
    public @NotNull ResourceLocation getTextureResource(GateBlockEntity animatable) {
        return new ResourceLocation(Mirage_gfbs.MODID, "textures/block/gate.png");
    }

    @Override
    public @NotNull ResourceLocation getAnimationResource(GateBlockEntity animatable) {
        return new ResourceLocation(Mirage_gfbs.MODID, "animations/gate.animation.json");
    }
}
