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

package org.mirage.Phenomenon.FogApi;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.mirage.Phenomenon.network.Network.ClientEventHandler;

public class CustomFogModule {

    // 目标雾效果设置
    private float targetFogRed = 0.5f;
    private float targetFogGreen = 0.5f;
    private float targetFogBlue = 0.5f;
    private float targetFogStart = 0.0f;
    private float targetFogEnd = 1.0f;
    private boolean targetActive = false;

    // 当前显示的雾效果设置（用于渐变）
    private float currentFogRed = 0.5f;
    private float currentFogGreen = 0.5f;
    private float currentFogBlue = 0.5f;
    private float currentFogStart = 0.0f;
    private float currentFogEnd = 1.0f;
    private boolean currentActive = false;

    // 渐变参数
    private static final float TRANSITION_SPEED = 0.05f; // 渐变速度（每帧变化量）
    private boolean isTransitioning = false;
    private long transitionStartTime = 0;
    private static final long TRANSITION_DURATION = 1000; // 渐变持续时间（毫秒）

    public CustomFogModule() {
        ClientEventHandler.registerEvent("fog_settings", this::handleFogSettingsUpdate);
        // 注册客户端tick事件用于处理渐变
        MinecraftForge.EVENT_BUS.register(new TransitionHandler());
    }

    /**
     * 处理从服务端接收的雾效果设置更新，启动渐变过程
     */
    private void handleFogSettingsUpdate(CompoundTag data) {
        // 设置目标值
        this.setTargetFogColor(
                data.getFloat("red"),
                data.getFloat("green"),
                data.getFloat("blue")
        );
        this.setTargetFogRange(data.getFloat("start"), data.getFloat("end"));

        this.targetActive = data.getBoolean("active");

        // 启动渐变过程
        startTransition();
    }

    /**
     * 启动雾效果渐变过程
     */
    private void startTransition() {
        isTransitioning = true;
        transitionStartTime = System.currentTimeMillis();

        // 如果是从关闭到开启，先注册事件监听器
        if (targetActive && !currentActive) {
            MinecraftForge.EVENT_BUS.register(this);
            currentActive = true;
        }
    }

    /**
     * 设置目标雾颜色
     */
    public void setTargetFogColor(float r, float g, float b) {
        targetFogRed = clamp(r);
        targetFogGreen = clamp(g);
        targetFogBlue = clamp(b);
    }

    /**
     * 设置目标雾范围
     */
    public void setTargetFogRange(float start, float end) {
        targetFogStart = Math.max(0, start);
        targetFogEnd = Math.max(targetFogStart + 0.1f, end);
    }

    /**
     * 线性插值函数
     */
    private float lerp(float start, float end, float progress) {
        return start + (end - start) * progress;
    }

    /**
     * 计算当前渐变进度（0.0到1.0）
     */
    private float calculateTransitionProgress() {
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - transitionStartTime;
        return Math.min(1.0f, (float) elapsed / TRANSITION_DURATION);
    }

    /**
     * 更新当前显示的雾效果设置（渐变过程）
     */
    private void updateCurrentFogSettings() {
        if (!isTransitioning) return;

        float progress = calculateTransitionProgress();

        // 使用缓动函数使渐变更自然
        float easeProgress = easeInOutCubic(progress);

        // 线性插值当前值
        currentFogRed = lerp(currentFogRed, targetFogRed, easeProgress);
        currentFogGreen = lerp(currentFogGreen, targetFogGreen, easeProgress);
        currentFogBlue = lerp(currentFogBlue, targetFogBlue, easeProgress);
        currentFogStart = lerp(currentFogStart, targetFogStart, easeProgress);
        currentFogEnd = lerp(currentFogEnd, targetFogEnd, easeProgress);

        // 渐变完成
        if (progress >= 1.0f) {
            isTransitioning = false;

            // 如果是从开启到关闭，注销事件监听器
            if (!targetActive && currentActive) {
                MinecraftForge.EVENT_BUS.unregister(this);
                currentActive = false;
            }
        }
    }

    /**
     * 缓动函数：三次缓动
     */
    private float easeInOutCubic(float x) {
        return x < 0.5 ? 4 * x * x * x : 1 - (float)Math.pow(-2 * x + 2, 3) / 2;
    }

    private float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    @SubscribeEvent
    public void onFogColors(ViewportEvent.ComputeFogColor event) {
        if (!currentActive) return;

        event.setRed(currentFogRed);
        event.setGreen(currentFogGreen);
        event.setBlue(currentFogBlue);
    }

    @SubscribeEvent
    public void onFogRender(ViewportEvent.RenderFog event) {
        if (!currentActive) return;

        Entity entity = Minecraft.getInstance().getCameraEntity();
        if (entity == null) return;

        FogType fogType = Minecraft.getInstance().gameRenderer.getMainCamera().getFluidInCamera();
        if (fogType == FogType.NONE) {
            event.setNearPlaneDistance(currentFogStart);
            event.setFarPlaneDistance(currentFogEnd);
            event.setCanceled(true);
        }
    }

    /**
     * 内部类用于处理渐变更新
     */
    private class TransitionHandler {
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                updateCurrentFogSettings();
            }
        }
    }

    public boolean isActive() {
        return currentActive;
    }

    public boolean isTransitioning() {
        return isTransitioning;
    }

    // 获取当前显示值的方法
    public float getCurrentFogRed() { return currentFogRed; }
    public float getCurrentFogGreen() { return currentFogGreen; }
    public float getCurrentFogBlue() { return currentFogBlue; }
    public float getCurrentFogStart() { return currentFogStart; }
    public float getCurrentFogEnd() { return currentFogEnd; }

    // 获取目标值的方法
    public float getTargetFogRed() { return targetFogRed; }
    public float getTargetFogGreen() { return targetFogGreen; }
    public float getTargetFogBlue() { return targetFogBlue; }
    public float getTargetFogStart() { return targetFogStart; }
    public float getTargetFogEnd() { return targetFogEnd; }
}