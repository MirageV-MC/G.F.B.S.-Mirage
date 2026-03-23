package org.mirage.gfbs.objects.renderer.ColoredDoor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.mirage.gfbs.objects.items.ColoredDoor.ColoredDoorGeoItem;
import org.mirage.gfbs.objects.items.ColoredDoor.ColoredDoorItemModel;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ColoredDoorItemRenderer extends GeoItemRenderer<ColoredDoorGeoItem> {
    public ColoredDoorItemRenderer() {
        super(new ColoredDoorItemModel());
        this.withScale(0.5f);
    }

    @Override
    public void preRender(PoseStack poseStack, ColoredDoorGeoItem animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.translate(0.0, -0.25, 0.0);
    }
}
