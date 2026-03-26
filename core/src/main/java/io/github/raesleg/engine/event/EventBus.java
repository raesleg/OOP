package io.github.raesleg.engine.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * EventBus — Lightweight publish/subscribe messaging system.
 * <p>
 * Allows decoupled systems to communicate without direct references.
 * A system publishes a {@link GameEvent}; all subscribers for that
 * event type are notified immediately (synchronous dispatch).
 * <p>
 * <b>Design pattern:</b> Observer (generalised) — decouples publishers
 * from subscribers at the engine level.
 * <p>
 * <b>Lifecycle:</b> One EventBus per scene. Disposed when the scene
 * shuts down to prevent stale references.
 */
public final class EventBus {

    private final Map<Class<? extends GameEvent>, List<Consumer<? extends GameEvent>>> listeners = new HashMap<>();

    /**
     * Subscribes a handler for a specific event type.
     *
     * @param <T>       the event type
     * @param eventType the class of the event to listen for
     * @param handler   the callback invoked when the event is published
     */
    public <T extends GameEvent> void subscribe(Class<T> eventType, Consumer<T> handler) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }

    /**
     * Publishes an event to all subscribers of its type.
     *
     * @param event the event instance to dispatch
     */
    @SuppressWarnings("unchecked")
    public <T extends GameEvent> void publish(T event) {
        List<Consumer<? extends GameEvent>> handlers = listeners.get(event.getClass());
        if (handlers != null) {
            for (Consumer<? extends GameEvent> handler : handlers) {
                ((Consumer<T>) handler).accept(event);
            }
        }
    }

    /** Removes all subscribers — call when the owning scene is disposed. */
    public void clear() {
        listeners.clear();
    }
}
