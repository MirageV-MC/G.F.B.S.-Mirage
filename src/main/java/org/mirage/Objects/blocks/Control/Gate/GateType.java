package org.mirage.Objects.blocks.Control.Gate;

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

    public GateType(String id, String texturePath) {
        this.id = Objects.requireNonNull(id, "id");
        this.texturePath = Objects.requireNonNull(texturePath, "texturePath");
        if (this.id.isEmpty()) {
            throw new IllegalArgumentException("GateType id cannot be empty.");
        }
        if (this.texturePath.isEmpty()) {
            throw new IllegalArgumentException("GateType texturePath cannot be empty.");
        }
    }

    /** Stable id (should match registry name such as "gate", "check_point_gate"). */
    public String id() {
        return id;
    }

    /** Texture path under mod assets, e.g. "textures/block/gate_core.png". */
    public String texturePath() {
        return texturePath;
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
