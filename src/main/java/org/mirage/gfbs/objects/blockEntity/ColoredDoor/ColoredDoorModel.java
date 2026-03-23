package org.mirage.gfbs.objects.blockEntity.ColoredDoor;

import net.minecraft.resources.ResourceLocation;
import org.mirage.gfbs.MirageGFBS;
import org.mirage.gfbs.objects.blocks.classs.ColoredDoor.ColoredDoorBlock;
import software.bernie.geckolib.model.GeoModel;

public class ColoredDoorModel extends GeoModel<ColoredDoorBlockEntity> {

    @Override
    public ResourceLocation getModelResource(ColoredDoorBlockEntity object) {
        return new ResourceLocation(MirageGFBS.MODID, "geo/door.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ColoredDoorBlockEntity object) {
        if (object.getBlockState().getBlock() instanceof ColoredDoorBlock block) {
            switch (block.getColor()) {
                case RED:
                    return new ResourceLocation(MirageGFBS.MODID, "textures/block/doors/door_red.png");
                case BLACK:
                    return new ResourceLocation(MirageGFBS.MODID, "textures/block/doors/door_black.png");
                case ORANGE:
                    return new ResourceLocation(MirageGFBS.MODID, "textures/block/doors/door_orange.png");
                case BLUE:
                default:
                    return new ResourceLocation(MirageGFBS.MODID, "textures/block/doors/door_blue.png");
            }
        }
        return new ResourceLocation(MirageGFBS.MODID, "textures/block/doors/door_blue.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ColoredDoorBlockEntity animatable) {
        return new ResourceLocation(MirageGFBS.MODID, "animations/door.animation.json");
    }
}
