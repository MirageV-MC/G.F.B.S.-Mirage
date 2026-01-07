package org.mirage.Tools.CountdownPopup;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

public final class CountdownEndHooks {

    private static final List<Hook> HOOKS = new CopyOnWriteArrayList<>();
    private static final Set<UUID> FIRED = ConcurrentHashMap.newKeySet();

    @FunctionalInterface
    public interface Hook {
        void run(ServerPlayer player);
    }

    private CountdownEndHooks() {}

    public static void register(Hook hook) {
        if (hook != null) HOOKS.add(hook);
    }

    public static void fire(ServerPlayer player) {
        if (player == null) return;

        UUID id = player.getUUID();
        if (!FIRED.add(id)) return; // already fired for this player

        for (Hook hook : HOOKS) {
            try {
                hook.run(player);
            } catch (Throwable ignored) {}
        }
    }

    public static void reset(ServerPlayer player) {
        if (player != null) FIRED.remove(player.getUUID());
    }

    public static void resetAll() {
        FIRED.clear();
    }

    public static void unregister(Hook hook) {
        if (hook != null) HOOKS.remove(hook);
    }

    public static void unregisterAll() {
        HOOKS.clear();
    }
}
