package org.mirage.gfbs.Objects.blocks.Control.Gate;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for {@link GateType}.
 *
 * <p>Adding a new gate variant should only require:
 * <ol>
 *   <li>Register a {@link GateType} here (or via {@link #register(GateType)} at runtime)</li>
 *   <li>Register a {@code GateBlock} instance in {@code BlockRegistration}</li>
 * </ol>
 */
public final class GateTypes {
    private static final Map<String, GateType> REGISTRY = new ConcurrentHashMap<>();

    // Built-in types
    public static final GateType STANDARD = register(new GateType(
            "gate",
            "textures/block/gate_core.png"
    ));

    public static final GateType CHECK_POINT = register(new GateType(
            "check_point_gate",
            "textures/block/check_point_gate_core.png"
    ));

    private GateTypes() {}

    public static GateType register(GateType type) {
        // Keep first registration to avoid accidental override.
        REGISTRY.putIfAbsent(type.id(), type);
        return REGISTRY.get(type.id());
    }

    /** Returns registered type, or {@link #STANDARD} if not found. */
    public static GateType get(String id) {
        GateType t = REGISTRY.get(id);
        return t != null ? t : STANDARD;
    }

    public static Map<String, GateType> all() {
        return Collections.unmodifiableMap(REGISTRY);
    }
}
