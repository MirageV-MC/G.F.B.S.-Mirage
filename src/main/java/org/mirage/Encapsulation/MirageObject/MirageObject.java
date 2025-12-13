package org.mirage.Encapsulation.MirageObject;

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
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.core.animation.AnimatableManager;

import java.util.Collections;
import java.util.List;

public abstract class MirageObject {

    private final ResourceLocation id;
    private final ResourceLocation model;
    private final ResourceLocation texture;
    private final ResourceLocation animation;

    protected MirageObject(ResourceLocation id,
                           ResourceLocation model,
                           ResourceLocation texture,
                           ResourceLocation animation) {
        this.id = id;
        this.model = model;
        this.texture = texture;
        this.animation = animation;
    }

    public ResourceLocation getId() {
        return id;
    }

    public ResourceLocation getModel() {
        return model;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public ResourceLocation getAnimation() {
        return animation;
    }

    protected float width;
    protected float height;

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public List<AABB> getCollision(MirageObjectEntity entity) {
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();

        AABB box = new AABB(
                x - width / 2.0,
                y,
                z - width / 2.0,
                x + width / 2.0,
                y + height,
                z + width / 2.0
        );
        return Collections.singletonList(box);
    }

    public void initEntity(MirageObjectEntity entity) {}

    public void serverTick(MirageObjectEntity entity) {}

    public void clientTick(MirageObjectEntity entity) {}

    public abstract void registerControllers(MirageObjectEntity entity,
                                             AnimatableManager.ControllerRegistrar controllers);
}
