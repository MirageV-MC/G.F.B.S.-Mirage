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

    public static final ClientConfigKey<Boolean> ENABLE_BROAD_SYSTEM_REVERB =
            GFBSClientConfigAPI.bool(
                    "audio.broadsystem.reverb.enabled",
                    "音频",
                    "启用喇叭混响效果",
                    "默认关闭。开启后 Broad System 喇叭会播放带有混响效果的音效，模拟真实环境的回声。",
                    false
            );

    public static final ClientConfigKey<Double> BROAD_SYSTEM_REVERB_ROOM_SIZE =
            GFBSClientConfigAPI.dbl(
                    "audio.broadsystem.reverb.room_size",
                    "音频",
                    "喇叭混响-房间大小(%)",
                    "控制混响的空间感大小。0=小房间，100=大教堂。",
                    100.0,
                    0.0,
                    100.0
            );

    public static final ClientConfigKey<Double> BROAD_SYSTEM_REVERB_PRE_DELAY =
            GFBSClientConfigAPI.dbl(
                    "audio.broadsystem.reverb.pre_delay",
                    "音频",
                    "喇叭混响-预延迟(ms)",
                    "早期反射与直达声之间的时间间隔。增加可增强空间感。",
                    0.0,
                    0.0,
                    300.0
            );

    public static final ClientConfigKey<Double> BROAD_SYSTEM_REVERB_REVERB_FEEL =
            GFBSClientConfigAPI.dbl(
                    "audio.broadsystem.reverb.reverb_feel",
                    "音频",
                    "喇叭混响-混响感(%)",
                    "控制混响的扩散程度。数值越大混响越丰富。",
                    20.0,
                    0.0,
                    100.0
            );

    public static final ClientConfigKey<Double> BROAD_SYSTEM_REVERB_DAMPING =
            GFBSClientConfigAPI.dbl(
                    "audio.broadsystem.reverb.damping",
                    "音频",
                    "喇叭混响-消声(%)",
                    "控制高频衰减程度。数值越大混响越暗淡。",
                    4.0,
                    0.0,
                    100.0
            );

    public static final ClientConfigKey<Double> BROAD_SYSTEM_REVERB_LOW_TONE =
            GFBSClientConfigAPI.dbl(
                    "audio.broadsystem.reverb.low_tone",
                    "音频",
                    "喇叭混响-低音调(%)",
                    "控制低频混响增益。数值越大低频越强。",
                    19.0,
                    0.0,
                    100.0
            );

    public static final ClientConfigKey<Double> BROAD_SYSTEM_REVERB_HIGH_TONE =
            GFBSClientConfigAPI.dbl(
                    "audio.broadsystem.reverb.high_tone",
                    "音频",
                    "喇叭混响-高音调(%)",
                    "控制高频混响增益。数值越大高频越亮。",
                    100.0,
                    0.0,
                    100.0
            );

    public static final ClientConfigKey<Double> BROAD_SYSTEM_REVERB_WET_GAIN =
            GFBSClientConfigAPI.dbl(
                    "audio.broadsystem.reverb.wet_gain",
                    "音频",
                    "喇叭混响-湿增益(dB)",
                    "混响信号的增益。正值增强混响，负值减弱。",
                    3.0,
                    -60.0,
                    20.0
            );

    public static final ClientConfigKey<Double> BROAD_SYSTEM_REVERB_DRY_GAIN =
            GFBSClientConfigAPI.dbl(
                    "audio.broadsystem.reverb.dry_gain",
                    "音频",
                    "喇叭混响-干增益(dB)",
                    "原始信号的增益。正值增强原声，负值减弱。",
                    -2.0,
                    -60.0,
                    20.0
            );

    public static final ClientConfigKey<Double> BROAD_SYSTEM_REVERB_STEREO_WIDTH =
            GFBSClientConfigAPI.dbl(
                    "audio.broadsystem.reverb.stereo_width",
                    "音频",
                    "喇叭混响-立体声宽度(%)",
                    "控制混响的立体声宽度。0=单声道，100=全立体声。",
                    14.0,
                    0.0,
                    100.0
            );

}
