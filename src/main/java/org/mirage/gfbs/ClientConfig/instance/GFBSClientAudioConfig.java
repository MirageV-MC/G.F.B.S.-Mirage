package org.mirage.gfbs.ClientConfig.instance;

import org.mirage.gfbs.ClientConfig.ClientConfigKey;
import org.mirage.gfbs.ClientConfig.GFBSClientConfigAPI;

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

    public static final ClientConfigKey<Boolean> ENABLE_DISTORTION =
            GFBSClientConfigAPI.bool(
                    "audio.distortion.enabled",
                    "音频",
                    "启用喇叭失真效果",
                    "默认开启。开启后 Broad System 喇叭会播放带有软失真和旧感的音效，模拟老旧村喇叭的质感。",
                    true
            );

    public static final ClientConfigKey<Double> DISTORTION_STRENGTH =
            GFBSClientConfigAPI.dbl(
                    "audio.distortion.strength",
                    "音频",
                    "失真强度",
                    "控制失真效果的强度。数值越大失真越明显，0 会关闭失真但保留均衡器效果。",
                    0.5,
                    0.0,
                    1.0
            );

}
