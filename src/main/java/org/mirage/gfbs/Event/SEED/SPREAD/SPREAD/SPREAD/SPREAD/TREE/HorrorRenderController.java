package org.mirage.gfbs.Event.SEED.SPREAD.SPREAD.SPREAD.SPREAD.TREE;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/lgpl-3.0.html>.
 */

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mirage.gfbs.MirageGFBS;
import org.mirage.gfbs.mixin.PostChainAccessor;
import org.mirage.gfbs.mixin.PostPassAccessor;
import org.mirage.gfbs.tween.*;

import java.io.IOException;
import java.util.List;

@Mod.EventBusSubscriber(modid = MirageGFBS.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HorrorRenderController {

    private static final ResourceLocation HORROR_POST_ID = ResourceLocation.tryBuild(MirageGFBS.MODID, "shaders/post/horror_post.json");
    
    private static final TweenService tweenService = new TweenService();
    
    private static volatile float saturation = 1.0f;
    private static volatile float sharpness = 0.0f;
    private static volatile float vignetteIntensity = 0.0f;
    private static volatile float colorShift = 0.0f;
    
    private static PostChain horrorPostChain;
    private static EffectInstance horrorEffect;
    
    private static volatile boolean effectActive = false;
    private static volatile boolean initialized = false;
    
    private HorrorRenderController() {}
    
    public static void init() {
        if (initialized) return;
        initialized = true;
        
        Minecraft mc = Minecraft.getInstance();
        loadPostChain(mc, mc.getWindow().getWidth(), mc.getWindow().getHeight());
    }
    
    public static void loadPostChain(Minecraft mc, int width, int height) {
        if (horrorPostChain != null) {
            horrorPostChain.resize(width, height);
            return;
        }
        
        try {
            horrorPostChain = new PostChain(
                mc.getTextureManager(),
                mc.getResourceManager(),
                mc.getMainRenderTarget(),
                HORROR_POST_ID
            );
            horrorPostChain.resize(width, height);
            
            PostChainAccessor accessor = (PostChainAccessor) horrorPostChain;
            List<PostPass> passes = accessor.mirage_gfbs$getPasses();
            
            if (!passes.isEmpty()) {
                PostPass firstPass = passes.get(0);
                PostPassAccessor passAccessor = (PostPassAccessor) firstPass;
                horrorEffect = passAccessor.mirage_gfbs$getEffect();
            }
            
        } catch (IOException e) {
            MirageGFBS.LOGGER.error("Failed to load horror post chain", e);
        }
    }
    
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tweenService.tick(1.0 / 20.0);
    }
    
    @SubscribeEvent
    public static void onRender(ScreenEvent.Render event) {
        if (!effectActive) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();

        if (horrorPostChain == null) {
            MirageGFBS.LOGGER.info("HorrorRenderController: Loading post chain...");
            loadPostChain(mc, width, height);
        } else {
            horrorPostChain.resize(width, height);
        }

        if (horrorPostChain == null || horrorEffect == null) {
            MirageGFBS.LOGGER.warn("HorrorRenderController: horrorPostChain or horrorEffect is null after loading!");
            return;
        }

        updateUniforms();
        horrorPostChain.process(event.getPartialTick());
        
        // 每60帧输出一次，确认 process 被调用
        if (System.currentTimeMillis() % 1000 < 20) {
            MirageGFBS.LOGGER.info("HorrorRenderController: horrorPostChain.process() called!");
        }
    }

    private static void updateUniforms() {
        if (horrorEffect == null) {
            MirageGFBS.LOGGER.warn("HorrorRenderController: horrorEffect is null!");
            return;
        }

        EffectInstance effect = horrorEffect;

        if (effect.safeGetUniform("Saturation") != null) {
            effect.safeGetUniform("Saturation").set(saturation);
        } else {
            MirageGFBS.LOGGER.warn("HorrorRenderController: Saturation uniform not found!");
        }
        if (effect.safeGetUniform("Sharpness") != null) {
            effect.safeGetUniform("Sharpness").set(sharpness);
        } else {
            MirageGFBS.LOGGER.warn("HorrorRenderController: Sharpness uniform not found!");
        }
        if (effect.safeGetUniform("VignetteIntensity") != null) {
            effect.safeGetUniform("VignetteIntensity").set(vignetteIntensity);
        }
        if (effect.safeGetUniform("ColorShift") != null) {
            effect.safeGetUniform("ColorShift").set(colorShift);
        }
        
        // 每60帧（约1秒）输出一次当前值，避免刷屏
        if (System.currentTimeMillis() % 1000 < 20) {
            MirageGFBS.LOGGER.info("HorrorRenderController: sat=" + saturation + ", sharp=" + sharpness + ", vig=" + vignetteIntensity + ", shift=" + colorShift);
        }
    }
    
    public static void triggerHorrorEffect(double durationSeconds, HorrorIntensity intensity) {
        triggerHorrorEffect(durationSeconds, intensity, EasingStyle.QUAD, EasingDirection.OUT);
    }
    
    public static void triggerHorrorEffect(double durationSeconds, HorrorIntensity intensity, 
                                           EasingStyle easingStyle, EasingDirection easingDirection) {
        MirageGFBS.LOGGER.info("HorrorRenderController: triggerHorrorEffect called! duration=" + durationSeconds + ", targetSat=" + intensity.targetSaturation);
        effectActive = true;
        
        TweenInfo info = TweenInfo.of(durationSeconds)
            .easing(easingStyle, easingDirection)
            .build();
        
        tweenService.create(
            () -> saturation,
            v -> saturation = (float) v,
            info,
            intensity.targetSaturation
        ).play();
        
        tweenService.create(
            () -> sharpness,
            v -> sharpness = (float) v,
            info,
            intensity.targetSharpness
        ).play();
        
        tweenService.create(
            () -> vignetteIntensity,
            v -> vignetteIntensity = (float) v,
            info,
            intensity.targetVignette
        ).play();
        
        tweenService.create(
            () -> colorShift,
            v -> colorShift = (float) v,
            info,
            intensity.targetColorShift
        ).play();
    }
    
    public static void triggerProgressiveHorror(double totalDurationSeconds) {
        effectActive = true;
        
        double phaseDuration = totalDurationSeconds / 4.0;
        
        TweenInfo phase1 = TweenInfo.of(phaseDuration)
            .easing(EasingStyle.SINE, EasingDirection.IN)
            .build();
        
        TweenInfo phase2 = TweenInfo.of(phaseDuration)
            .easing(EasingStyle.QUAD, EasingDirection.IN_OUT)
            .delay(phaseDuration)
            .build();
        
        TweenInfo phase3 = TweenInfo.of(phaseDuration)
            .easing(EasingStyle.CUBIC, EasingDirection.IN)
            .delay(phaseDuration * 2)
            .build();
        
        TweenInfo phase4 = TweenInfo.of(phaseDuration)
            .easing(EasingStyle.QUART, EasingDirection.IN)
            .delay(phaseDuration * 3)
            .build();
        
        tweenService.create(() -> saturation, v -> saturation = (float) v, phase1, 0.7).play();
        tweenService.create(() -> sharpness, v -> sharpness = (float) v, phase1, 0.5).play();
        
        tweenService.create(() -> saturation, v -> saturation = (float) v, phase2, 0.4).play();
        tweenService.create(() -> sharpness, v -> sharpness = (float) v, phase2, 1.0).play();
        tweenService.create(() -> vignetteIntensity, v -> vignetteIntensity = (float) v, phase2, 0.5).play();
        
        tweenService.create(() -> saturation, v -> saturation = (float) v, phase3, 0.2).play();
        tweenService.create(() -> sharpness, v -> sharpness = (float) v, phase3, 2.0).play();
        tweenService.create(() -> vignetteIntensity, v -> vignetteIntensity = (float) v, phase3, 1.0).play();
        tweenService.create(() -> colorShift, v -> colorShift = (float) v, phase3, 0.5).play();
        
        tweenService.create(() -> saturation, v -> saturation = (float) v, phase4, 0.0).play();
        tweenService.create(() -> sharpness, v -> sharpness = (float) v, phase4, 3.0).play();
        tweenService.create(() -> vignetteIntensity, v -> vignetteIntensity = (float) v, phase4, 1.5).play();
        tweenService.create(() -> colorShift, v -> colorShift = (float) v, phase4, 1.0).play();
    }
    
    public static void resetEffect(double durationSeconds) {
        TweenInfo info = TweenInfo.of(durationSeconds)
            .easing(EasingStyle.QUAD, EasingDirection.OUT)
            .build();
        
        tweenService.create(() -> saturation, v -> saturation = (float) v, info, 1.0).play();
        tweenService.create(() -> sharpness, v -> sharpness = (float) v, info, 0.0).play();
        tweenService.create(() -> vignetteIntensity, v -> vignetteIntensity = (float) v, info, 0.0).play();
        tweenService.create(() -> colorShift, v -> colorShift = (float) v, info, 0.0).play();
        
        TweenInfo disableInfo = TweenInfo.of(0.1)
            .delay(durationSeconds)
            .build();
        
        tweenService.create(() -> effectActive ? 1.0 : 0.0, v -> {}, disableInfo, 0.0)
            .onCompleted(t -> effectActive = false)
            .play();
    }
    
    public static void instantReset() {
        saturation = 1.0f;
        sharpness = 0.0f;
        vignetteIntensity = 0.0f;
        colorShift = 0.0f;
        effectActive = false;
        tweenService.cancelAll();
    }
    
    public static void setSaturation(float value) {
        saturation = Math.max(0.0f, Math.min(1.0f, value));
    }
    
    public static void setSharpness(float value) {
        sharpness = Math.max(0.0f, value);
    }
    
    public static void setVignetteIntensity(float value) {
        vignetteIntensity = Math.max(0.0f, value);
    }
    
    public static void setColorShift(float value) {
        colorShift = Math.max(0.0f, value);
    }
    
    public static float getSaturation() {
        return saturation;
    }
    
    public static float getSharpness() {
        return sharpness;
    }
    
    public static float getVignetteIntensity() {
        return vignetteIntensity;
    }
    
    public static float getColorShift() {
        return colorShift;
    }
    
    public static boolean isEffectActive() {
        return effectActive;
    }
    
    public static void resize(int width, int height) {
        if (horrorPostChain != null) {
            horrorPostChain.resize(width, height);
        }
    }
    
    public static void shutdown() {
        if (horrorPostChain != null) {
            horrorPostChain.close();
            horrorPostChain = null;
        }
        horrorEffect = null;
        tweenService.cancelAll();
        initialized = false;
    }
    
    public static final class HorrorIntensity {
        public static final HorrorIntensity SUBTLE = new HorrorIntensity(0.7f, 0.5f, 0.3f, 0.0f);
        public static final HorrorIntensity MODERATE = new HorrorIntensity(0.4f, 1.0f, 0.6f, 0.3f);
        public static final HorrorIntensity INTENSE = new HorrorIntensity(0.2f, 2.0f, 1.0f, 0.6f);
        public static final HorrorIntensity EXTREME = new HorrorIntensity(0.0f, 3.0f, 1.5f, 1.0f);
        public static final HorrorIntensity NIGHTMARE = new HorrorIntensity(0.0f, 5.0f, 2.0f, 1.5f);
        
        public final float targetSaturation;
        public final float targetSharpness;
        public final float targetVignette;
        public final float targetColorShift;
        
        public HorrorIntensity(float saturation, float sharpness, float vignette, float colorShift) {
            this.targetSaturation = saturation;
            this.targetSharpness = sharpness;
            this.targetVignette = vignette;
            this.targetColorShift = colorShift;
        }
        
        public static HorrorIntensity custom(float saturation, float sharpness, float vignette, float colorShift) {
            return new HorrorIntensity(saturation, sharpness, vignette, colorShift);
        }
    }
}
