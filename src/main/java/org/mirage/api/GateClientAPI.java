package org.mirage.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.mirage.Objects.blockEntity.GateBlockEntity;
import org.mirage.Objects.blocks.Control.GateServerManager;
import org.mirage.Objects.blocks.classs.GateBlock;
import org.mirage.Phenomenon.network.Network.ClientEventHandler;
import org.mirage.Phenomenon.network.Network.ClientToServer;
import org.mirage.Phenomenon.network.Network.NetworkHandler;

import java.util.List;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Convex89524
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


public class GateClientAPI {
    public static boolean GLOBAL_GATE_STATE = false;

    public static void openAll() {
        GLOBAL_GATE_STATE = true;

        for (GateBlockEntity gate : GateBlockEntity.getClientGates()) {
            gate.setLogicalOpen(true);
        }

        ClientToServer.run("open_all_gates", (player) -> {
            Level level = player.level();
            List<BlockPos> gatePositions = GateServerManager.getGatesInLevel(level);

            NetworkHandler.sendToAll("mirage_gate_busy");

            for (BlockPos pos : gatePositions) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof GateBlock gateBlock) {
                    gateBlock.applyOpenStateDirect(level, pos, true);
                }
            }
        });
    }

    public static void closeAll() {
        GLOBAL_GATE_STATE = false;

        for (GateBlockEntity gate : GateBlockEntity.getClientGates()) {
            gate.setLogicalOpen(false);
        }

        ClientToServer.run("close_all_gates", (player) -> {
            Level level = player.level();
            List<BlockPos> gatePositions = GateServerManager.getGatesInLevel(level);

            NetworkHandler.sendToAll("mirage_gate_busy");

            for (BlockPos pos : gatePositions) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof GateBlock gateBlock) {
                    gateBlock.applyOpenStateDirect(level, pos, false);
                }
            }
        });
    }

    public static void applyStateToAllLoaded() {
        for (GateBlockEntity gate : GateBlockEntity.getClientGates()) {
            gate.setLogicalOpen(GLOBAL_GATE_STATE);
        }
    }

    public static void register(){
        ClientEventHandler.registerEvent("open_all_gate", (data)->{
            openAll();
        });
        ClientEventHandler.registerEvent("close_all_gate", (data)->{
            closeAll();
        });
    }
}
