package org.mirage.gfbs.advanced.rwl.client;

import net.minecraft.client.Minecraft;
import org.mirage.gfbs.advanced.rwl.RotatingWarningLightBlockEntity;

import java.util.IdentityHashMap;
import java.util.Map;

public final class RWLClientSoundRegistry {

    private static final Map<RotatingWarningLightBlockEntity, RWLSoundInstance> PLAYING = new IdentityHashMap<>();

    private RWLClientSoundRegistry() {}

    public static void onLoad(RotatingWarningLightBlockEntity be) {
        if (!be.isPoweredCached()) return;
        if (PLAYING.containsKey(be)) return;

        RWLSoundInstance inst = new RWLSoundInstance(be);
        PLAYING.put(be, inst);
        Minecraft.getInstance().getSoundManager().play(inst);
    }

    public static void onRemove(RotatingWarningLightBlockEntity be) {
        RWLSoundInstance inst = PLAYING.remove(be);
        if (inst != null) {
            inst.requestStop();
        }
    }

    public static void onPowerChange(RotatingWarningLightBlockEntity be, boolean powered) {
        if (powered) {
            onLoad(be);
        } else {
            onRemove(be);
        }
    }
}
