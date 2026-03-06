package org.mirage.gfbs.Phenomenon.BlackHole;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.mirage.gfbs.MirageGFBS;

import java.lang.reflect.Method;

import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BlackHoleProRenderer {
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        List<BlackHole> blackHoles = BlackHoleManager.getBlackHoles();
        if (blackHoles.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        ShaderInstance shader = BlackHoleShaderManager.getBlackHoleShader();
        if (mc.level == null || mc.player == null || shader == null) return;

        for (BlackHole blackHole : blackHoles) {
            if (blackHole.getAnimationState() == BlackHole.AnimationState.REMOVED) continue;
            renderBlackHole(event, blackHole, shader);
        }
    }

    private static void renderBlackHole(RenderLevelStageEvent event, BlackHole blackHole, ShaderInstance shader) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();

        RenderSystem.backupProjectionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        RenderSystem.depthMask(true);

        try {
            PoseStack viewStack = new PoseStack();
            viewStack.pushPose();

            double fov = getFov(mc, camera, event.getPartialTick());
            Matrix4f projectionMatrix = mc.gameRenderer.getProjectionMatrix((float) fov);

            viewStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
            viewStack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
            Matrix4f modelViewMatrix = viewStack.last().pose();

            RenderSystem.setShader(() -> shader);

            shader.setSampler("DepthSampler", mc.getMainRenderTarget().getDepthTextureId());
            shader.setSampler("TextureSampler", createNoiseTexture());
            shader.setSampler("RenderTargetSampler", mc.getMainRenderTarget().getColorTextureId());

            if (shader.safeGetUniform("screenSize") != null) {
                shader.safeGetUniform("screenSize").set(
                        (float) mc.getWindow().getWidth(),
                        (float) mc.getWindow().getHeight()
                );
            }
            if (shader.safeGetUniform("time") != null) {
                shader.safeGetUniform("time").set(BlackHoleShaderManager.getShaderTime() * 35);
            }
            if (shader.safeGetUniform("projectionMatrix") != null) {
                shader.safeGetUniform("projectionMatrix").set(projectionMatrix);
            }
            if (shader.safeGetUniform("modelViewMatrix") != null) {
                shader.safeGetUniform("modelViewMatrix").set(modelViewMatrix);
            }
            if (shader.safeGetUniform("cameraPos") != null) {
                shader.safeGetUniform("cameraPos").set(
                        (float) camera.getPosition().x,
                        (float) camera.getPosition().y,
                        (float) camera.getPosition().z
                );
            }
            if (shader.safeGetUniform("entityPos") != null) {
                shader.safeGetUniform("entityPos").set(
                        (float) blackHole.getPosition().x,
                        (float) blackHole.getPosition().y,
                        (float) blackHole.getPosition().z
                );
            }
            if (shader.safeGetUniform("scale") != null) {
                shader.safeGetUniform("scale").set((float) blackHole.getRenderRadius(event.getPartialTick()));
            }
            if (shader.safeGetUniform("eventHorizonScale") != null) {
                shader.safeGetUniform("eventHorizonScale").set((float) blackHole.getEventHorizonScale());
            }
            if (shader.safeGetUniform("accretionDiskOpacity") != null) {
                shader.safeGetUniform("accretionDiskOpacity").set((float) blackHole.getAccretionDiskOpacity());
            }
            if (shader.safeGetUniform("diskInnerExpansion") != null) {
                shader.safeGetUniform("diskInnerExpansion").set((float) blackHole.getDespawnDiskInnerExpansion());
            }
            if (shader.safeGetUniform("diskOuterExpansion") != null) {
                shader.safeGetUniform("diskOuterExpansion").set((float) blackHole.getDespawnDiskOuterExpansion());
            }
            if (shader.safeGetUniform("isDespawning") != null) {
                int isDespawning = (blackHole.getAnimationState() == BlackHole.AnimationState.DESPAWNING) ? 1 : 0;
                shader.safeGetUniform("isDespawning").set(isDespawning);
            }

            BlackHoleShaderManager.drawFullscreenQuad();

            shader.clear();
            viewStack.popPose();
        } finally {
            RenderSystem.restoreProjectionMatrix();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        }
    }

    private static double getFov(Minecraft mc, Camera camera, float partialTicks) {
        GameRenderer gameRenderer = mc.gameRenderer;
        try {
            Method getFovMethod = GameRenderer.class.getDeclaredMethod("getFov", Camera.class, float.class, boolean.class);
            getFovMethod.setAccessible(true);
            return (float) getFovMethod.invoke(gameRenderer, camera, partialTicks, true);
        } catch (Exception e) {
            return mc.options.fov().get();
        }
    }

    private static int createNoiseTexture() {
        return Minecraft.getInstance().getTextureManager().getTexture(
                new ResourceLocation("textures/block/stone.png")
        ).getId();
    }
}
