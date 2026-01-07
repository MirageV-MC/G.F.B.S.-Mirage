package org.mirage.api;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.mirage.Objects.blockEntity.Gate.GateBlockEntity;
import org.mirage.Objects.blocks.Control.Gate.GateServerManager;
import org.mirage.Objects.blocks.Control.Gate.GateType;
import org.mirage.Objects.blocks.Control.Gate.GateTypes;
import org.mirage.Objects.blocks.classs.Gate.GateBlock;
import org.mirage.Phenomenon.network.Network.ClientEventHandler;
import org.mirage.Phenomenon.network.Network.ClientToServer;
import org.mirage.Phenomenon.network.Network.NetworkHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified gate client/server API (covers both normal gate and checkpoint gate).
 *
 * <p>Important: Client events MUST NOT call "openAll()" directly, otherwise the client will
 * re-send packets back to server and create feedback loops. Events only apply local state.</p>
 */
public class GateClientAPI {

    /** Legacy: global state for STANDARD gate only (kept for compatibility). */
    public static boolean GLOBAL_GATE_STATE = false;

    private static final Map<String, Boolean> GLOBAL_STATE_BY_TYPE = new ConcurrentHashMap<>();

    private static final String C2S_SET_ALL = "mirage_gate_set_all";      // client -> server
    private static final String S2C_SET_ALL = "mirage_gate_set_all";      // server -> clients (event id)

    static {
        GLOBAL_STATE_BY_TYPE.put(GateTypes.STANDARD.id(), false);
        GLOBAL_STATE_BY_TYPE.put(GateTypes.CHECK_POINT.id(), false);
    }

    public static boolean getGlobalState(GateType type) {
        return GLOBAL_STATE_BY_TYPE.getOrDefault(type.id(), false);
    }

    private static void setGlobalState(GateType type, boolean open) {
        GLOBAL_STATE_BY_TYPE.put(type.id(), open);
        if (GateTypes.STANDARD.equals(type)) {
            GLOBAL_GATE_STATE = open;
        }
    }

    /** Client-local apply only (no network). Used by server broadcasts and by onLoad sync. */
    public static void applyClientState(GateType type, boolean open) {
        setGlobalState(type, open);
        for (GateBlockEntity gate : GateBlockEntity.getClientGates(type)) {
            gate.setLogicalOpen(open);
            gate.refreshAnimationState();
        }
    }

    // --------------------
    // Client request APIs
    // --------------------

    public static void openAll() {
        openAll(GateTypes.STANDARD);
    }

    public static void closeAll() {
        closeAll(GateTypes.STANDARD);
    }

    public static void openAll(GateType type) {
        // apply instantly on local client for responsiveness
        applyClientState(type, true);

        ClientToServer.runWithString(C2S_SET_ALL, encode(type, true), (player, data) -> {
            GateType parsedType = decodeType(data);
            boolean open = decodeOpen(data);

            Level level = player.level();
            List<BlockPos> gatePositions = GateServerManager.getGatesInLevel(level, parsedType);

            NetworkHandler.sendToAll("mirage_gate_busy");

            for (BlockPos pos : gatePositions) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof GateBlock gateBlock) {
                    gateBlock.applyOpenStateDirect(level, pos, open);
                }
            }

            broadcastToClients(parsedType, open);
        });
    }

    public static void closeAll(GateType type) {
        applyClientState(type, false);

        ClientToServer.runWithString(C2S_SET_ALL, encode(type, false), (player, data) -> {
            GateType parsedType = decodeType(data);
            boolean open = decodeOpen(data);

            Level level = player.level();
            List<BlockPos> gatePositions = GateServerManager.getGatesInLevel(level, parsedType);

            NetworkHandler.sendToAll("mirage_gate_busy");

            for (BlockPos pos : gatePositions) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof GateBlock gateBlock) {
                    gateBlock.applyOpenStateDirect(level, pos, open);
                }
            }

            broadcastToClients(parsedType, open);
        });
    }

    // --------------------
    // Server-side helpers
    // --------------------

    /** Server-side bulk apply (for commands / scripts). */
    public static void setAllServer(Level level, GateType type, boolean open) {
        List<BlockPos> gatePositions = GateServerManager.getGatesInLevel(level, type);

        for (BlockPos pos : gatePositions) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof GateBlock gateBlock) {
                gateBlock.applyOpenStateDirect(level, pos, open);
            }
        }

        broadcastToClients(type, open);
    }

    public static void openAllServer(Level level) {
        setAllServer(level, GateTypes.STANDARD, true);
    }

    public static void closeAllServer(Level level) {
        setAllServer(level, GateTypes.STANDARD, false);
    }

    // --------------------
    // Client sync / events
    // --------------------

    public static void applyStateToAllLoaded() {
        // legacy: standard only
        applyStateToAllLoaded(GateTypes.STANDARD);
    }

    public static void applyStateToAllLoaded(GateType type) {
        boolean open = getGlobalState(type);
        for (GateBlockEntity gate : GateBlockEntity.getClientGates(type)) {
            gate.setLogicalOpen(open);
            if (gate.getLevel() != null && gate.getLevel().isClientSide) {
                gate.refreshAnimationState();
            }
        }
    }

    public static void register() {
        // New unified event (with data)
        ClientEventHandler.registerEvent(S2C_SET_ALL, (data) -> {
            String typeId = data.getString("type");
            boolean open = data.getBoolean("open");
            applyClientState(GateTypes.get(typeId), open);
        });

        // Legacy events (no data) - apply client only (NO server packet)
        ClientEventHandler.registerEvent("open_all_gate", (data) -> applyClientState(GateTypes.STANDARD, true));
        ClientEventHandler.registerEvent("close_all_gate", (data) -> applyClientState(GateTypes.STANDARD, false));
        ClientEventHandler.registerEvent("open_all_pg_gate", (data) -> applyClientState(GateTypes.CHECK_POINT, true));
        ClientEventHandler.registerEvent("close_all_pg_gate", (data) -> applyClientState(GateTypes.CHECK_POINT, false));
    }

    private static void broadcastToClients(GateType type, boolean open) {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type.id());
        tag.putBoolean("open", open);
        NetworkHandler.sendToAll(S2C_SET_ALL, tag);

        // Optional legacy broadcasts for old ids (safe even if unused)
        if (GateTypes.STANDARD.equals(type)) {
            NetworkHandler.sendToAll(open ? "open_all_gate" : "close_all_gate");
        } else if (GateTypes.CHECK_POINT.equals(type)) {
            NetworkHandler.sendToAll(open ? "open_all_pg_gate" : "close_all_pg_gate");
        }
    }

    private static String encode(GateType type, boolean open) {
        return type.id() + "|" + (open ? "1" : "0");
    }

    private static GateType decodeType(String data) {
        if (data == null) return GateTypes.STANDARD;
        String[] parts = data.split("\\|", 2);
        if (parts.length == 0) return GateTypes.STANDARD;
        return GateTypes.get(parts[0]);
    }

    private static boolean decodeOpen(String data) {
        if (data == null) return false;
        String[] parts = data.split("\\|", 2);
        if (parts.length < 2) return false;
        return "1".equals(parts[1]) || "true".equalsIgnoreCase(parts[1]);
    }
}
