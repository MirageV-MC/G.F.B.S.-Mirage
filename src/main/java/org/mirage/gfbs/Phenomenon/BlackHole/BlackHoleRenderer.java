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

package org.mirage.gfbs.Phenomenon.BlackHole;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL13;
import org.mirage.gfbs.MirageGFBS;

import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BlackHoleRenderer {
    private static final ResourceLocation EVENT_HORIZON_TEXTURE = new ResourceLocation(MirageGFBS.MODID, "textures/entity/black_hole.png");

    // 最大支持的黑洞数量，与着色器中的数组大小保持一致
    private static final int MAX_BLACK_HOLES = 8;

    @SubscribeEvent
    public static void onWorldRenderLast(RenderLevelStageEvent event) {
        // 此渲染器已禁用，避免出现多余的透明黑球
        // 黑洞现在由 BlackHoleProRenderer 负责渲染
    }

    private static void renderEventHorizon(RenderLevelStageEvent event, BlackHole blackHole) {
        PoseStack poseStack = event.getPoseStack();
        Vec3 pos = blackHole.getPosition();
        double radius = blackHole.getRenderRadius(event.getPartialTick());

        poseStack.pushPose();
        poseStack.translate(pos.x - event.getCamera().getPosition().x,
                pos.y - event.getCamera().getPosition().y,
                pos.z - event.getCamera().getPosition().z);

        // 计算相机距离以调整透明度
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        double distance = camera.getPosition().distanceTo(pos);
        float alpha = (float) Mth.clamp(1.0 - distance / 100.0, 0.1, 0.9);

        // 获取VertexConsumer
        VertexConsumer consumer = Minecraft.getInstance().renderBuffers().bufferSource()
                .getBuffer(RenderType.entityTranslucentCull(EVENT_HORIZON_TEXTURE));

        // 细分球体渲染（减少细分程度以提高性能）
        int stacks = 64;
        int slices = 64;

        // 生成球体顶点
        for (int i = 0; i < stacks; ++i) {
            double theta1 = Math.PI * i / stacks;
            double theta2 = Math.PI * (i + 1) / stacks;

            for (int j = 0; j < slices; ++j) {
                double phi1 = 2 * Math.PI * j / slices;
                double phi2 = 2 * Math.PI * (j + 1) / slices;

                // 计算四个顶点
                Vec3 v1 = getSpherePoint(radius, theta1, phi1);
                Vec3 v2 = getSpherePoint(radius, theta1, phi2);
                Vec3 v3 = getSpherePoint(radius, theta2, phi2);
                Vec3 v4 = getSpherePoint(radius, theta2, phi1);

                // 绘制两个三角形形成一个四边形面片
                drawQuad(poseStack, consumer, v1, v2, v3, v4, alpha);
            }
        }

        poseStack.popPose();
    }

    // 计算球体上的点
    private static Vec3 getSpherePoint(double radius, double theta, double phi) {
        double x = radius * Math.sin(theta) * Math.cos(phi);
        double y = radius * Math.sin(theta) * Math.sin(phi);
        double z = radius * Math.cos(theta);
        return new Vec3(x, y, z);
    }

    // 绘制四边形（优化版本）
    private static void drawQuad(PoseStack poseStack, VertexConsumer consumer, Vec3 v1, Vec3 v2, Vec3 v3, Vec3 v4, float alpha) {
        Vec3 normal = v2.subtract(v1).cross(v3.subtract(v1)).normalize();

        int overlay = 0;
        int light = 15728880;
        float nx = (float) normal.x;
        float ny = (float) normal.y;
        float nz = (float) normal.z;

        consumer.vertex(poseStack.last().pose(), (float) v1.x, (float) v1.y, (float) v1.z)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(0, 0)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(nx, ny, nz)
                .endVertex();

        consumer.vertex(poseStack.last().pose(), (float) v2.x, (float) v2.y, (float) v2.z)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(0, 1)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(nx, ny, nz)
                .endVertex();

        consumer.vertex(poseStack.last().pose(), (float) v3.x, (float) v3.y, (float) v3.z)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(1, 1)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(nx, ny, nz)
                .endVertex();

        consumer.vertex(poseStack.last().pose(), (float) v4.x, (float) v4.y, (float) v4.z)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(1, 0)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(nx, ny, nz)
                .endVertex();
    }

//    private static void applyLensingEffect(RenderLevelStageEvent event, List<BlackHole> blackHoles) {
//        if (blackHoles.isEmpty()) return;
//
//        Minecraft minecraft = Minecraft.getInstance();
//
//        // 直接使用已注册的着色器实例
//        ShaderInstance shaderInstance = Mirage_gfbs.ShaderRegistry.LENSING_SHADER_INSTANCE;
//        if (shaderInstance == null) {
//            Mirage_gfbs.LOGGER.error("Lensing shader not initialized");
//            return;
//        }
//
//        // 保存当前渲染状态
//        minecraft.getMainRenderTarget().bindWrite(false);
//
//        try {
//            // 设置着色器参数
//            setShaderUniforms(shaderInstance, minecraft, event, blackHoles);
//
//            // 应用着色器
//            shaderInstance.apply();
//
//            // 渲染全屏四边形
//            renderFullscreenQuad();
//
//        } catch (Exception e) {
//            Mirage_gfbs.LOGGER.error("Failed to apply lensing effect: {}", e.getMessage());
//        } finally {
//            shaderInstance.clear();
//            // 恢复默认着色器
//            minecraft.gameRenderer.blitShader.clear();
//        }
//    }

    private static void setShaderUniforms(ShaderInstance shaderInstance, Minecraft minecraft,
                                          RenderLevelStageEvent event, List<BlackHole> blackHoles) {
        // 设置投影矩阵
        Uniform projMatUniform = shaderInstance.getUniform("ProjMat");
        if (projMatUniform != null) {
            projMatUniform.set(minecraft.gameRenderer.getProjectionMatrix(event.getPartialTick()));
        }

        // 设置视图矩阵
        Uniform viewMatUniform = shaderInstance.getUniform("ViewMat");
        if (viewMatUniform != null) {
            Matrix4f viewMat = event.getPoseStack().last().pose();
            viewMatUniform.set(viewMat);
        }

        // 设置逆投影矩阵
        Uniform invProjMatUniform = shaderInstance.getUniform("InvProjMat");
        if (invProjMatUniform != null) {
            Matrix4f projMat = minecraft.gameRenderer.getProjectionMatrix(event.getPartialTick());
            Matrix4f invProjMat = new Matrix4f(projMat);
            invProjMat.invert();
            invProjMatUniform.set(invProjMat);
        }

        // 设置逆视图矩阵
        Uniform invViewMatUniform = shaderInstance.getUniform("InvViewMat");
        if (invViewMatUniform != null) {
            Matrix4f viewMat = event.getPoseStack().last().pose();
            Matrix4f invViewMat = new Matrix4f(viewMat);
            invViewMat.invert();
            invViewMatUniform.set(invViewMat);
        }

        // 设置相机位置
        Uniform cameraPosUniform = shaderInstance.getUniform("CameraPos");
        if (cameraPosUniform != null) {
            Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();
            cameraPosUniform.set((float) cameraPos.x, (float) cameraPos.y, (float) cameraPos.z);
        }

        // 设置屏幕尺寸
        Uniform screenSizeUniform = shaderInstance.getUniform("ScreenSize");
        if (screenSizeUniform != null) {
            screenSizeUniform.set(
                    (float) minecraft.getWindow().getWidth(),
                    (float) minecraft.getWindow().getHeight()
            );
        }

        // 设置颜色采样器
        Uniform diffuseSamplerUniform = shaderInstance.getUniform("DiffuseSampler");
        if (diffuseSamplerUniform != null) {
            // 绑定颜色纹理到纹理单元0
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
            GlStateManager._bindTexture(minecraft.getMainRenderTarget().getColorTextureId());
            diffuseSamplerUniform.set(0);
        }

        // 设置深度采样器
        Uniform depthSamplerUniform = shaderInstance.getUniform("DepthSampler");
        if (depthSamplerUniform != null) {
            // 绑定深度纹理到纹理单元1
            GlStateManager._activeTexture(GL13.GL_TEXTURE1);
            GlStateManager._bindTexture(minecraft.getMainRenderTarget().getDepthTextureId());
            depthSamplerUniform.set(1);

            // 恢复活动纹理单元到0
            GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        }

        // 为每个黑洞设置参数
        int blackHoleCount = Math.min(blackHoles.size(), MAX_BLACK_HOLES);
        for (int i = 0; i < blackHoleCount; i++) {
            BlackHole blackHole = blackHoles.get(i);

            Uniform blackHolePosUniform = shaderInstance.getUniform("BlackHolePos[" + i + "]");
            if (blackHolePosUniform != null) {
                Vec3 pos = blackHole.getPosition();
                blackHolePosUniform.set((float) pos.x, (float) pos.y, (float) pos.z);
            }

            Uniform lensingFactorUniform = shaderInstance.getUniform("LensingFactor[" + i + "]");
            if (lensingFactorUniform != null) {
                lensingFactorUniform.set((float) blackHole.getLensingFactor());
            }

            Uniform eventHorizonUniform = shaderInstance.getUniform("EventHorizon[" + i + "]");
            if (eventHorizonUniform != null) {
                eventHorizonUniform.set((float) blackHole.getRenderRadius(event.getPartialTick()));
            }
        }

        // 设置黑洞数量
        Uniform blackHoleCountUniform = shaderInstance.getUniform("BlackHoleCount");
        if (blackHoleCountUniform != null) {
            blackHoleCountUniform.set(blackHoleCount);
        }
    }

    // 添加全屏四边形渲染方法
    private static void renderFullscreenQuad() {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        bufferBuilder.vertex(-1.0, -1.0, 0.0).uv(0.0F, 0.0F).endVertex();
        bufferBuilder.vertex(1.0, -1.0, 0.0).uv(1.0F, 0.0F).endVertex();
        bufferBuilder.vertex(1.0, 1.0, 0.0).uv(1.0F, 1.0F).endVertex();
        bufferBuilder.vertex(-1.0, 1.0, 0.0).uv(0.0F, 1.0F).endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());
    }
}