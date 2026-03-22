package io.github.raesleg.game.event;

import io.github.raesleg.engine.event.GameEvent;

/** Published when the player collects a fuel/charge pickup. */
public final class PickupCollectedEvent implements GameEvent {
    private static final PickupCollectedEvent INSTANCE = new PickupCollectedEvent();

    private PickupCollectedEvent() {
    }

    public static PickupCollectedEvent instance() {
        return INSTANCE;
    }
}
