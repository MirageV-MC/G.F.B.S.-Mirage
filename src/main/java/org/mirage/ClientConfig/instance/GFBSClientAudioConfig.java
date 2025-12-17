package org.mirage.ClientConfig.instance;

import org.mirage.ClientConfig.ClientConfigKey;
import org.mirage.ClientConfig.GFBSClientConfigAPI;

public final class GFBSClientAudioConfig {

    private GFBSClientAudioConfig() {}

    public static final ClientConfigKey<Boolean> ENABLE_REVERB =
            GFBSClientConfigAPI.bool(
                    "audio.reverb.enabled",
                    "音频",
                    "启用混响 (实验性)",
                    "默认关闭。开启后会对播放的声音增加 OpenAL EFX 混响效果。",
                    false
            );


    public static final ClientConfigKey<Double> REVERB_STRENGTH =
            GFBSClientConfigAPI.dbl(
                    "audio.reverb.strength",
                    "音频",
                    "混响强度 (实验性)",
                    "1 = 默认混响强度。数值越大混响越强，0 会显著减弱 (但不等同于关闭)。",
                    1.0,
                    0.0,
                    5.0
            );

}
