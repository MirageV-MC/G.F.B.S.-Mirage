package org.mirage.gfbs.Event.ccio.dmr;

import org.mirage.gfbs.ccio.event.CCIoEventManager;

public final class DmrMeltdownEvents {
    private DmrMeltdownEvents() {}

    public static final String MELTDOWN_START = "dmr_meltdown_start";
    public static final String IMPLOSION = "dmr_implosion";
    public static final String IMPLOSION_2 = "dmr_implosion_2";
    public static final String FLASH_LOOP_START = "dmr_flash_loop_start";
    public static final String REDCODE_ANNOUNCED = "dmr_redcode_announced";
    public static final String COUNTDOWN_START = "dmr_countdown_start";
    public static final String SHUTDOWN_WINDOW_OPEN = "dmr_shutdown_window_open";
    public static final String HEX_CRACKER_TRIGGERED = "dmr_hex_cracker_triggered";
    public static final String COUNTDOWN_END = "dmr_countdown_end";
    public static final String IMPLOSION_3 = "dmr_implosion_3";
    public static final String P2_START = "dmr_p2_start";
    public static final String SHELTER_GATE_OPENED = "dmr_shelter_gate_opened";
    public static final String LOCKDOWN_INITIATED = "dmr_lockdown_initiated";
    public static final String GRAVITY_SOURCE_DETECTED = "dmr_gravity_source_detected";
    public static final String MELTDOWN_END = "dmr_meltdown_end";
    public static final String EXPLOSION_START = "dmr_explosion_start";
    public static final String EXPLOSION_MAIN = "dmr_explosion_main";
    public static final String FACILITY_RESTORE = "dmr_facility_restore";
    public static final String SHUTDOWN_SUCCESS = "dmr_shutdown_success";
    public static final String SHUTDOWN_FAILURE = "dmr_shutdown_failure";

    private static boolean registered = false;

    public static void registerAll() {
        if (registered) return;
        registered = true;

        CCIoEventManager manager = CCIoEventManager.getInstance();
        manager.registerEvent(MELTDOWN_START);
        manager.registerEvent(IMPLOSION);
        manager.registerEvent(IMPLOSION_2);
        manager.registerEvent(FLASH_LOOP_START);
        manager.registerEvent(REDCODE_ANNOUNCED);
        manager.registerEvent(COUNTDOWN_START);
        manager.registerEvent(SHUTDOWN_WINDOW_OPEN);
        manager.registerEvent(HEX_CRACKER_TRIGGERED);
        manager.registerEvent(COUNTDOWN_END);
        manager.registerEvent(IMPLOSION_3);
        manager.registerEvent(P2_START);
        manager.registerEvent(SHELTER_GATE_OPENED);
        manager.registerEvent(LOCKDOWN_INITIATED);
        manager.registerEvent(GRAVITY_SOURCE_DETECTED);
        manager.registerEvent(MELTDOWN_END);
        manager.registerEvent(EXPLOSION_START);
        manager.registerEvent(EXPLOSION_MAIN);
        manager.registerEvent(FACILITY_RESTORE);
        manager.registerEvent(SHUTDOWN_SUCCESS);
        manager.registerEvent(SHUTDOWN_FAILURE);
    }

    public static void trigger(String eventId, Object... args) {
        CCIoEventManager.getInstance().triggerEvent(eventId, args);
    }

    public static void triggerWithLevel(String eventId, Object... args) {
        CCIoEventManager.getInstance().triggerEvent(eventId, args);
    }
}
