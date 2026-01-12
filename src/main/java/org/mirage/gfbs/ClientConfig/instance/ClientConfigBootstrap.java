package org.mirage.gfbs.ClientConfig.instance;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.mirage.gfbs.Client.audio.MirageReverb;
import org.mirage.gfbs.ClientConfig.GFBSClientConfigAPI;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientConfigBootstrap {

    private ClientConfigBootstrap() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        reverbInit();

        GFBSClientConfigAPI.reloadFromDisk();
    }

    // 混响配置初始化
    private static void reverbInit(){
        GFBSClientAudioConfig.ENABLE_REVERB.hashCode();
        GFBSClientAudioConfig.REVERB_STRENGTH.hashCode();

        MirageReverb.setStrength(GFBSClientConfigAPI.get(GFBSClientAudioConfig.REVERB_STRENGTH));

        if (!GFBSClientConfigAPI.get(GFBSClientAudioConfig.ENABLE_REVERB)) {
            MirageReverb.shutdown();
        }

        GFBSClientConfigAPI.onChange(GFBSClientAudioConfig.ENABLE_REVERB, (k, oldV, newV) -> {
            if (Boolean.TRUE.equals(newV)) {
                MirageReverb.setStrength(GFBSClientConfigAPI.get(GFBSClientAudioConfig.REVERB_STRENGTH));
            } else {
                MirageReverb.shutdown();
            }
        });

        GFBSClientConfigAPI.onChange(GFBSClientAudioConfig.REVERB_STRENGTH, (k, oldV, newV) -> {
            MirageReverb.setStrength(newV);

            if (GFBSClientConfigAPI.get(GFBSClientAudioConfig.ENABLE_REVERB)){
                MirageReverb.shutdown();
            }
        });
    }
}
