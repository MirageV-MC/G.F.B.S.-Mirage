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

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;

import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Collections;
import java.util.List;

public class MirageObjectEntity extends PathfinderMob implements GeoEntity {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final ObjectStateContainer states = new ObjectStateContainer();

    private MirageObject definition;
    private EntityDimensions customDimensions = null;

    public MirageObjectEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        MirageObjectRuntime.track(this);
    }

    public void setDefinition(MirageObject definition) {
        this.definition = definition;
        if (definition != null) {
            setCustomDimensions(definition.getWidth(), definition.getHeight());
            definition.initEntity(this);
        }
    }

    public List<AABB> getCollision() {
        if (definition != null) {
            return definition.getCollision(this);
        }
        return Collections.singletonList(this.getBoundingBox());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        if (definition != null) {
            definition.registerControllers(this, controllers);
        }
    }

    public MirageObject getDefinition() {
        return definition;
    }

    public ObjectStateContainer getStates() {
        return states;
    }

    public void setCustomDimensions(float width, float height) {
        this.customDimensions = EntityDimensions.scalable(width, height);
        this.refreshDimensions();
    }

    public void clearCustomDimensions() {
        this.customDimensions = null;
        this.refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return customDimensions != null ? customDimensions : super.getDimensions(pose);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && definition != null) {
            definition.serverTick(this);
        } else if (level().isClientSide && definition != null) {
            definition.clientTick(this);
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        MirageObjectRuntime.untrack(this);
    }
}