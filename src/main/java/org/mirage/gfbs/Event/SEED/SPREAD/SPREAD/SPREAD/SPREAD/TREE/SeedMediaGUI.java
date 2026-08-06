package org.mirage.gfbs.Event.SEED.SPREAD.SPREAD.SPREAD.SPREAD.TREE;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.mirage.gfbs.MirageGFBS;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SeedMediaGUI extends Screen {

    private static final String TEXTURE_PATH = "textures/gui/seedseedseedseedseed/";
    private static final long DEFAULT_DISPLAY_TIME = 5000L;
    private static final long FADE_DURATION = 500L;

    private static final List<MediaEntry> MEDIA_LIBRARY = new ArrayList<>();

    private static int currentIndex = -1;
    private static int nextIndex = -1;

    private static boolean showing = false;
    private static boolean transitioning = false;
    private static boolean hiding = false;

    private static long displayDuration = DEFAULT_DISPLAY_TIME;
    private static long pendingDisplayDuration = DEFAULT_DISPLAY_TIME;

    private static long stateStartTime = 0L;
    private static float fadeAlpha = 0.0f;

    public SeedMediaGUI() {
        super(Component.translatable("seed_media_gui.title"));
    }

    public static void registerImage(String id, String fileName) {
        if (id == null || id.isBlank()) {
            MirageGFBS.LOGGER.warn("SeedMediaGUI: registerImage failed, id is null or blank");
            return;
        }
        if (fileName == null || fileName.isBlank()) {
            MirageGFBS.LOGGER.warn("SeedMediaGUI: registerImage failed, fileName is null or blank, id={}", id);
            return;
        }

        ResourceLocation location = ResourceLocation.tryBuild(MirageGFBS.MODID, TEXTURE_PATH + fileName);
        if (location == null) {
            MirageGFBS.LOGGER.warn("SeedMediaGUI: registerImage failed, invalid resource location for id={}, fileName={}", id, fileName);
            return;
        }

        int existingIndex = findMediaIndex(id);
        if (existingIndex >= 0) {
            MEDIA_LIBRARY.set(existingIndex, new MediaEntry(id, location, MediaType.IMAGE));
            return;
        }

        MEDIA_LIBRARY.add(new MediaEntry(id, location, MediaType.IMAGE));
    }

    public static void showMedia(String mediaId) {
        showMedia(mediaId, DEFAULT_DISPLAY_TIME);
    }

    public static void showMedia(String mediaId, long durationMs) {
        int index = findMediaIndex(mediaId);
        if (index < 0) {
            MirageGFBS.LOGGER.warn("SeedMediaGUI: media not found: {}", mediaId);
            return;
        }
        showMediaByIndex(index, durationMs);
    }

    public static void showMediaByIndex(int index, long durationMs) {
        if (MEDIA_LIBRARY.isEmpty()) {
            MirageGFBS.LOGGER.warn("SeedMediaGUI: cannot show media, media library is empty");
            return;
        }
        if (index < 0 || index >= MEDIA_LIBRARY.size()) {
            MirageGFBS.LOGGER.warn("SeedMediaGUI: invalid media index: {}", index);
            return;
        }

        long safeDuration = Math.max(0L, durationMs);

        if (!showing && currentIndex < 0) {
            currentIndex = index;
            displayDuration = safeDuration;
            pendingDisplayDuration = safeDuration;
            showing = true;
            transitioning = false;
            hiding = false;
            fadeAlpha = 0.0f;
            stateStartTime = System.currentTimeMillis();
            return;
        }

        if (currentIndex == index && showing && !transitioning && !hiding) {
            displayDuration = safeDuration;
            pendingDisplayDuration = safeDuration;
            stateStartTime = System.currentTimeMillis();
            return;
        }

        nextIndex = index;
        pendingDisplayDuration = safeDuration;

        if (!showing) {
            currentIndex = nextIndex;
            displayDuration = pendingDisplayDuration;
            nextIndex = -1;
            showing = true;
            transitioning = false;
            hiding = false;
            fadeAlpha = 0.0f;
            stateStartTime = System.currentTimeMillis();
            return;
        }

        transitioning = true;
        hiding = false;
        stateStartTime = System.currentTimeMillis();
    }

    public static void showNext() {
        if (MEDIA_LIBRARY.isEmpty()) {
            return;
        }

        int baseIndex = currentIndex;
        if (baseIndex < 0 || baseIndex >= MEDIA_LIBRARY.size()) {
            baseIndex = 0;
        }

        int next = (baseIndex + 1) % MEDIA_LIBRARY.size();
        showMediaByIndex(next, DEFAULT_DISPLAY_TIME);
    }

    public static void showPrevious() {
        if (MEDIA_LIBRARY.isEmpty()) {
            return;
        }

        int baseIndex = currentIndex;
        if (baseIndex < 0 || baseIndex >= MEDIA_LIBRARY.size()) {
            baseIndex = 0;
        }

        int prev = (baseIndex - 1 + MEDIA_LIBRARY.size()) % MEDIA_LIBRARY.size();
        showMediaByIndex(prev, DEFAULT_DISPLAY_TIME);
    }

    public static void hide() {
        if (!showing) {
            return;
        }

        transitioning = false;
        hiding = true;
        nextIndex = -1;
        stateStartTime = System.currentTimeMillis();
    }

    public static void forceHide() {
        showing = false;
        transitioning = false;
        hiding = false;
        nextIndex = -1;
        currentIndex = -1;
        fadeAlpha = 0.0f;
        stateStartTime = 0L;
    }

    public static void renderOverlay(GuiGraphics guiGraphics, float partialTick) {
        if (MEDIA_LIBRARY.isEmpty()) {
            forceHide();
            return;
        }

        if (!showing || currentIndex < 0 || currentIndex >= MEDIA_LIBRARY.size()) {
            return;
        }

        long now = System.currentTimeMillis();

        if (transitioning) {
            float progress = getProgress(now, stateStartTime, FADE_DURATION);
            fadeAlpha = 1.0f - progress;

            if (progress >= 1.0f) {
                currentIndex = nextIndex >= 0 ? nextIndex : currentIndex;
                nextIndex = -1;
                displayDuration = pendingDisplayDuration;
                transitioning = false;
                hiding = false;
                fadeAlpha = 0.0f;
                stateStartTime = now;
            }
        } else if (hiding) {
            float progress = getProgress(now, stateStartTime, FADE_DURATION);
            fadeAlpha = 1.0f - progress;

            if (progress >= 1.0f) {
                forceHide();
                return;
            }
        } else {
            long elapsed = now - stateStartTime;

            if (elapsed < FADE_DURATION) {
                fadeAlpha = getProgress(now, stateStartTime, FADE_DURATION);
            } else if (displayDuration > 0L && elapsed >= displayDuration) {
                long fadeOutStart = stateStartTime + displayDuration;
                float progress = getProgress(now, fadeOutStart, FADE_DURATION);
                fadeAlpha = 1.0f - progress;

                if (progress >= 1.0f) {
                    forceHide();
                    return;
                }
            } else {
                fadeAlpha = 1.0f;
            }
        }

        fadeAlpha = clamp01(fadeAlpha);

        MediaEntry entry = MEDIA_LIBRARY.get(currentIndex);
        renderMedia(guiGraphics, entry, fadeAlpha);
    }

    private static void renderMedia(GuiGraphics guiGraphics, MediaEntry entry, float alpha) {
        if (entry == null || entry.location == null || alpha <= 0.001f) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, entry.location);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

        if (entry.type == MediaType.IMAGE) {
            // 使用 blit 方法铺满整个屏幕
            // 参数: location, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight
            guiGraphics.blit(entry.location, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static float getProgress(long now, long start, long duration) {
        if (duration <= 0L) {
            return 1.0f;
        }
        return clamp01((float) (now - start) / (float) duration);
    }

    private static float clamp01(float value) {
        if (value < 0.0f) {
            return 0.0f;
        }
        if (value > 1.0f) {
            return 1.0f;
        }
        return value;
    }

    private static int findMediaIndex(String mediaId) {
        if (mediaId == null) {
            return -1;
        }

        for (int i = 0; i < MEDIA_LIBRARY.size(); i++) {
            MediaEntry entry = MEDIA_LIBRARY.get(i);
            if (entry != null && Objects.equals(entry.id, mediaId)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isShowing() {
        return showing;
    }

    public static int getMediaCount() {
        return MEDIA_LIBRARY.size();
    }

    public static int getCurrentIndex() {
        return currentIndex;
    }

    public static void setDisplayDuration(long durationMs) {
        displayDuration = Math.max(0L, durationMs);
        pendingDisplayDuration = displayDuration;
    }

    public static void clearLibrary() {
        MEDIA_LIBRARY.clear();
        forceHide();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderOverlay(guiGraphics, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class MediaEntry {
        final String id;
        final ResourceLocation location;
        final MediaType type;

        MediaEntry(String id, ResourceLocation location, MediaType type) {
            this.id = id;
            this.location = location;
            this.type = type;
        }
    }

    private enum MediaType {
        IMAGE,
        VIDEO
    }
}