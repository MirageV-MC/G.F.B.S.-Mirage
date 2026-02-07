package org.mirage.gfbs.advanced.rwl;

import net.minecraft.server.level.ServerLevel;

import java.util.*;

public final class RWLServerRegistry {

    private static final Map<ServerLevel, Set<RotatingWarningLightBlockEntity>> LOADED =
            Collections.synchronizedMap(new WeakHashMap<>());

    private RWLServerRegistry() {}

    public static void onLoad(RotatingWarningLightBlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel sl)) return;
        LOADED.computeIfAbsent(sl, k -> Collections.newSetFromMap(new IdentityHashMap<>())).add(be);
    }

    public static void onRemove(RotatingWarningLightBlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel sl)) return;
        Set<RotatingWarningLightBlockEntity> set = LOADED.get(sl);
        if (set != null) {
            set.remove(be);
            if (set.isEmpty()) LOADED.remove(sl);
        }
    }

    public static void applyLevelState(ServerLevel level, RWLLevelState st, boolean updateBlockStatePowered) {
        Set<RotatingWarningLightBlockEntity> set = LOADED.get(level);
        if (set == null || set.isEmpty()) return;

        List<RotatingWarningLightBlockEntity> snapshot;
        synchronized (LOADED) {
            snapshot = new ArrayList<>(set);
        }

        for (RotatingWarningLightBlockEntity be : snapshot) {
            if (be == null || be.isRemoved() || be.getLevel() != level) continue;
            st.applyTo(level, be, updateBlockStatePowered);
        }
    }
}
