package io.github.raesleg.game.event;

import io.github.raesleg.engine.event.GameEvent;

/** Published when a level-ending instant failure occurs. */
public final class InstantFailEvent implements GameEvent {

    private final String reason;

    public InstantFailEvent(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
