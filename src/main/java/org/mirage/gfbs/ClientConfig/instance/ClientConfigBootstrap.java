package org.mirage.gfbs.ClientConfig.instance;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.mirage.gfbs.Client.audio.MirageDistortion;
import org.mirage.gfbs.Client.audio.MirageEqualizer;
import org.mirage.gfbs.Client.audio.MirageReverb;
import org.mirage.gfbs.ClientConfig.GFBSClientConfigAPI;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientConfigBootstrap {

    private ClientConfigBootstrap() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        reverbInit();
        distortionInit();

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

    // 失真配置初始化
    private static void distortionInit(){
        GFBSClientAudioConfig.ENABLE_DISTORTION.hashCode();
        GFBSClientAudioConfig.DISTORTION_STRENGTH.hashCode();

        applyDistortionSettings();

        GFBSClientConfigAPI.onChange(GFBSClientAudioConfig.ENABLE_DISTORTION, (k, oldV, newV) -> {
            if (Boolean.FALSE.equals(newV)) {
                MirageDistortion.shutdown();
            } else {
                MirageDistortion.ensureInit();
                applyDistortionSettings();
            }
        });

        GFBSClientConfigAPI.onChange(GFBSClientAudioConfig.DISTORTION_STRENGTH, (k, oldV, newV) -> {
            applyDistortionSettings();
        });
    }

    private static void applyDistortionSettings() {
        double strength = GFBSClientConfigAPI.get(GFBSClientAudioConfig.DISTORTION_STRENGTH);
        boolean enabled = GFBSClientConfigAPI.get(GFBSClientAudioConfig.ENABLE_DISTORTION);
        
        if (!enabled) {
            return;
        }
        
        MirageDistortion.setStrength((float) strength);
        
        float lowGain = 3.0f + (float)(strength * 1.5f);
        float mid1Gain = 4.0f + (float)(strength * 1.5f);
        float mid2Gain = 1.5f + (float)(strength * 1.0f);
        float highGain = -6.0f - (float)(strength * 3.0f);
        
        MirageEqualizer.setLowGain(lowGain);
        MirageEqualizer.setMid1Gain(mid1Gain);
        MirageEqualizer.setMid2Gain(mid2Gain);
        MirageEqualizer.setHighGain(highGain);
        
        System.out.println("[MirageGFBS] Distortion settings applied: strength=" + strength + 
                          ", low=" + lowGain + ", mid1=" + mid1Gain + ", mid2=" + mid2Gain + ", high=" + highGain);
    }
}
