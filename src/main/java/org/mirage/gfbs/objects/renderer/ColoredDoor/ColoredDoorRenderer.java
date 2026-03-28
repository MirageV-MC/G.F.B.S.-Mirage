package org.mirage.gfbs.objects.renderer.ColoredDoor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mirage.gfbs.objects.blockEntity.ColoredDoor.ColoredDoorBlockEntity;
import org.mirage.gfbs.objects.blockEntity.ColoredDoor.ColoredDoorModel;
import org.mirage.gfbs.objects.blocks.classs.ColoredDoor.ColoredDoorBlock;
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
    public @Nullable RenderType getRenderType(
            ColoredDoorBlockEntity animatable,
            ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource,
            float partialTick
    ) {
        return RenderType.entityCutoutNoCull(texture);
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

        if (this.animatable != null) {
            BlockState state = this.animatable.getBlockState();
            if (state.hasProperty(ColoredDoorBlock.HINGE)) {
                if (state.getValue(ColoredDoorBlock.HINGE) == DoorHingeSide.RIGHT) {
                    poseStack.scale(-1.0f, 1.0f, 1.0f);
                }
            }
        }
    }
}
