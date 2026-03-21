package io.github.raesleg.game.movement;

/**
 * Holds the intended horizontal crossing direction for a pedestrian.
 * This is not the entity and not the actual movement execution.
 */
public class PedestrianIntent {

    private final float direction; // -1 = right to left, +1 = left to right

    public PedestrianIntent(float direction) {
        this.direction = direction < 0f ? -1f : 1f;
    }

    public float getDirection() {
        return direction;
    }
}