package org.mirage.gfbs.advanced.rwl.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import org.joml.Matrix4f;
import org.mirage.gfbs.advanced.rwl.RotatingWarningLightBlock;
import org.mirage.gfbs.advanced.rwl.RotatingWarningLightBlockEntity;

/**
 * 只渲染灯体/灯头，不渲染任何“光束/糊糊面”。
 * 彩色聚光效果交给屏幕空间后处理（PostChain）去做。
 */
public class RotatingWarningLightBER implements BlockEntityRenderer<RotatingWarningLightBlockEntity> {

    private static final ResourceLocation TEX = new ResourceLocation("minecraft", "block/redstone_lamp");

    public RotatingWarningLightBER(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(RotatingWarningLightBlockEntity be, float partialTicks, PoseStack ps, MultiBufferSource buf, int packedLight, int packedOverlay) {
        if (be.getLevel() == null) return;

        float angleDeg;
        if (be.isPoweredCached()) {
            angleDeg = calcAngleDeg(be, partialTicks);
        } else {
            angleDeg = be.getStartAngleDeg();
        }

        renderLampHead(be, ps, buf, packedLight, packedOverlay, angleDeg, true);
    }

    private float calcAngleDeg(RotatingWarningLightBlockEntity be, float partialTicks) {
        long msPerRev = Math.max(50L, be.getMsPerRevolution());

        float gameTime = (be.getLevel().getGameTime() - be.getStartGameTime()) + partialTicks;
        float elapsedMs = gameTime * 50.0f;
        float t = (elapsedMs % msPerRev) / (float) msPerRev;

        return (be.getStartAngleDeg() + t * 360.0f) % 360.0f;
    }

    private void applyMountTransform(BlockState state, PoseStack ps) {
        AttachFace face = state.getValue(RotatingWarningLightBlock.FACE);
        Direction facing = state.getValue(RotatingWarningLightBlock.FACING);

        ps.translate(0.5, 0.5, 0.5);

        if (face == AttachFace.FLOOR) {
            // do nothing
        } else if (face == AttachFace.CEILING) {
            ps.mulPose(Axis.XP.rotationDegrees(180f));
        } else {
            // wall
            switch (facing) {
                case NORTH -> ps.mulPose(Axis.XP.rotationDegrees(-90f)); // +Y -> -Z
                case SOUTH -> ps.mulPose(Axis.XP.rotationDegrees(90f));  // +Y -> +Z
                case WEST  -> ps.mulPose(Axis.ZP.rotationDegrees(90f));  // +Y -> -X
                case EAST  -> ps.mulPose(Axis.ZP.rotationDegrees(-90f)); // +Y -> +X
            }
        }

        if (face != AttachFace.WALL) {
            float yRot;
            switch (facing) {
                case NORTH -> yRot = 180f;
                case SOUTH -> yRot = 0f;
                case WEST -> yRot = 90f;
                case EAST -> yRot = -90f;
                default -> yRot = 0f;
            }
            ps.mulPose(Axis.YP.rotationDegrees(yRot));
        }

        ps.translate(-0.5, -0.5, -0.5);
    }

    private void renderLampHead(RotatingWarningLightBlockEntity be, PoseStack ps, MultiBufferSource buf,
                                int packedLight, int packedOverlay, float angleDeg, boolean spinning) {

        BlockState state = be.getBlockState();

        ps.pushPose();
        applyMountTransform(state, ps);

        ps.translate(0.5, 0.30, 0.5);

        if (spinning) ps.mulPose(Axis.YP.rotationDegrees(angleDeg));

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(TEX);

        VertexConsumer vc = buf.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));

        float sx = 0.10f;
        float sy = 0.10f;
        float sz = 0.18f;

        ps.translate(0.0, 0.10, 0.0);

        Matrix4f m = ps.last().pose();
        drawBox(vc, m, sprite, -sx, 0.0f, -sz, sx, sy, sz, packedLight, packedOverlay);

        float rf = be.getColorR() / 255.0f;
        float gf = be.getColorG() / 255.0f;
        float bf = be.getColorB() / 255.0f;

        drawQuadColored(vc, m, sprite,
                -sx * 0.9f, 0.02f, sz,
                sx * 0.9f, sy * 0.95f, sz,
                rf, gf, bf, 0.9f,
                packedLight, packedOverlay);

        ps.popPose();
    }

    private static void drawBox(VertexConsumer vc, Matrix4f m, TextureAtlasSprite s,
                                float x0, float y0, float z0, float x1, float y1, float z1,
                                int light, int overlay) {
        // 前(z1)
        drawQuad(vc, m, s, x0, y0, z1, x1, y1, z1, light, overlay);
        // 后(z0)
        drawQuad(vc, m, s, x1, y0, z0, x0, y1, z0, light, overlay);
        // 左(x0)
        drawQuad(vc, m, s, x0, y0, z0, x0, y1, z1, light, overlay);
        // 右(x1)
        drawQuad(vc, m, s, x1, y0, z1, x1, y1, z0, light, overlay);
        // 上(y1)
        drawQuad(vc, m, s, x0, y1, z1, x1, y1, z0, light, overlay);
        // 下(y0)
        drawQuad(vc, m, s, x0, y0, z0, x1, y0, z1, light, overlay);
    }

    private static void drawQuad(VertexConsumer vc, Matrix4f m, TextureAtlasSprite s,
                                 float x0, float y0, float z0,
                                 float x1, float y1, float z1,
                                 int light, int overlay) {
        float u0 = s.getU0(), u1 = s.getU1();
        float v0 = s.getV0(), v1 = s.getV1();

        vc.vertex(m, x0, y0, z0)
                .color(1f,1f,1f,1f)
                .uv(u0, v1)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(0f, 0f, 1f)
                .endVertex();

        vc.vertex(m, x1, y0, z0)
                .color(1f,1f,1f,1f)
                .uv(u1, v1)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(0f, 0f, 1f)
                .endVertex();

        vc.vertex(m, x1, y1, z1)
                .color(1f,1f,1f,1f)
                .uv(u1, v0)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(0f, 0f, 1f)
                .endVertex();

        vc.vertex(m, x0, y1, z1)
                .color(1f,1f,1f,1f)
                .uv(u0, v0)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(0f, 0f, 1f)
                .endVertex();
    }

    private static void drawQuadColored(VertexConsumer vc, Matrix4f m, TextureAtlasSprite s,
                                        float x0, float y0, float z0,
                                        float x1, float y1, float z1,
                                        float r, float g, float b, float a,
                                        int light, int overlay) {

        float u0 = s.getU0(), u1 = s.getU1();
        float v0 = s.getV0(), v1 = s.getV1();

        vc.vertex(m, x0, y0, z0)
                .color(r, g, b, a)
                .uv(u0, v1)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(0f, 0f, 1f)
                .endVertex();

        vc.vertex(m, x1, y0, z0)
                .color(r, g, b, a)
                .uv(u1, v1)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(0f, 0f, 1f)
                .endVertex();

        vc.vertex(m, x1, y1, z1)
                .color(r, g, b, a)
                .uv(u1, v0)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(0f, 0f, 1f)
                .endVertex();

        vc.vertex(m, x0, y1, z1)
                .color(r, g, b, a)
                .uv(u0, v0)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(0f, 0f, 1f)
                .endVertex();
    }
}
