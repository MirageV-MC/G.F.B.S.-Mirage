package org.mirage.gfbs.ccio.event;

import dan200.computercraft.api.peripheral.IComputerAccess;
import net.minecraft.server.level.ServerLevel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public final class CCIoEventManager {
    private static final CCIoEventManager INSTANCE = new CCIoEventManager();

    private final Set<String> registeredEvents = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<EventListener>> eventListeners = new ConcurrentHashMap<>();
    private final Map<Integer, Set<String>> computerSubscriptions = new ConcurrentHashMap<>();

    private static final String EVENT_PREFIX = "gfbs_event";

    private CCIoEventManager() {}

    public static CCIoEventManager getInstance() {
        return INSTANCE;
    }

    public void registerEvent(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Event ID cannot be null or blank");
        }
        registeredEvents.add(eventId);
    }

    public void unregisterEvent(String eventId) {
        registeredEvents.remove(eventId);
        eventListeners.remove(eventId);
    }

    public boolean isEventRegistered(String eventId) {
        return registeredEvents.contains(eventId);
    }

    public Set<String> getRegisteredEvents() {
        return Collections.unmodifiableSet(registeredEvents);
    }

    public void subscribe(String eventId, IComputerAccess computer) {
        if (!registeredEvents.contains(eventId)) {
            throw new IllegalArgumentException("Event not registered: " + eventId);
        }
        if (computer == null) {
            throw new IllegalArgumentException("Computer cannot be null");
        }

        eventListeners.computeIfAbsent(eventId, k -> new CopyOnWriteArraySet<>())
                .add(new EventListener(computer));

        int computerId = computer.getID();
        computerSubscriptions.computeIfAbsent(computerId, k -> ConcurrentHashMap.newKeySet())
                .add(eventId);
    }

    public void unsubscribe(String eventId, IComputerAccess computer) {
        if (eventId == null || computer == null) return;

        Set<EventListener> listeners = eventListeners.get(eventId);
        if (listeners != null) {
            listeners.removeIf(l -> l.matches(computer));
        }

        int computerId = computer.getID();
        Set<String> subs = computerSubscriptions.get(computerId);
        if (subs != null) {
            subs.remove(eventId);
        }
    }

    public void unsubscribeAll(IComputerAccess computer) {
        if (computer == null) return;

        int computerId = computer.getID();
        Set<String> subs = computerSubscriptions.remove(computerId);
        if (subs != null) {
            for (String eventId : subs) {
                Set<EventListener> listeners = eventListeners.get(eventId);
                if (listeners != null) {
                    listeners.removeIf(l -> l.matches(computer));
                }
            }
        }
    }

    public void triggerEvent(String eventId, Object... args) {
        triggerEvent(null, eventId, args);
    }

    public void triggerEvent(ServerLevel level, String eventId, Object... args) {
        if (!registeredEvents.contains(eventId)) {
            return;
        }

        Set<EventListener> listeners = eventListeners.get(eventId);
        if (listeners == null || listeners.isEmpty()) {
            return;
        }

        Object[] eventArgs = buildEventArgs(eventId, args);

        for (EventListener listener : listeners) {
            listener.computer.queueEvent(EVENT_PREFIX, eventArgs);
        }
    }

    public void triggerEventForComputer(IComputerAccess computer, String eventId, Object... args) {
        if (!registeredEvents.contains(eventId) || computer == null) {
            return;
        }

        Object[] eventArgs = buildEventArgs(eventId, args);
        computer.queueEvent(EVENT_PREFIX, eventArgs);
    }

    private Object[] buildEventArgs(String eventId, Object... args) {
        Object[] eventArgs;
        if (args == null || args.length == 0) {
            eventArgs = new Object[]{eventId};
        } else {
            eventArgs = new Object[1 + args.length];
            eventArgs[0] = eventId;
            System.arraycopy(args, 0, eventArgs, 1, args.length);
        }
        return eventArgs;
    }

    public Set<String> getComputerSubscriptions(int computerId) {
        Set<String> subs = computerSubscriptions.get(computerId);
        return subs != null ? Collections.unmodifiableSet(subs) : Collections.emptySet();
    }

    public int getListenerCount(String eventId) {
        Set<EventListener> listeners = eventListeners.get(eventId);
        return listeners != null ? listeners.size() : 0;
    }

    public void clearAll() {
        registeredEvents.clear();
        eventListeners.clear();
        computerSubscriptions.clear();
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("registeredEvents", registeredEvents.size());
        stats.put("totalListeners", eventListeners.values().stream().mapToInt(Set::size).sum());
        stats.put("subscribedComputers", computerSubscriptions.size());
        return stats;
    }

    private static final class EventListener {
        final IComputerAccess computer;
        final int computerId;

        EventListener(IComputerAccess computer) {
            this.computer = computer;
            this.computerId = computer.getID();
        }

        boolean matches(IComputerAccess other) {
            return other != null && this.computerId == other.getID();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EventListener that)) return false;
            return computerId == that.computerId;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(computerId);
        }
    }
}
