package org.mirage.Objects.renderer.Gate;

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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.mirage.Objects.blockEntity.CheckPointGate.CheckPointGateBlockEntity;
import org.mirage.Objects.blockEntity.CheckPointGate.CheckPointGateBlockModel;
import org.mirage.Objects.blocks.classs.Gate.CheckPointGateBlock;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class CheckPointGateBlockRenderer extends GeoBlockRenderer<CheckPointGateBlockEntity> {

    public CheckPointGateBlockRenderer(BlockEntityRendererProvider.Context ctx) {
        super(new CheckPointGateBlockModel());
    }

    @Override
    public void actuallyRender(@NotNull PoseStack poseStack,
                               @NotNull CheckPointGateBlockEntity blockEntity,
                               @NotNull BakedGeoModel model,
                               @NotNull RenderType renderType,
                               @NotNull MultiBufferSource bufferSource,
                               @NotNull VertexConsumer buffer,
                               boolean isReRender,
                               float partialTick,
                               int packedLight,
                               int packedOverlay,
                               float red,
                               float green,
                               float blue,
                               float alpha) {

        poseStack.pushPose();

        if (blockEntity.getLevel() != null) {
            var state = blockEntity.getBlockState();
            if (state.hasProperty(CheckPointGateBlock.AXIS)) {
                Direction.Axis axis = state.getValue(CheckPointGateBlock.AXIS);
                if (axis == Direction.Axis.Z) {
                    poseStack.translate(0.5, 0, 0.5);
                    poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));
                    poseStack.translate(-0.5, 0, -0.5);
                }
            }
        }

        super.actuallyRender(poseStack, blockEntity, model, renderType,
                bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha);

        poseStack.popPose();
    }
}
