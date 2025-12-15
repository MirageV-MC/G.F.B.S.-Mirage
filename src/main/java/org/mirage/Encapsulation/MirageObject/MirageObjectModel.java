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

import software.bernie.geckolib.model.GeoModel;
import net.minecraft.resources.ResourceLocation;

public class MirageObjectModel extends GeoModel<MirageObjectEntity> {

    @Override
    public ResourceLocation getModelResource(MirageObjectEntity animatable) {
        return animatable.getDefinition().getModel();
    }

    @Override
    public ResourceLocation getTextureResource(MirageObjectEntity animatable) {
        return animatable.getDefinition().getTexture();
    }

    @Override
    public ResourceLocation getAnimationResource(MirageObjectEntity animatable) {
        return animatable.getDefinition().getAnimation();
    }
}