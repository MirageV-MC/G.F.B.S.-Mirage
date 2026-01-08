package org.mirage.Utils;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.mirage.Mirage_gfbs;
import org.mirage.Objects.blocks.Control.Gate.GateServerManager;
import org.mirage.Objects.blocks.Control.Gate.GateType;
import org.mirage.Objects.blocks.classs.Gate.GateBlock;

import java.util.Optional;

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

public final class GateUtils {

    private GateUtils() {}

    public static Optional<Boolean> getGateOpenState(ServerLevel level, GateType type) {
        if (level == null) {
            return Optional.empty();
        }

        var gates = GateServerManager.getGatesInLevel(level, type);
        if (gates == null || gates.isEmpty()) {
            Mirage_gfbs.LOGGER.warn("GATE IS NOT FOUND.");
            return Optional.empty();
        }

        var pos = gates.get(0);
        if (pos == null) {
            Mirage_gfbs.LOGGER.warn("GATE IS NOT FOUND....");
            return Optional.empty();
        }

        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GateBlock)) {
            return Optional.empty();
        }

        return Optional.of(state.getValue(GateBlock.OPEN));
    }
}
