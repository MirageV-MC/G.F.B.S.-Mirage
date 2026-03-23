package org.mirage.gfbs.objects.renderer.ColoredDoor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.mirage.gfbs.objects.blockEntity.ColoredDoor.ColoredDoorBlockEntity;
import org.mirage.gfbs.objects.blockEntity.ColoredDoor.ColoredDoorModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class ColoredDoorRenderer extends GeoBlockRenderer<ColoredDoorBlockEntity> {

    public ColoredDoorRenderer(BlockEntityRendererProvider.Context ctx) {
        super(new ColoredDoorModel());
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull ColoredDoorBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        switch (facing) {
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0f));
            default -> {
            }
        }
    }
}
