package org.mirage.gfbs.ccio.event;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.mirage.gfbs.ccio.api.CCIoApiRegistry;

import java.util.*;

public final class CCIoEventApis {

    private CCIoEventApis() {}

    public static void register() {
        CCIoApiRegistry.register("event.register", CCIoEventApis::registerEvent);
        CCIoApiRegistry.register("event.unregister", CCIoEventApis::unregisterEvent);
        CCIoApiRegistry.register("event.isRegistered", CCIoEventApis::isRegistered);
        CCIoApiRegistry.register("event.list", CCIoEventApis::listEvents);
        CCIoApiRegistry.register("event.subscribe", CCIoEventApis::subscribe);
        CCIoApiRegistry.register("event.unsubscribe", CCIoEventApis::unsubscribe);
        CCIoApiRegistry.register("event.unsubscribeAll", CCIoEventApis::unsubscribeAll);
        CCIoApiRegistry.register("event.getSubscriptions", CCIoEventApis::getSubscriptions);
        CCIoApiRegistry.register("event.getListenerCount", CCIoEventApis::getListenerCount);
        CCIoApiRegistry.register("event.stats", CCIoEventApis::getStats);
    }

    private static Object registerEvent(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        if (args == null || args.length < 1) {
            throw new LuaException("registerEvent expects: (eventId)");
        }
        if (!(args[0] instanceof String eventId)) {
            throw new LuaException("eventId must be a string");
        }

        CCIoEventManager manager = CCIoEventManager.getInstance();
        manager.registerEvent(eventId);

        return true;
    }

    private static Object unregisterEvent(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        if (args == null || args.length < 1) {
            throw new LuaException("unregisterEvent expects: (eventId)");
        }
        if (!(args[0] instanceof String eventId)) {
            throw new LuaException("eventId must be a string");
        }

        CCIoEventManager manager = CCIoEventManager.getInstance();
        manager.unregisterEvent(eventId);

        return true;
    }

    private static Object isRegistered(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        if (args == null || args.length < 1) {
            throw new LuaException("isRegistered expects: (eventId)");
        }
        if (!(args[0] instanceof String eventId)) {
            throw new LuaException("eventId must be a string");
        }

        return CCIoEventManager.getInstance().isEventRegistered(eventId);
    }

    private static Object listEvents(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        Set<String> events = CCIoEventManager.getInstance().getRegisteredEvents();
        return new ArrayList<>(events);
    }

    private static Object subscribe(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        if (args == null || args.length < 1) {
            throw new LuaException("subscribe expects: (eventId)");
        }
        if (!(args[0] instanceof String eventId)) {
            throw new LuaException("eventId must be a string");
        }

        CCIoEventManager manager = CCIoEventManager.getInstance();

        if (!manager.isEventRegistered(eventId)) {
            throw new LuaException("Event not registered: " + eventId);
        }

        manager.subscribe(eventId, computer);

        return true;
    }

    private static Object unsubscribe(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        if (args == null || args.length < 1) {
            throw new LuaException("unsubscribe expects: (eventId)");
        }
        if (!(args[0] instanceof String eventId)) {
            throw new LuaException("eventId must be a string");
        }

        CCIoEventManager.getInstance().unsubscribe(eventId, computer);

        return true;
    }

    private static Object unsubscribeAll(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        CCIoEventManager.getInstance().unsubscribeAll(computer);
        return true;
    }

    private static Object getSubscriptions(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        Set<String> subs = CCIoEventManager.getInstance().getComputerSubscriptions(computer.getID());
        return new ArrayList<>(subs);
    }

    private static Object getListenerCount(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        if (args == null || args.length < 1) {
            throw new LuaException("getListenerCount expects: (eventId)");
        }
        if (!(args[0] instanceof String eventId)) {
            throw new LuaException("eventId must be a string");
        }

        return CCIoEventManager.getInstance().getListenerCount(eventId);
    }

    private static Object getStats(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        return CCIoEventManager.getInstance().getStats();
    }
}
