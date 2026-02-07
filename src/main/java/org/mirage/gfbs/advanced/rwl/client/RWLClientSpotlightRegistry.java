package org.mirage.gfbs.advanced.rwl.client;

import org.mirage.gfbs.advanced.rwl.RotatingWarningLightBlockEntity;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端维护所有已加载的 RWL 方块实体（避免去翻 Level 内部私有列表）。
 */
public final class RWLClientSpotlightRegistry {

    private RWLClientSpotlightRegistry() {}

    private static final Set<RotatingWarningLightBlockEntity> LOADED = ConcurrentHashMap.newKeySet();

    public static void onLoad(RotatingWarningLightBlockEntity be) {
        LOADED.add(be);
    }

    public static void onRemove(RotatingWarningLightBlockEntity be) {
        LOADED.remove(be);
    }

    public static Set<RotatingWarningLightBlockEntity> getLoaded() {
        return LOADED;
    }
}
