package org.mirage.gfbs.advanced.rwl.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.mirage.gfbs.MirageGFBS;
import org.mirage.gfbs.advanced.rwl.RotatingWarningLightBlockEntity;
import org.mirage.gfbs.mixin.PostChainAccessor;
import org.mirage.gfbs.mixin.PostPassAccessor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RWLSpotlightPost {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final RWLSpotlightPost INSTANCE = new RWLSpotlightPost();

    private static final ResourceLocation POST_RL = new ResourceLocation(MirageGFBS.MODID, "shaders/post/rwl_spotlight.json");
    private static final ResourceLocation POST_RL_SIMPLE = new ResourceLocation(MirageGFBS.MODID, "shaders/post/rwl_spotlight_simple.json");
    private static final ResourceLocation POST_RL_TEST = new ResourceLocation(MirageGFBS.MODID, "shaders/post/rwl_spotlight_test.json");

    private PostChain chain;
    private int lastW = -1;
    private int lastH = -1;
    private boolean failed = false;

    private static final float MAX_DIST = 16.0f;
    private static final float HALF_ANGLE_DEG = 50.0f;
    private static final float INTENSITY_BASE = 1.0f;
    private static final float SOFTNESS = 0.0f;

    private RWLSpotlightPost() {}

    public void onAfterLevelRender(float partialTick) {
        if (failed) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        ensureChain(mc);
        if (chain == null) {
            LOGGER.debug("RWLSpotlightPost: chain is null, skipping");
            return;
        }

        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        if (w != lastW || h != lastH) {
            lastW = w;
            lastH = h;
            try {
                chain.resize(w, h);
                LOGGER.debug("RWLSpotlightPost: resized chain to {}x{}", w, h);
            } catch (Throwable t) {
                LOGGER.error("RWLSpotlightPost: failed to resize chain", t);
                failed = true;
                return;
            }
        }

        List<Spotlight> spotlights = pickActiveSpotlights(mc, partialTick);
        if (spotlights.isEmpty()) {
            LOGGER.debug("RWLSpotlightPost: no active spotlights found");
        } else {
            LOGGER.debug("RWLSpotlightPost: found {} active spotlights", spotlights.size());
        }

        EffectInstance effect = getFirstPassEffect(chain);
        if (effect == null) {
            LOGGER.error("RWLSpotlightPost: effect is null");
            return;
        }

        setUniform(effect, "Spot1_Enable", 0.0f);
        setUniform(effect, "Spot2_Enable", 0.0f);

        Matrix4f proj = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4f mv = new Matrix4f(RenderSystem.getModelViewMatrix());
        Matrix4f invViewProj = proj.mul(mv).invert();

        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();

        setUniform(effect, "ModelViewMat", mv);

        int spotlightCount = Math.min(spotlights.size(), 2);

        if (spotlightCount >= 1) {
            Spotlight s1 = spotlights.get(0);
            setUniform(effect, "Spot1_Enable", 1.0f);
            setUniform(effect, "Spot1_Color", s1.r, s1.g, s1.b);
            setUniform(effect, "Spot1_Pos", (float)(s1.pos.x - camPos.x), (float)(s1.pos.y - camPos.y), (float)(s1.pos.z - camPos.z));
            setUniform(effect, "Spot1_Dir", (float) s1.dir.x, (float) s1.dir.y, (float) s1.dir.z);
            setUniform(effect, "Spot1_AngleCos", s1.angleCos);
            setUniform(effect, "Spot1_MaxDist", s1.maxDist);
            setUniform(effect, "Spot1_Intensity", s1.intensity);
            setUniform(effect, "Spot1_Softness", s1.softness);
        } else {
            setUniform(effect, "Spot1_Enable", 0.0f);
        }

        if (spotlightCount >= 2) {
            Spotlight s2 = spotlights.get(1);
            setUniform(effect, "Spot2_Enable", 1.0f);
            setUniform(effect, "Spot2_Color", s2.r, s2.g, s2.b);
            setUniform(effect, "Spot2_Pos", (float)(s2.pos.x - camPos.x), (float)(s2.pos.y - camPos.y), (float)(s2.pos.z - camPos.z));
            setUniform(effect, "Spot2_Dir", (float) s2.dir.x, (float) s2.dir.y, (float) s2.dir.z);
            setUniform(effect, "Spot2_AngleCos", s2.angleCos);
            setUniform(effect, "Spot2_MaxDist", s2.maxDist);
            setUniform(effect, "Spot2_Intensity", s2.intensity);
            setUniform(effect, "Spot2_Softness", s2.softness);
        } else {
            setUniform(effect, "Spot2_Enable", 0.0f);
        }

        try {
            chain.process(partialTick);
        } catch (Throwable t) {
            LOGGER.error("RWLSpotlightPost: failed to process chain", t);
            failed = true;
        }
    }

    private void ensureChain(Minecraft mc) {
        if (chain != null || failed) return;
        try {
            LOGGER.info("RWLSpotlightPost: initializing PostChain with shader: {}", POST_RL_TEST);
            chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), POST_RL_TEST);
            lastW = mc.getWindow().getWidth();
            lastH = mc.getWindow().getHeight();
            chain.resize(lastW, lastH);
            LOGGER.info("RWLSpotlightPost: PostChain initialized successfully");
        } catch (Throwable t) {
            LOGGER.error("RWLSpotlightPost: failed to initialize PostChain", t);
            failed = true;
            chain = null;
        }
    }

    private static EffectInstance getFirstPassEffect(PostChain ch) {
        if (!(ch instanceof PostChainAccessor a)) return null;
        List<PostPass> passes = a.mirage_gfbs$getPasses();
        if (passes == null || passes.isEmpty()) return null;
        PostPass p = passes.get(0);
        if (!(p instanceof PostPassAccessor pa)) return null;
        return pa.mirage_gfbs$getEffect();
    }

    private static void setUniform(EffectInstance sh, String name, float v) {
        var u = sh.getUniform(name);
        if (u != null) {
            u.set(v);
        } else {
            LOGGER.warn("RWLSpotlightPost: uniform '{}' not found", name);
        }
    }

    private static void setUniform(EffectInstance sh, String name, float x, float y) {
        var u = sh.getUniform(name);
        if (u != null) {
            u.set(x, y);
        } else {
            LOGGER.warn("RWLSpotlightPost: uniform '{}' not found", name);
        }
    }

    private static void setUniform(EffectInstance sh, String name, float x, float y, float z) {
        var u = sh.getUniform(name);
        if (u != null) {
            u.set(x, y, z);
        } else {
            LOGGER.warn("RWLSpotlightPost: uniform '{}' not found", name);
        }
    }

    private static void setUniform(EffectInstance sh, String name, Matrix4f m) {
        var u = sh.getUniform(name);
        if (u != null) {
            u.set(m);
        } else {
            LOGGER.warn("RWLSpotlightPost: uniform '{}' not found", name);
        }
    }

    private List<Spotlight> pickActiveSpotlights(Minecraft mc, float partialTick) {
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();

        List<RotatingWarningLightBlockEntity> list = new ArrayList<>();
        for (RotatingWarningLightBlockEntity be : RWLClientSpotlightRegistry.getLoaded()) {
            if (be == null) continue;
            if (be.getLevel() != mc.level) continue;
            if (!be.isPoweredCached()) {
                LOGGER.debug("RWLSpotlightPost: BE at {} is not powered", be.getBlockPos());
                continue;
            }

            double dx = (be.getBlockPos().getX() + 0.5) - cam.x;
            double dy = (be.getBlockPos().getY() + 0.5) - cam.y;
            double dz = (be.getBlockPos().getZ() + 0.5) - cam.z;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > (64.0 * 64.0)) {
                LOGGER.debug("RWLSpotlightPost: BE at {} is too far (dist={})", be.getBlockPos(), Math.sqrt(d2));
                continue;
            }

            LOGGER.debug("RWLSpotlightPost: found active BE at {}", be.getBlockPos());
            list.add(be);
        }

        if (list.isEmpty()) {
            LOGGER.debug("RWLSpotlightPost: no active BEs found in range");
            return new ArrayList<>();
        }

        list.sort(Comparator.comparingDouble(be -> {
            BlockPos p = be.getBlockPos();
            double dx = (p.getX() + 0.5) - cam.x;
            double dy = (p.getY() + 0.5) - cam.y;
            double dz = (p.getZ() + 0.5) - cam.z;
            return dx * dx + dy * dy + dz * dz;
        }));

        RotatingWarningLightBlockEntity be = list.get(0);
        LOGGER.debug("RWLSpotlightPost: selected nearest BE at {}", be.getBlockPos());

        List<Spotlight> spotlights = new ArrayList<>();

        float angleDeg1 = calcAngleDegClient(be, partialTick);
        RotatingWarningLightBlockEntity.SpotRay ray1 = RotatingWarningLightBlockEntity.computeSpotRay(be.getBlockPos(), be.getBlockState(), angleDeg1, MAX_DIST);
        Spotlight s1 = new Spotlight();
        s1.pos = ray1.start;
        s1.dir = ray1.dir;
        s1.r = be.getColorR() / 255.0f;
        s1.g = be.getColorG() / 255.0f;
        s1.b = be.getColorB() / 255.0f;
        s1.maxDist = MAX_DIST;
        s1.angleCos = (float) Math.cos(Math.toRadians(HALF_ANGLE_DEG));
        s1.intensity = INTENSITY_BASE;
        s1.softness = SOFTNESS;
        spotlights.add(s1);

        float angleDeg2 = (angleDeg1 + 180.0f) % 360.0f;
        RotatingWarningLightBlockEntity.SpotRay ray2 = RotatingWarningLightBlockEntity.computeSpotRay(be.getBlockPos(), be.getBlockState(), angleDeg2, MAX_DIST);
        Spotlight s2 = new Spotlight();
        s2.pos = ray2.start;
        s2.dir = ray2.dir;
        s2.r = be.getColorR() / 255.0f;
        s2.g = be.getColorG() / 255.0f;
        s2.b = be.getColorB() / 255.0f;
        s2.maxDist = MAX_DIST;
        s2.angleCos = (float) Math.cos(Math.toRadians(HALF_ANGLE_DEG));
        s2.intensity = INTENSITY_BASE;
        s2.softness = SOFTNESS;
        spotlights.add(s2);

        return spotlights;
    }

    private static float calcAngleDegClient(RotatingWarningLightBlockEntity be, float partialTicks) {
        long msPerRev = Math.max(50L, be.getMsPerRevolution());
        long gt = be.getLevel().getGameTime();
        float gameTime = (gt - be.getStartGameTime()) + partialTicks;
        float elapsedMs = gameTime * 50.0f;
        float t = (elapsedMs % msPerRev) / (float) msPerRev;
        return (be.getStartAngleDeg() + be.getRandomOffset() + t * 360.0f) % 360.0f;
    }

    static final class Spotlight {
        Vec3 pos;
        Vec3 dir;
        float r, g, b;
        float angleCos;
        float maxDist;
        float intensity;
        float softness;
    }
}
