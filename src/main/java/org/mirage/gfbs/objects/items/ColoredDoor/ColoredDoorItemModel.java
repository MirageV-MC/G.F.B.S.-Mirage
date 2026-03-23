package org.mirage.gfbs.objects.items.ColoredDoor;

import net.minecraft.resources.ResourceLocation;
import org.mirage.gfbs.MirageGFBS;
import software.bernie.geckolib.model.GeoModel;

public class ColoredDoorItemModel extends GeoModel<ColoredDoorGeoItem> {
    @Override
    public ResourceLocation getModelResource(ColoredDoorGeoItem animatable) {
        return new ResourceLocation(MirageGFBS.MODID, "geo/door.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ColoredDoorGeoItem animatable) {
        return animatable.getTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(ColoredDoorGeoItem animatable) {
        return new ResourceLocation(MirageGFBS.MODID, "animations/door.animation.json");
    }
}
