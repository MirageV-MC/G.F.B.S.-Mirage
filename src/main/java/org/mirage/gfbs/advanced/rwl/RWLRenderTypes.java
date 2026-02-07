package org.mirage.gfbs.advanced.rwl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;

import java.util.function.Supplier;

public final class RWLRenderTypes {
    private RWLRenderTypes() {}

    public static final VertexFormat VOLUME_FORMAT = DefaultVertexFormat.POSITION_COLOR_TEX;

    private static Supplier<ShaderInstance> VOLUME_SHADER = () -> null;

    private static final RenderStateShard.TransparencyStateShard ADDITIVE_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard(
                    "rwl_additive",
                    () -> {
                        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                        com.mojang.blaze3d.systems.RenderSystem.blendFunc(
                                GlStateManager.SourceFactor.SRC_ALPHA,
                                GlStateManager.DestFactor.ONE
                        );
                    },
                    () -> {
                        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
                        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
                    }
            );

    private static final RenderStateShard.DepthTestStateShard LEQUAL_DEPTH_TEST =
            new RenderStateShard.DepthTestStateShard("rwl_lequal", 515); // GL_LEQUAL

    private static final RenderStateShard.WriteMaskStateShard COLOR_ONLY_WRITE =
            new RenderStateShard.WriteMaskStateShard(true, false);

    private static final RenderStateShard.CullStateShard NO_CULL =
            new RenderStateShard.CullStateShard(false);

    private static final RenderStateShard.OutputStateShard TRANSLUCENT_TARGET =
            new RenderStateShard.OutputStateShard(
                    "rwl_translucent_target",
                    () -> com.mojang.blaze3d.systems.RenderSystem.enableBlend(),
                    () -> {}
            );

    private static final RenderType VOLUME_LIGHT = RenderType.create(
            "rwl_volume_light",
            VOLUME_FORMAT,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> VOLUME_SHADER.get()))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_ONLY_WRITE)
                    .setCullState(NO_CULL)
                    .setOutputState(TRANSLUCENT_TARGET)
                    .createCompositeState(false)
    );

    private static final RenderType SPOTLIGHT_HIGHLIGHT = RenderType.create(
            "rwl_spotlight_highlight",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_ONLY_WRITE)
                    .setCullState(NO_CULL)
                    .setOutputState(TRANSLUCENT_TARGET)
                    .createCompositeState(false)
    );

    public static void bindVolumeShader(Supplier<ShaderInstance> shaderSupplier) {
        VOLUME_SHADER = shaderSupplier;
    }

    public static RenderType volumeLight() {
        return VOLUME_LIGHT;
    }

    public static RenderType spotlightHighlight() {
        return SPOTLIGHT_HIGHLIGHT;
    }
}
