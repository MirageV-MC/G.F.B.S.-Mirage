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

package org.mirage.gfbs.Tools;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mirage.gfbs.MirageGFBS;

import java.util.Random;

/**
 * F.A.A.S.临时关机代码破解器
 */
@Mod.EventBusSubscriber(modid = MirageGFBS.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HexCrackerUI {

    private static final HexCrackerUI INSTANCE = new HexCrackerUI();

    public static final IGuiOverlay OVERLAY = (ForgeGui gui, GuiGraphics graphics,
                                               float partialTick, int screenWidth, int screenHeight) ->
            INSTANCE.render(graphics, partialTick, screenWidth, screenHeight);
    public static void start() {
        INSTANCE.startInternal();
    }

    public static void stop() {
        INSTANCE.stopInternal();
    }

    public static void updateCrackedDigits(int[] crackedDigits) {
        INSTANCE.updateCrackedDigitsInternal(crackedDigits);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        if (mc.isPaused()) return;
        INSTANCE.tick();
    }


    @Mod.EventBusSubscriber(modid = MirageGFBS.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "hex_cracker", HexCrackerUI.OVERLAY);
        }
    }


    private enum State {
        HIDDEN,
        OPENING,
        RUNNING,
        STOPPING,
        CLOSING
    }

    private Thread producerThread;
    private volatile boolean producingThreadRunning = false;

    private State state = State.HIDDEN;

    private static final int ANIM_TICKS = 20;
    private int animationTicks = 0;

    private static final int STOP_OVERLAY_TICKS = 20 * 5;
    private int stopTicks = 0;

    private final Random random = new Random();
    private long globalTicks = 0;
    private boolean producing = false;

    private int currentIntervalTicks = 100;
    private int ticksUntilNextNumber = 100;
    private int generatedCount = 0;
    // 版本号 vA.B.C
    private int vA = 1, vB = 0, vC = 0;
    private String versionString = "v1.0.0";

    private String currentTopNumber = "------";
    private final HistoryEntry[] history = new HistoryEntry[6];
    private int[] crackedDigits = new int[]{-1, -1, -1, -1, -1, -1};

    private static class HistoryEntry {
        final String number;
        final int[] crackedAtTime;

        HistoryEntry(String number, int[] crackedAtTime) {
            this.number = number;
            this.crackedAtTime = crackedAtTime != null ? crackedAtTime.clone() : new int[]{-1, -1, -1, -1, -1, -1};
        }
    }

    private HexCrackerUI() {
        resetHistory();
    }

    private void resetHistory() {
        for (int i = 0; i < history.length; i++) {
            history[i] = new HistoryEntry("------", null);
        }
    }

    private void updateCrackedDigitsInternal(int[] digits) {
        if (digits != null && digits.length == 6) {
            this.crackedDigits = digits.clone();
        }
    }


    private void startInternal() {
        producing = true;

        state = State.OPENING;
        animationTicks = 0;
        stopTicks = 0;
        producing = true;

        currentIntervalTicks = 100;
        ticksUntilNextNumber = 20;
        generatedCount = 0;

        vA = 1; vB = 0; vC = 0;
        versionString = "v1.0.0";

        currentTopNumber = "------";
        crackedDigits = new int[]{-1, -1, -1, -1, -1, -1};
        resetHistory();

        startProductionThread();
    }

    private void startProductionThread() {
        producingThreadRunning = false;
        if (producerThread != null && producerThread.isAlive()) {
            producerThread.interrupt();
        }

        producingThreadRunning = true;

        producerThread = new Thread(() -> {
            while (producingThreadRunning) {

                generateNextNumber();
                generatedCount++;

                if (generatedCount % 3 == 0) {
                    increaseSpeedAndVersion();
                }

                try {
                    long delayMs = (long) (currentIntervalTicks * (1000.0 / 20.0));
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        producerThread.setDaemon(true);
        producerThread.start();
    }


    private void stopInternal() {
        if (state == State.HIDDEN || state == State.CLOSING) {
            return;
        }
        producing = false;
        animationTicks = ANIM_TICKS;
        state = State.STOPPING;
        stopTicks = 0;
        producingThreadRunning = false;

    }


    private void tick() {
        globalTicks++;

        switch (state) {
            case HIDDEN -> {
            }
            case OPENING -> {
                if (animationTicks < ANIM_TICKS) {
                    animationTicks++;
                }
                if (animationTicks >= ANIM_TICKS) {
                    animationTicks = ANIM_TICKS;
                    state = State.RUNNING;
                }
            }
            case RUNNING -> {
                animationTicks = ANIM_TICKS;
            }
            case STOPPING -> {
                stopTicks++;
                if (stopTicks >= STOP_OVERLAY_TICKS) {
                    state = State.CLOSING;
                }
            }
            case CLOSING -> {
                if (animationTicks > 0) {
                    animationTicks--;
                }
                if (animationTicks <= 0) {
                    animationTicks = 0;
                    state = State.HIDDEN;
                    producing = false;
                }
            }
        }
    }

    private void generateNextNumber() {
        StringBuilder num = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (crackedDigits[i] >= 0) {
                num.append(crackedDigits[i]);
            } else {
                num.append(random.nextInt(10));
            }
        }
        currentTopNumber = num.toString();

        for (int i = history.length - 1; i > 0; i--) {
            history[i] = history[i - 1];
        }
        history[0] = new HistoryEntry(currentTopNumber, crackedDigits);
    }


    private static final int TICKS_PER_SECOND = 40;
    private static final int MIN_INTERVAL_TICKS = TICKS_PER_SECOND / 25;
    private static final float EXP_DECAY = 0.82f;

    private void increaseSpeedAndVersion() {
        vC++;
        if (vC > 9) {
            vC = 0;
            vB++;
            if (vB > 9) {
                vB = 0;
                vA++;
            }
        }

        // 封顶版本：v8.7.4
        if (vA > 8 || (vA == 8 && (vB > 7 || (vB == 7 && vC > 4)))) {
            vA = 8;
            vB = 7;
            vC = 4;
        }
        versionString = "v" + vA + "." + vB + "." + vC;

        int newInterval = Math.round(currentIntervalTicks * EXP_DECAY);
        if (newInterval < MIN_INTERVAL_TICKS) {
            newInterval = MIN_INTERVAL_TICKS;
        }
        if (newInterval < currentIntervalTicks) {
            currentIntervalTicks = newInterval;
        }
    }



    private void render(GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (state == State.HIDDEN) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        RenderSystem.disableDepthTest();

        float scaleW = screenWidth / 2560f;
        float scaleH = screenHeight / 1440f;
        float scale = Math.min(scaleW, scaleH) * 2;

        int panelWidth       = (int) (145 * scale);
        int rowHeight        = (int) (32  * scale);
        int topDisplayHeight = (int) (50  * scale);
        int bottomHeight     = (int) (28  * scale);

        int titleHeight      = (int) (20 * scale);

        int listRows = history.length;
        int panelHeight = titleHeight + (int)(4 * scale)
                + topDisplayHeight + (int)(4 * scale)
                + listRows * rowHeight
                + (int)(6 * scale)
                + bottomHeight;

        int centerY = screenHeight / 2;
        int baseY = centerY - panelHeight / 2;

        float rawProgress = animationTicks / (float) ANIM_TICKS;
        float openProgress = easeInOutCubic(rawProgress);

        int offscreenX = screenWidth + (int)(10 * scale);
        int visibleX = screenWidth - panelWidth - (int)(10 * scale);
        int panelX = (int) (offscreenX + (visibleX - offscreenX) * openProgress);

        // 面板背景
        int bgColor = 0xCC000000;
        graphics.fill(panelX, baseY, panelX + panelWidth, baseY + panelHeight, bgColor);

        // 标题栏
        String title = "F.A.A.S.临时停机终端";
        graphics.pose().pushPose();
        float titleScale = scale * 1.5f;
        graphics.pose().scale(titleScale, titleScale, 1);
        int titleWidth = font.width(title);
        int titleX = (int)((panelX + (panelWidth - titleWidth * titleScale) / 2) / titleScale);
        int titleY = (int)((baseY + (titleHeight - font.lineHeight * titleScale) / 2) / titleScale);
        graphics.drawString(font, title, titleX, titleY, 0xFFFCE9FF, false);
        graphics.pose().popPose();

        if (state == State.STOPPING) {
            graphics.fill(panelX + 1, baseY + 1, panelX + panelWidth - 1, baseY + panelHeight - 1, 0xEE000000);

            String msg = "系统无数据 (0x8FC6A5)";
            int msgWidth = (int)(font.width(msg) * scale);
            int msgX = panelX + (panelWidth - msgWidth) / 2;
            int msgY = baseY + (panelHeight - (int)(font.lineHeight * scale)) / 2;

            boolean visible = ((globalTicks / 10) % 2) == 0;
            if (visible) {
                graphics.pose().pushPose();
                graphics.pose().scale(scale, scale, 1); // 字体按比例缩放
                graphics.drawString(font, msg,
                        (int)(msgX / scale),
                        (int)(msgY / scale),
                        0xFFFF5555, false);
                graphics.pose().popPose();
            }

            RenderSystem.enableDepthTest();
            return;
        }


        int topX = panelX + (int)(8 * scale);
        int topY = baseY + titleHeight + (int)(4 * scale);
        int topW = panelWidth - (int)(16 * scale);
        int topH = topDisplayHeight - (int)(4 * scale);

        graphics.fill(topX, topY, topX + topW, topY + topH, 0xFF202020);
        graphics.fill(topX + 1, topY + 1, topX + topW - 1, topY + topH - 1, 0xFF101010);

        graphics.pose().pushPose();
        float text_scale0 = scale * 1.5f;
        graphics.pose().scale(text_scale0, text_scale0, 1);

        int charWidth = font.width("0");
        int totalWidth = charWidth * 6;
        int startX = (int)((topX + (topW - totalWidth * text_scale0) / 2) / text_scale0);
        int drawY = (int)((topY + (topH - font.lineHeight * text_scale0) / 2) / text_scale0);

        for (int i = 0; i < 6; i++) {
            char c = currentTopNumber.charAt(i);
            int color = (crackedDigits[i] >= 0) ? 0xFF66BB66 : 0xFFFCE9FF;
            graphics.drawString(font, String.valueOf(c), startX + i * charWidth, drawY, color, false);
        }
        graphics.pose().popPose();


        int listStartY = topY + topH + (int)(4 * scale);

        for (int i = 0; i < history.length; i++) {
            int rowY = listStartY + i * rowHeight;
            int rowW = panelWidth - (int)(20 * scale);
            int rowH = rowHeight - (int)(2 * scale);
            int rowX = panelX + (int)(10 * scale);

            graphics.fill(rowX, rowY, rowX + rowW, rowY + rowH, 0xFF181818);

            HistoryEntry entry = history[i];
            String value = entry.number;
            int[] entryCracked = entry.crackedAtTime;

            graphics.pose().pushPose();
            float text_scale = scale * 1.5f;
            graphics.pose().scale(text_scale, text_scale, 1);

            int histCharWidth = font.width("0");
            int histTotalWidth = histCharWidth * 6;
            int hx = (int)((rowX + (rowW - histTotalWidth * text_scale) / 2) / text_scale);
            int hy = (int)((rowY + (rowH - font.lineHeight * text_scale) / 2) / text_scale);

            for (int j = 0; j < 6 && j < value.length(); j++) {
                char c = value.charAt(j);
                int color = (entryCracked[j] >= 0) ? 0xFF66BB66 : 0xFFBBBBBB;
                graphics.drawString(font, String.valueOf(c), hx + j * histCharWidth, hy, color, false);
            }
            graphics.pose().popPose();
        }

        graphics.pose().pushPose();
        float text_scale2 = scale * 1.5f;
        graphics.pose().scale(text_scale2, text_scale2, 1);

        int vWidth = font.width(versionString);
        int vx = (int)((panelX + (panelWidth - vWidth * text_scale2) / 2) / text_scale2);
        int vy = (int)((baseY + panelHeight - bottomHeight + 2 * text_scale2) / text_scale2);

        graphics.drawString(font, versionString, vx, vy, 0xFFBBBBBB, false);

        graphics.pose().popPose();
        RenderSystem.enableDepthTest();
    }

    private static float easeInOutCubic(float t) {
        if (t < 0.0f) t = 0.0f;
        if (t > 1.0f) t = 1.0f;
        if (t < 0.5f) {
            return 4.0f * t * t * t;
        } else {
            float f = -2.0f * t + 2.0f;
            return 1.0f - (f * f * f) / 2.0f;
        }
    }
}
