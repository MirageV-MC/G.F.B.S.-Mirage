package org.mirage.gfbs.ccio.blockentity;

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

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.mirage.gfbs.ccio.CCIoRegistry;
import org.mirage.gfbs.ccio.peripheral.CCIoBridgePeripheral;

public class CCIoBridgeBlockEntity extends BlockEntity {
    private CCIoBridgePeripheral peripheral;

    public CCIoBridgeBlockEntity(BlockPos pos, BlockState state) {
        super(CCIoRegistry.CC_IO_BRIDGE_BE.get(), pos, state);
    }

    public CCIoBridgePeripheral getOrCreatePeripheral() {
        if (peripheral == null) peripheral = new CCIoBridgePeripheral(this);
        return peripheral;
    }

    public void invalidatePeripheral() {
        if (peripheral != null) {
            peripheral.invalidate();
            peripheral = null;
        }
    }
}
