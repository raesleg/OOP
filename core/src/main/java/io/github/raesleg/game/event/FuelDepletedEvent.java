package io.github.raesleg.game.event;

import io.github.raesleg.engine.event.GameEvent;

/** Published when the player's fuel reaches zero. */
public final class FuelDepletedEvent implements GameEvent {
    private static final FuelDepletedEvent INSTANCE = new FuelDepletedEvent();

    private FuelDepletedEvent() {
    }

    public static FuelDepletedEvent instance() {
        return INSTANCE;
    }
}
