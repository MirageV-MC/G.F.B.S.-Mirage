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
import org.mirage.gfbs.MirageGFBS;
import org.mirage.gfbs.advanced.rwl.RotatingWarningLightBlockEntity;
import org.mirage.gfbs.mixin.PostChainAccessor;
import org.mirage.gfbs.mixin.PostPassAccessor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 屏幕空间彩色聚光（后处理）：
 */
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

    // 可调参数（你想做成配置也行）
    private static final float MAX_DIST = 16.0f;
    private static final float HALF_ANGLE_DEG = 18.0f;
    private static final float INTENSITY_BASE = 0.85f;
    private static final float SOFTNESS = 0.12f; // 边缘羽化，0=硬边

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

        Spotlight s = pickNearestSpotlight(mc, partialTick);
        if (s == null) {
            LOGGER.debug("RWLSpotlightPost: no active spotlight found");
        } else {
            LOGGER.debug("RWLSpotlightPost: active spotlight at pos={}, dir={}", s.pos, s.dir);
        }

        EffectInstance effect = getFirstPassEffect(chain);
        if (effect == null) {
            LOGGER.error("RWLSpotlightPost: effect is null");
            return;
        }

        // 计算 invViewProj（世界坐标 -> NDC 的逆）
        Matrix4f proj = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4f mv = new Matrix4f(RenderSystem.getModelViewMatrix());
        Matrix4f invViewProj = proj.mul(mv).invert();

        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();

        // 写入 uniforms
        if (s != null) {
            setUniform(effect, "Enable", 1.0f);
            setUniform(effect, "Color", s.r, s.g, s.b);
        } else {
            setUniform(effect, "Enable", 0.0f);
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

    private static Spotlight pickNearestSpotlight(Minecraft mc, float partialTick) {
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
            return null;
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

        float angleDeg = calcAngleDegClient(be, partialTick);
        RotatingWarningLightBlockEntity.SpotRay ray = RotatingWarningLightBlockEntity.computeSpotRay(be.getBlockPos(), be.getBlockState(), angleDeg, MAX_DIST);

        Spotlight s = new Spotlight();
        s.pos = ray.start;
        s.dir = ray.dir;

        s.r = be.getColorR() / 255.0f;
        s.g = be.getColorG() / 255.0f;
        s.b = be.getColorB() / 255.0f;

        s.maxDist = MAX_DIST;
        s.angleCos = (float) Math.cos(Math.toRadians(HALF_ANGLE_DEG));
        s.intensity = INTENSITY_BASE;
        s.softness = SOFTNESS;

        return s;
    }

    private static float calcAngleDegClient(RotatingWarningLightBlockEntity be, float partialTicks) {
        long msPerRev = Math.max(50L, be.getMsPerRevolution());
        long gt = be.getLevel().getGameTime();
        float gameTime = (gt - be.getStartGameTime()) + partialTicks;
        float elapsedMs = gameTime * 50.0f;
        float t = (elapsedMs % msPerRev) / (float) msPerRev;
        return (be.getStartAngleDeg() + t * 360.0f) % 360.0f;
    }

    private static final class Spotlight {
        Vec3 pos;
        Vec3 dir;
        float r, g, b;
        float angleCos;
        float maxDist;
        float intensity;
        float softness;
    }
}
