package io.github.raesleg.game.event;

import io.github.raesleg.engine.event.GameEvent;

/**
 * Published when a score change occurs (positive reward or negative penalty).
 */
public final class ScoreChangedEvent implements GameEvent {

    private final int delta;

    public ScoreChangedEvent(int delta) {
        this.delta = delta;
    }

    public int getDelta() {
        return delta;
    }
}
