package org.mirage.Client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class LoopingSoundInstance extends AbstractTickableSoundInstance {

    private final float minVolume;

    public LoopingSoundInstance(SoundEvent sound,
                                SoundSource source,
                                float volume,
                                float pitch,
                                float minVolume) {
        super(sound, source, SoundInstance.createUnseededRandom());
        this.volume = volume;
        this.pitch = pitch;
        this.minVolume = minVolume;
        this.looping = true;
        this.delay = 0;
        this.relative = false; // 若想跟随玩家可改 true
    }

    @Override
    public void tick() {
        // 不做任何检查，一直播放，直到被 stop
    }

    @Override
    public float getVolume() {
        return Math.max(this.minVolume, super.getVolume());
    }
}