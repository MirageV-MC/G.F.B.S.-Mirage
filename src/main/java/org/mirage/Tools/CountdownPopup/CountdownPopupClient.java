package org.mirage.Tools.CountdownPopup;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public final class CountdownPopupClient {

    private static final CountdownPopupClient INSTANCE = new CountdownPopupClient();
    public static CountdownPopupClient get() { return INSTANCE; }


    /** 弹窗宽度占屏幕宽度的比例（0~1）*/
    private static final float BOX_WIDTH_SCREEN_RATIO = 0.2f;

    /**
     * 弹窗长宽比（宽:高）
     */
    private static final float ASPECT_W = 16f;
    private static final float ASPECT_H = 4f;

    /** 倒计时文字相对“自动适配”后的缩放系数（建议 0.6~1.0）。 */
    private static final float COUNTDOWN_SCALE = 0.95f;

    /** “[已过期]”文字相对“自动适配”后的缩放系数（建议 0.6~1.0）。 */
    private static final float EXPIRED_SCALE = 0.95f;

    /** 标题文字相对“自动适配”后的缩放系数（建议 0.6~1.0）。 */
    private static final float TITLE_SCALE = 0.90f;

    /** 文字缩放下限/上限，防止极端情况下过小/过大。 */
    private static final float MIN_TEXT_SCALE = 0.50f;
    private static final float MAX_TEXT_SCALE = 4.00f;

    private enum State {
        HIDDEN,
        ENTERING,
        SHOWN_IDLE,
        COUNTING,
        EXPIRED_FLASH,
        EXITING
    }

    private State state = State.HIDDEN;

    private ScheduledExecutorService countdownExecutor;
    private ScheduledFuture<?> countdownTask;

    private volatile long remainingMillis;
    private volatile long displayMillis;

    private String title = "";
    private long totalMillis = 0L;

    // animation
    private long animStartNs = 0L;
    private long animDurationNs = 0L;
    private float animFrom = 0f; // 0=隐藏位置, 1=显示位置
    private float animTo = 0f;
    private float animValue = 0f;

    private long displayAccumNs = 0L;

    // counting timing (server-authoritative sync)
    private long serverEndGameTime = -1L; // ServerLevel#getGameTime() end tick
    private long lastClientGameTime = 0L;
    private long lastClientGameTimeNano = 0L;

    // expired flashing
    private int flashesDone = 0;         // 0..4
    private boolean flashVisible = true;
    private long nextFlashNs = 0L;

    private CountdownPopupClient() {}

    private void ensureCountdownExecutor() {
        if (countdownExecutor != null) return;

        ThreadFactory tf = r -> {
            Thread t = new Thread(r, "gfbs-countdown-popup-timer");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        };

        countdownExecutor = Executors.newSingleThreadScheduledExecutor(tf);
    }

    private void cancelCountdownTask() {
        if (countdownTask != null) {
            countdownTask.cancel(false);
            countdownTask = null;
        }
    }


    public void popup(String title, int minutes, int seconds, int millisOrCs) {
        if (title == null) title = "";
        this.title = title;

        int m = Math.max(0, minutes);
        int s = Mth.clamp(seconds, 0, 59);
        int cs = Mth.clamp(millisOrCs, 0, 99); // 两位：00-99

        this.totalMillis = (m * 60L + s) * 1000L + (cs * 10L);
        this.remainingMillis = this.totalMillis;

        this.displayMillis = this.totalMillis;
        this.displayAccumNs = 0L;

        this.serverEndGameTime = -1L;

        cancelCountdownTask();

        startAnim(State.ENTERING, 0f, 1f, 600_000_000L);

    }

    public void startCountdown(long endGameTime) {
        if (state == State.HIDDEN) return;
        if (state == State.EXPIRED_FLASH) return;

        this.serverEndGameTime = endGameTime;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            this.lastClientGameTime = mc.level.getGameTime();
            this.lastClientGameTimeNano = System.nanoTime();
        } else {
            this.lastClientGameTime = 0L;
            this.lastClientGameTimeNano = System.nanoTime();
        }

        this.remainingMillis = this.totalMillis;
        this.displayMillis = this.totalMillis;
        this.displayAccumNs = 0L;

        state = State.COUNTING;

        ensureCountdownExecutor();
        cancelCountdownTask();

        countdownTask = countdownExecutor.scheduleAtFixedRate(() -> {
            long n = System.nanoTime();

            double estGameTime = (double) lastClientGameTime;
            long dt = n - lastClientGameTimeNano;
            if (dt > 0L) {
                estGameTime += (double) dt / 50_000_000.0; // 50ms = 1 tick
            }

            double ticksLeft = (double) serverEndGameTime - estGameTime;
            long remainMs = (long) Math.ceil(ticksLeft * 50.0);

            if (remainMs <= 0L) {
                remainingMillis = 0L;
                displayMillis = 0L;

                cancelCountdownTask();
                beginExpiredFlow();
                return;
            }

            remainingMillis = remainMs;
            displayMillis = remainMs;
        }, 0L, 10L, TimeUnit.MILLISECONDS);
    }

    public void onServerEnded() {
        if (state == State.COUNTING || state == State.SHOWN_IDLE) {
            beginExpiredFlow();
        }
    }


    public void stop() {
        if (state == State.HIDDEN) return;
        cancelCountdownTask();
        startAnim(State.EXITING, animValue, 0f, 600_000_000L);
    }

    public void tick() {
        long now = System.nanoTime();
        updateAnim(now);

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            this.lastClientGameTime = mc.level.getGameTime();
            this.lastClientGameTimeNano = now;
        }

        if (state == State.ENTERING && animFinished(now)) {
            state = State.SHOWN_IDLE;
        }

        if (state == State.EXITING && animFinished(now)) {
            state = State.HIDDEN;
        }

        if (state == State.EXPIRED_FLASH) {
            if (now >= nextFlashNs) {
                flashVisible = !flashVisible;
                nextFlashNs = now + 700_000_000L;

                flashesDone++;
                if (flashesDone >= 8) {
                    startAnim(State.EXITING, animValue, 0f, 600_000_000L);
                }
            }
        }
    }

    public void render(GuiGraphics gg, float partialTick, int screenW, int screenH) {
        if (state == State.HIDDEN) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        int boxW = (int) (screenW * BOX_WIDTH_SCREEN_RATIO);

        int boxH = (int) (boxW * (ASPECT_H / ASPECT_W));

        int x = (screenW - boxW) / 2;

        int shownY = 10;
        int hiddenY = -boxH - 10;

        float eased = easeInOut(animValue);
        int y = (int) Mth.lerp(eased, hiddenY, shownY);

        boolean drawLowerLine = (state != State.EXPIRED_FLASH) || flashVisible;
        int bg = 0xAA000000;
        int border = 0xCCFFFFFF;
        drawRect(gg, x, y, boxW, boxH, bg);
        drawBorder(gg, x, y, boxW, boxH, border);

        String upper = title;
        String lower;

        if (state == State.EXPIRED_FLASH) {
            lower = "[已过期]";
        } else {
            lower = formatMmSsCs(displayMillis);
        }

        int padding = 8;

        int contentW = Math.max(1, boxW - padding * 2);

        int upperTop = y + padding;
        int upperBottom = y + (boxH / 2);
        int upperAreaH = Math.max(1, upperBottom - upperTop);

        int lowerTop = y + (boxH / 2);
        int lowerBottom = y + boxH - padding;
        int lowerAreaH = Math.max(1, lowerBottom - lowerTop);

        // 标题缩放
        float upperScale = fitScale(font, upper, contentW, upperAreaH, TITLE_SCALE);
        int upperScaledW = (int) (font.width(upper) * upperScale);
        int upperScaledH = (int) (font.lineHeight * upperScale);
        int upperX = x + (boxW - upperScaledW) / 2;
        int upperY = upperTop + (upperAreaH - upperScaledH) / 2;

        gg.pose().pushPose();
        gg.pose().translate(upperX, upperY, 0);
        gg.pose().scale(upperScale, upperScale, 1.0f);
        gg.drawString(font, upper, 0, 0, 0xFFFFFFFF, false);
        gg.pose().popPose();

        // 倒计时/已过期缩放
        float lowerFactor = (state == State.EXPIRED_FLASH) ? EXPIRED_SCALE : COUNTDOWN_SCALE;
        float lowerScale = fitScale(font, lower, contentW, lowerAreaH, lowerFactor);

        int lowerScaledW = (int) (font.width(lower) * lowerScale);
        int lowerScaledH = (int) (font.lineHeight * lowerScale);
        int lowerX = x + (boxW - lowerScaledW) / 2;
        int lowerY = lowerTop + (lowerAreaH - lowerScaledH) / 2;

        if (drawLowerLine) {
            gg.pose().pushPose();
            gg.pose().translate(lowerX, lowerY, 0);
            gg.pose().scale(lowerScale, lowerScale, 1.0f);
            gg.drawString(font, lower, 0, 0, 0xFFFFFFFF, true);
            gg.pose().popPose();
        }

        RenderSystem.enableBlend();
    }

    private void beginExpiredFlow() {
        cancelCountdownTask();

        state = State.EXPIRED_FLASH;
        flashesDone = 0;
        flashVisible = true;
        nextFlashNs = System.nanoTime() + 700_000_000L;
    }

    private void startAnim(State newState, float from, float to, long durationNs) {
        this.state = newState;
        this.animFrom = from;
        this.animTo = to;
        this.animDurationNs = Math.max(1L, durationNs);
        this.animStartNs = System.nanoTime();
        this.animValue = from;
    }

    private void updateAnim(long nowNs) {
        if (!(state == State.ENTERING || state == State.EXITING)) return;

        long elapsed = nowNs - animStartNs;
        float t = (float) elapsed / (float) animDurationNs;
        t = Mth.clamp(t, 0f, 1f);

        float v = Mth.lerp(t, animFrom, animTo);
        animValue = v;
    }

    private boolean animFinished(long nowNs) {
        if (!(state == State.ENTERING || state == State.EXITING)) return false;
        return (nowNs - animStartNs) >= animDurationNs;
    }

    private float easeInOut(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return 0.5f - 0.5f * (float) Math.cos(Math.PI * x);
    }


    private float fitScale(Font font, String text, int maxW, int maxH, float factor) {
        if (text == null) text = "";
        int w = Math.max(1, font.width(text));
        int h = Math.max(1, font.lineHeight);

        float scaleW = (maxW <= 0) ? 1.0f : (float) maxW / (float) w;
        float scaleH = (maxH <= 0) ? 1.0f : (float) maxH / (float) h;

        float fit = Math.min(scaleW, scaleH);
        float out = fit * factor;
        return Mth.clamp(out, MIN_TEXT_SCALE, MAX_TEXT_SCALE);
    }

    private String formatMmSsCs(long millis) {
        long clamped = Math.max(0L, millis);

        long totalSeconds = clamped / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;

        long cs = (clamped % 1000L) / 10L;

        return two(minutes) + ":" + two(seconds) + ":" + two(cs);
    }

    private String two(long v) {
        v = Math.max(0, v);
        if (v < 10) return "0" + v;
        return Long.toString(v);
    }

    private void drawRect(GuiGraphics gg, int x, int y, int w, int h, int argb) {
        gg.fill(x, y, x + w, y + h, argb);
    }

    private void drawBorder(GuiGraphics gg, int x, int y, int w, int h, int argb) {
        // top
        gg.fill(x, y, x + w, y + 1, argb);
        // bottom
        gg.fill(x, y + h - 1, x + w, y + h, argb);
        // left
        gg.fill(x, y, x + 1, y + h, argb);
        // right
        gg.fill(x + w - 1, y, x + w, y + h, argb);
    }
}