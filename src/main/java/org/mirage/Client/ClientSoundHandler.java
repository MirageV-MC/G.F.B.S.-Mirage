package org.mirage.Client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSoundHandler {

    /** key: soundId + "|" + source  ->  LoopingSoundInstance */
    private static final Map<String, LoopingSoundInstance> LOOPING_SOUNDS = new HashMap<>();

    public static void startLoopSound(ResourceLocation soundId,
                                      SoundSource source,
                                      float volume,
                                      float pitch,
                                      float minVolume) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        SoundEvent soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(soundId);
        if (soundEvent == null) {
            return;
        }

        String key = makeKey(soundId, source);
        stopLoopSound(soundId, source);

        LoopingSoundInstance instance = new LoopingSoundInstance(soundEvent, source, volume, pitch, minVolume);
        LOOPING_SOUNDS.put(key, instance);
        mc.getSoundManager().play(instance);
    }

    public static void stopLoopSound(ResourceLocation soundId, SoundSource source) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        String key = makeKey(soundId, source);
        LoopingSoundInstance instance = LOOPING_SOUNDS.remove(key);
        if (instance != null) {
            mc.getSoundManager().stop((SoundInstance) instance);
        }
    }

    private static String makeKey(ResourceLocation id, SoundSource source) {
        return id.toString() + "|" + source.name();
    }

    @SubscribeEvent
    public static void onReload(RegisterClientReloadListenersEvent event) {
        LOOPING_SOUNDS.clear();
    }
}