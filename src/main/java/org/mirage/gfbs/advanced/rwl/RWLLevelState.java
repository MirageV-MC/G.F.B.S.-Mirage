package org.mirage.gfbs.advanced.rwl;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public class RWLLevelState extends SavedData {

    public static final String DATA_NAME = "rwl_level_state";

    private boolean enabled = false;

    private int colorR = 255;
    private int colorG = 64;
    private int colorB = 64;

    private String soundId = "minecraft:block.note_block.bell";
    private long msPerRevolution = 1200L;

    public RWLLevelState() {}

    public static @NotNull RWLLevelState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RWLLevelState::load, RWLLevelState::new, DATA_NAME);
    }

    private static RWLLevelState load(CompoundTag tag) {
        RWLLevelState st = new RWLLevelState();
        st.enabled = tag.getBoolean("enabled");
        st.colorR = RWLApi.clamp255(tag.getInt("r"));
        st.colorG = RWLApi.clamp255(tag.getInt("g"));
        st.colorB = RWLApi.clamp255(tag.getInt("b"));
        st.soundId = tag.contains("soundId") ? tag.getString("soundId") : st.soundId;
        st.msPerRevolution = RWLApi.clampMs(tag.getLong("msPerRev"));
        return st;
    }

    public @NotNull CompoundTag save(CompoundTag tag) {
        tag.putBoolean("enabled", enabled);
        tag.putInt("r", colorR);
        tag.putInt("g", colorG);
        tag.putInt("b", colorB);
        tag.putString("soundId", soundId);
        tag.putLong("msPerRev", msPerRevolution);
        return tag;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getColorR() { return colorR; }
    public int getColorG() { return colorG; }
    public int getColorB() { return colorB; }
    public String getSoundId() { return soundId; }
    public long getMsPerRevolution() { return msPerRevolution; }

    public void setConfig(int r, int g, int b, String soundId, long msPerRev) {
        this.colorR = RWLApi.clamp255(r);
        this.colorG = RWLApi.clamp255(g);
        this.colorB = RWLApi.clamp255(b);
        if (soundId != null && !soundId.isEmpty()) this.soundId = soundId;
        this.msPerRevolution = RWLApi.clampMs(msPerRev);
    }

    public void applyTo(ServerLevel sl, RotatingWarningLightBlockEntity be, boolean updateBlockStatePowered) {
        be.applyFromLevelState(sl, this, updateBlockStatePowered);
    }
}
