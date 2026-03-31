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

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mirage.gfbs.MirageGFBS;
import org.mirage.gfbs.Tools.Task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(modid = MirageGFBS.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ScreenTextOverlay {

    private static final List<TextEntry> activeEntries = new ArrayList<>();
    private static final Random random = new Random();
    
    private ScreenTextOverlay() {}

    public static void showCenteredText(String text, int color, long durationMs) {
        showCenteredText(text, color, durationMs, 1.0f);
    }

    public static void showCenteredText(String text, int color, long durationMs, float scale) {
        TextEntry entry = new TextEntry(
            text,
            color,
            durationMs,
            TextDisplayMode.CENTERED,
            scale,
            0,
            0
        );
        activeEntries.add(entry);
        scheduleRemoval(entry, durationMs);
    }

    public static void showCenteredTextWithPosition(String text, int color, long durationMs, float scale, int offsetX, int offsetY) {
        TextEntry entry = new TextEntry(
            text,
            color,
            durationMs,
            TextDisplayMode.CENTERED,
            scale,
            offsetX,
            offsetY
        );
        activeEntries.add(entry);
        scheduleRemoval(entry, durationMs);
    }

    public static void showFullScreenText(String text, int color, long durationMs) {
        showFullScreenText(text, color, durationMs, 1.5f, 0.3f);
    }

    public static void showFullScreenText(String text, int color, long durationMs, float scale, float density) {
        TextEntry entry = new TextEntry(
            text,
            color,
            durationMs,
            TextDisplayMode.FULL_SCREEN,
            scale,
            density,
            0
        );
        activeEntries.add(entry);
        scheduleRemoval(entry, durationMs);
    }

    public static void showGlitchText(String text, int color, long durationMs) {
        TextEntry entry = new TextEntry(
            text,
            color,
            durationMs,
            TextDisplayMode.GLITCH,
            2.0f,
            0,
            0
        );
        activeEntries.add(entry);
        scheduleRemoval(entry, durationMs);
    }

    public static void showTypewriterText(String text, int color, long durationMs) {
        TextEntry entry = new TextEntry(
            text,
            color,
            durationMs,
            TextDisplayMode.TYPEWRITER,
            1.0f,
            0,
            0
        );
        activeEntries.add(entry);
        scheduleRemoval(entry, durationMs);
    }

    public static void showCreepyText(String text, int color, long durationMs) {
        TextEntry entry = new TextEntry(
            text,
            color,
            durationMs,
            TextDisplayMode.CREEPY,
            1.5f,
            0,
            0
        );
        activeEntries.add(entry);
        scheduleRemoval(entry, durationMs);
    }

    public static void showMultiLineText(List<String> lines, int color, long durationMs, float scale) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int offsetY = (i - lines.size() / 2) * (int)(Minecraft.getInstance().font.lineHeight * scale * 1.2f);
            TextEntry entry = new TextEntry(
                line,
                color,
                durationMs,
                TextDisplayMode.CENTERED,
                scale,
                0,
                offsetY
            );
            activeEntries.add(entry);
            scheduleRemoval(entry, durationMs);
        }
    }

    private static void scheduleRemoval(TextEntry entry, long delayMs) {
        Task.delay(() -> {
            activeEntries.remove(entry);
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    public static void clearAll() {
        activeEntries.clear();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        long currentTime = System.currentTimeMillis();
        
        Iterator<TextEntry> iterator = activeEntries.iterator();
        while (iterator.hasNext()) {
            TextEntry entry = iterator.next();
            if (entry.isExpired(currentTime)) {
                iterator.remove();
                continue;
            }
            entry.render(event.getGuiGraphics(), event.getPartialTick(), currentTime);
        }
    }

    public static boolean hasActiveText() {
        return !activeEntries.isEmpty();
    }

    public static int getActiveTextCount() {
        return activeEntries.size();
    }

    private enum TextDisplayMode {
        CENTERED,
        FULL_SCREEN,
        GLITCH,
        TYPEWRITER,
        CREEPY
    }

    private static final class TextEntry {
        private final String text;
        private final int color;
        private final long endTime;
        private final TextDisplayMode mode;
        private final float scale;
        private final float density;
        private final int offsetX;
        private final int offsetY;
        private final long startTime;
        
        private int typewriterIndex;
        
        private TextEntry(String text, int color, long durationMs, TextDisplayMode mode, 
                         float scale, float density, int offsetY) {
            this.text = text;
            this.color = color;
            this.startTime = System.currentTimeMillis();
            this.endTime = startTime + durationMs;
            this.mode = mode;
            this.scale = scale;
            this.density = density;
            this.offsetX = 0;
            this.offsetY = offsetY;
            this.typewriterIndex = 0;
        }
        
        private boolean isExpired(long currentTime) {
            return currentTime >= endTime;
        }
        
        private void render(GuiGraphics guiGraphics, float partialTick, long currentTime) {
            Minecraft mc = Minecraft.getInstance();
            Font font = mc.font;
            
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            
            updateAnimationState(currentTime);
            
            switch (mode) {
                case CENTERED:
                    renderCentered(guiGraphics, font, screenWidth, screenHeight);
                    break;
                case FULL_SCREEN:
                    renderFullScreen(guiGraphics, font, screenWidth, screenHeight);
                    break;
                case GLITCH:
                    renderGlitch(guiGraphics, font, screenWidth, screenHeight);
                    break;
                case TYPEWRITER:
                    renderTypewriter(guiGraphics, font, screenWidth, screenHeight);
                    break;
                case CREEPY:
                    renderCreepy(guiGraphics, font, screenWidth, screenHeight, currentTime);
                    break;
            }
            
            RenderSystem.disableBlend();
        }
        
        private void updateAnimationState(long currentTime) {
            if (mode == TextDisplayMode.TYPEWRITER) {
                long elapsed = currentTime - startTime;
                typewriterIndex = (int) (elapsed / 80);
                if (typewriterIndex > text.length()) {
                    typewriterIndex = text.length();
                }
            }
        }
        
        private void renderCentered(GuiGraphics guiGraphics, Font font, int screenWidth, int screenHeight) {
            int textWidth = (int) (font.width(text) * scale);
            int textHeight = (int) (font.lineHeight * scale);
            
            int x = (screenWidth - textWidth) / 2 + offsetX;
            int y = (screenHeight - textHeight) / 2 + offsetY;
            
            int renderColor = 0xFF000000 | color;
            
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y, 0);
            guiGraphics.pose().scale(scale, scale, 1.0f);
            guiGraphics.drawString(font, text, 0, 0, renderColor, true);
            guiGraphics.pose().popPose();
        }
        
        private void renderFullScreen(GuiGraphics guiGraphics, Font font, int screenWidth, int screenHeight) {
            int textWidth = font.width(text);
            int textHeight = font.lineHeight;
            
            int spacingX = (int) (textWidth * scale * (1.0f + density));
            int spacingY = (int) (textHeight * scale * (1.0f + density));
            
            int cols = screenWidth / spacingX + 2;
            int rows = screenHeight / spacingY + 2;
            
            int startX = -spacingX;
            int startY = -spacingY;
            
            int renderColor = 0xFF000000 | color;
            
            guiGraphics.pose().pushPose();
            
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    int x = startX + col * spacingX;
                    int y = startY + row * spacingY;
                    
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(x, y, 0);
                    guiGraphics.pose().scale(scale, scale, 1.0f);
                    guiGraphics.drawString(font, text, 0, 0, renderColor, false);
                    guiGraphics.pose().popPose();
                }
            }
            
            guiGraphics.pose().popPose();
        }
        
        private void renderGlitch(GuiGraphics guiGraphics, Font font, int screenWidth, int screenHeight) {
            int textWidth = (int) (font.width(text) * scale);
            int textHeight = (int) (font.lineHeight * scale);
            
            int baseX = (screenWidth - textWidth) / 2;
            int baseY = (screenHeight - textHeight) / 2;
            
            for (int i = 0; i < 3; i++) {
                if (random.nextFloat() > 0.7f) {
                    int glitchColor = getGlitchColor(i);
                    
                    int offsetX = random.nextInt(6) - 3;
                    int offsetY = random.nextInt(4) - 2;
                    
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(baseX + offsetX, baseY + offsetY, 0);
                    guiGraphics.pose().scale(scale, scale, 1.0f);
                    guiGraphics.drawString(font, text, 0, 0, glitchColor, false);
                    guiGraphics.pose().popPose();
                }
            }
            
            int mainColor = 0xFF000000 | color;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(baseX, baseY, 0);
            guiGraphics.pose().scale(scale, scale, 1.0f);
            guiGraphics.drawString(font, text, 0, 0, mainColor, true);
            guiGraphics.pose().popPose();
        }
        
        private void renderTypewriter(GuiGraphics guiGraphics, Font font, int screenWidth, int screenHeight) {
            String displayText = text.substring(0, Math.min(typewriterIndex, text.length()));
            
            int textWidth = (int) (font.width(displayText) * scale);
            int textHeight = (int) (font.lineHeight * scale);
            
            int x = (screenWidth - textWidth) / 2;
            int y = (screenHeight - textHeight) / 2;
            
            int renderColor = 0xFF000000 | color;
            
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x, y, 0);
            guiGraphics.pose().scale(scale, scale, 1.0f);
            guiGraphics.drawString(font, displayText, 0, 0, renderColor, true);
            
            if (typewriterIndex < text.length()) {
                int cursorX = font.width(displayText);
                guiGraphics.drawString(font, "_", cursorX, 0, renderColor, false);
            }
            
            guiGraphics.pose().popPose();
        }
        
        private void renderCreepy(GuiGraphics guiGraphics, Font font, int screenWidth, int screenHeight, long currentTime) {
            int textWidth = (int) (font.width(text) * scale);
            int textHeight = (int) (font.lineHeight * scale);
            
            int baseX = (screenWidth - textWidth) / 2;
            int baseY = (screenHeight - textHeight) / 2;
            
            float creepyOffset = (currentTime - startTime) / 100.0f;
            
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == ' ') continue;
                
                float charOffsetX = (float) Math.sin(creepyOffset + i * 0.5) * 3;
                float charOffsetY = (float) Math.cos(creepyOffset + i * 0.7) * 2;
                
                int charX = (int) (baseX + font.width(text.substring(0, i)) * scale + charOffsetX);
                int charY = (int) (baseY + charOffsetY);
                
                int renderColor = 0xFF000000 | color;
                
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(charX, charY, 0);
                guiGraphics.pose().scale(scale, scale, 1.0f);
                guiGraphics.drawString(font, String.valueOf(c), 0, 0, renderColor, true);
                guiGraphics.pose().popPose();
            }
        }
        
        private int getGlitchColor(int index) {
            switch (index) {
                case 0: return 0xFFFF0000;
                case 1: return 0xFF00FF00;
                case 2: return 0xFF0000FF;
                default: return 0xFF000000 | color;
            }
        }
    }

    public static final class Colors {
        public static final int WHITE = 0xFFFFFF;
        public static final int RED = 0xFF0000;
        public static final int GREEN = 0x00FF00;
        public static final int BLUE = 0x0000FF;
        public static final int YELLOW = 0xFFFF00;
        public static final int CYAN = 0x00FFFF;
        public static final int MAGENTA = 0xFF00FF;
        public static final int ORANGE = 0xFF8800;
        public static final int PURPLE = 0x8800FF;
        public static final int DARK_RED = 0x880000;
        public static final int BLOOD_RED = 0x660000;
        public static final int GHOST_WHITE = 0xCCCCCC;
        public static final int ERROR_RED = 0xFF3333;
        public static final int WARNING_YELLOW = 0xFFCC00;
        public static final int SYSTEM_GREEN = 0x00FF66;
        
        public static int rgb(int r, int g, int b) {
            return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
        }
        
        public static int rgba(int r, int g, int b, int a) {
            return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
        }
    }
}
