package io.github.raesleg.engine.event;

/**
 * GameEvent — Marker interface for all events dispatched through the
 * {@link EventBus}.
 * <p>
 * Concrete events are defined in the <b>game</b> layer (e.g.
 * {@code CrosswalkViolationEvent}, {@code PickupCollectedEvent}).
 * The engine only knows this marker, keeping the engine free of
 * game-specific dependencies (Dependency Inversion Principle).
 */
public interface GameEvent {
}
