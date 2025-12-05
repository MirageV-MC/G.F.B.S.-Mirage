package org.mirage.Objects.renderer;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Convex89524

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

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import org.joml.Matrix4f;
import org.mirage.Objects.blocks.Bases.PictureBlock.PictureBlockEntity;
import org.mirage.Objects.blocks.Bases.PictureBlock.PictureConfig;

public class PictureBlockRenderer implements BlockEntityRenderer<PictureBlockEntity> {

    public PictureBlockRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(PictureBlockEntity be, float partialTicks, PoseStack pose,
                       MultiBufferSource buffer, int light, int overlay) {

        PictureConfig cfg = be.config;
        if (cfg == null) return;

        int wPix = cfg.widthPixels();
        int hPix = cfg.heightPixels();

        float targetW = 1.0f;
        float targetH = 1.0f;

        switch (cfg.scalingMode()) {

            case FIT_INSIDE:
                float ratio = (float) wPix / hPix;

                if (ratio > 1) {
                    targetH = 1.0f / ratio;
                } else {
                    targetW = ratio;
                }
                break;

            case STRETCH:
                break;

            case ORIGINAL_SIZE:
                targetW = wPix / 16f;
                targetH = hPix / 16f;
                break;
        }

        pose.pushPose();
        pose.scale(targetW, targetH, 1.0f);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(cfg.texture()));
        Matrix4f mat = pose.last().pose();

        addQuad(vc, mat, light);
        pose.popPose();
    }

    private void addQuad(VertexConsumer vc, Matrix4f mat, int light) {
        vc.vertex(mat, -0.5f, -0.5f, 0.0f).uv(0, 1).uv2(light).endVertex();
        vc.vertex(mat,  0.5f, -0.5f, 0.0f).uv(1, 1).uv2(light).endVertex();
        vc.vertex(mat,  0.5f,  0.5f, 0.0f).uv(1, 0).uv2(light).endVertex();
        vc.vertex(mat, -0.5f,  0.5f, 0.0f).uv(0, 0).uv2(light).endVertex();
    }
}
