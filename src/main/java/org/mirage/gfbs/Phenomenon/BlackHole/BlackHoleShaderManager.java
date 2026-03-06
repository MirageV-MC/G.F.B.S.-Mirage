package org.mirage.gfbs.Phenomenon.BlackHole;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.mirage.gfbs.MirageGFBS;

import java.io.IOException;
import java.util.List;

@Mod.EventBusSubscriber(modid = MirageGFBS.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BlackHoleShaderManager {
    private static ShaderInstance blackHoleShader;
    private static long startTime = System.currentTimeMillis();

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(
                event.getResourceProvider(),
                new ResourceLocation(MirageGFBS.MODID, "black_hole_pro"),
                DefaultVertexFormat.POSITION
        ), shader -> {
            blackHoleShader = shader;
        });
    }

    public static ShaderInstance getBlackHoleShader() {
        return blackHoleShader;
    }

    public static float getShaderTime() {
        return (System.currentTimeMillis() - startTime) / 1000.0f;
    }

    public static void drawFullscreenQuad() {
        RenderSystem.backupProjectionMatrix();
        Matrix4f orthoMatrix = new Matrix4f().setOrtho(-1, 1, -1, 1, -1, 1);
        RenderSystem.setProjectionMatrix(orthoMatrix, VertexSorting.DISTANCE_TO_ORIGIN);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        bufferbuilder.vertex(-1.0, -1.0, 0.0).uv(0, 0).endVertex();
        bufferbuilder.vertex(1.0, -1.0, 0.0).uv(1, 0).endVertex();
        bufferbuilder.vertex(1.0, 1.0, 0.0).uv(1, 1).endVertex();
        bufferbuilder.vertex(-1.0, 1.0, 0.0).uv(0, 1).endVertex();
        BufferUploader.drawWithShader(bufferbuilder.end());

        RenderSystem.restoreProjectionMatrix();
    }
}
