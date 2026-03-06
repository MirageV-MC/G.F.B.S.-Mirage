package org.mirage.gfbs.objects.blocks.Control.Gate;

import java.util.Objects;

/**
 * Gate type metadata.
 *
 * <p>Goal: avoid class explosion (GateBlock/ClientAPI/ServerManager/BlockEntity per gate variant).
 * A new gate variant should be introduced by registering a {@link GateType} in {@link GateTypes},
 * not by copying classes.</p>
 */
public final class GateType {
    private final String id;
    private final String texturePath;
    private final String modelPath;
    private final String animationPath;

    private final String animOpen;
    private final String animClose;
    private final String animOpenIdle;
    private final String animIdle;

    private final String openSoundKey;
    private final String closeSoundKey;
    private final float openSoundVolume;
    private final float closeSoundVolume;

    public GateType(String id, String texturePath) {
        this(
                id,
                texturePath,
                "geo/gate.geo.json",
                "animations/gate.animation.json",
                "animation.gate.open",
                "animation.gate.close",
                "animation.gate.open_idle",
                "animation.gate.idle",
                "surroundings.big_gate_reverb",
                "surroundings.big_gate_reverb",
                1.0F,
                1.0F
        );
    }

    public GateType(
            String id,
            String texturePath,
            String modelPath,
            String animationPath,
            String animOpen,
            String animClose,
            String animOpenIdle,
            String animIdle,
            String openSoundKey,
            String closeSoundKey
    ) {
        this(
                id,
                texturePath,
                modelPath,
                animationPath,
                animOpen,
                animClose,
                animOpenIdle,
                animIdle,
                openSoundKey,
                closeSoundKey,
                1.0F,
                1.0F
        );
    }

    public GateType(
            String id,
            String texturePath,
            String modelPath,
            String animationPath,
            String animOpen,
            String animClose,
            String animOpenIdle,
            String animIdle,
            String openSoundKey,
            String closeSoundKey,
            float openSoundVolume,
            float closeSoundVolume
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.texturePath = Objects.requireNonNull(texturePath, "texturePath");
        this.modelPath = Objects.requireNonNull(modelPath, "modelPath");
        this.animationPath = Objects.requireNonNull(animationPath, "animationPath");
        this.animOpen = Objects.requireNonNull(animOpen, "animOpen");
        this.animClose = Objects.requireNonNull(animClose, "animClose");
        this.animOpenIdle = Objects.requireNonNull(animOpenIdle, "animOpenIdle");
        this.animIdle = Objects.requireNonNull(animIdle, "animIdle");
        this.openSoundKey = Objects.requireNonNull(openSoundKey, "openSoundKey");
        this.closeSoundKey = Objects.requireNonNull(closeSoundKey, "closeSoundKey");
        this.openSoundVolume = openSoundVolume;
        this.closeSoundVolume = closeSoundVolume;

        if (this.id.isEmpty()) throw new IllegalArgumentException("GateType id cannot be empty.");
        if (this.texturePath.isEmpty()) throw new IllegalArgumentException("GateType texturePath cannot be empty.");
        if (this.modelPath.isEmpty()) throw new IllegalArgumentException("GateType modelPath cannot be empty.");
        if (this.animationPath.isEmpty()) throw new IllegalArgumentException("GateType animationPath cannot be empty.");
        if (this.animOpen.isEmpty()) throw new IllegalArgumentException("GateType animOpen cannot be empty.");
        if (this.animClose.isEmpty()) throw new IllegalArgumentException("GateType animClose cannot be empty.");
        if (this.animOpenIdle.isEmpty()) throw new IllegalArgumentException("GateType animOpenIdle cannot be empty.");
        if (this.animIdle.isEmpty()) throw new IllegalArgumentException("GateType animIdle cannot be empty.");
        if (this.openSoundKey.isEmpty()) throw new IllegalArgumentException("GateType openSoundKey cannot be empty.");
        if (this.closeSoundKey.isEmpty()) throw new IllegalArgumentException("GateType closeSoundKey cannot be empty.");
        if (!(this.openSoundVolume > 0.0F)) throw new IllegalArgumentException("GateType openSoundVolume must be > 0.");
        if (!(this.closeSoundVolume > 0.0F)) throw new IllegalArgumentException("GateType closeSoundVolume must be > 0.");
    }

    /** Stable id (should match registry name such as "gate", "check_point_gate"). */
    public String id() {
        return id;
    }

    /** Texture path under mod assets, e.g. "textures/block/gate_core.png". */
    public String texturePath() {
        return texturePath;
    }

    public String modelPath() {
        return modelPath;
    }

    public String animationPath() {
        return animationPath;
    }

    public String animOpen() {
        return animOpen;
    }

    public String animClose() {
        return animClose;
    }

    public String animOpenIdle() {
        return animOpenIdle;
    }

    public String animIdle() {
        return animIdle;
    }

    public String openSoundKey() {
        return openSoundKey;
    }

    public String closeSoundKey() {
        return closeSoundKey;
    }

    public float openSoundVolume() {
        return openSoundVolume;
    }

    public float closeSoundVolume() {
        return closeSoundVolume;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GateType other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "GateType[" + id + "]";
    }
}
