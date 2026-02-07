package org.mirage.gfbs.advanced.rwl.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mirage.gfbs.MirageGFBS;

/**
 * 在世界渲染完成后，执行屏幕空间后处理（彩色聚光）。
 */
@Mod.EventBusSubscriber(modid = MirageGFBS.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RWLSpotlightPostHooks {

    private RWLSpotlightPostHooks() {}

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        RWLSpotlightPost.INSTANCE.onAfterLevelRender(e.getPartialTick());
    }
}
