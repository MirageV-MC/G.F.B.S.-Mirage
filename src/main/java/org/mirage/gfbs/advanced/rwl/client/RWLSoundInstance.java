package org.mirage.gfbs.advanced.rwl.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.mirage.gfbs.advanced.rwl.RWLApi;
import org.mirage.gfbs.advanced.rwl.RotatingWarningLightBlockEntity;

public class RWLSoundInstance extends AbstractTickableSoundInstance {

    private final RotatingWarningLightBlockEntity be;
    private boolean stopped = false;

    public RWLSoundInstance(RotatingWarningLightBlockEntity be) {
        super(resolve(be), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.be = be;

        this.looping = true;
        this.delay = 0;

        this.x = be.getBlockPos().getX() + 0.5;
        this.y = be.getBlockPos().getY() + 0.5;
        this.z = be.getBlockPos().getZ() + 0.5;

        this.volume = 1.0f;
        this.pitch = 1.0f;
    }

    private static SoundEvent resolve(RotatingWarningLightBlockEntity be) {
        @Nullable SoundEvent se = RWLApi.resolveSoundEvent(be.getSoundId());
        return se != null ? se : net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value();
    }

    @Override
    public void tick() {
        if (stopped) return;

        if (be.isRemoved() || be.getLevel() == null) {
            stopNow();
            return;
        }

        if (!be.isPoweredCached()) {
            stopNow();
            return;
        }

        this.x = be.getBlockPos().getX() + 0.5;
        this.y = be.getBlockPos().getY() + 0.5;
        this.z = be.getBlockPos().getZ() + 0.5;

        var cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
        double dx = this.x - cam.getPosition().x;
        double dy = this.y - cam.getPosition().y;
        double dz = this.z - cam.getPosition().z;
        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
        float v = (float) (1.0 / (1.0 + dist * 0.15));
        this.volume = Mth.clamp(v, 0.0f, 1.0f);
    }

    public void requestStop() {
        if (this.stopped) return;
        this.stopped = true;
        this.stop();
    }

    private void stopNow() {
        this.stopped = true;
        this.stop();
    }
}
