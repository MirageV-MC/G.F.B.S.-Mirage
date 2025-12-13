package org.mirage.Client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "mirage_gfbs", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientExposureEvents {

    private ClientExposureEvents() {}

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;

        float dtSec = e.renderTickTime / 20.0f;
        ExposureController.tick(dtSec);
    }

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre e) {
        float a = ExposureController.alpha();
        if (a <= 0.0001f) return;

        GuiGraphics gg = e.getGuiGraphics();
        int w = gg.guiWidth();
        int h = gg.guiHeight();

        // ARGB：alpha<<24 | (rgb & 0xFFFFFF)
        int argb = ((int) (a * 255.0f) << 24) | (ExposureController.rgb & 0xFFFFFF);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        gg.fill(0, 0, w, h, argb);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
