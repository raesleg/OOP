package io.github.raesleg.game.movement;

import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.movement.MovementStrategy;

/**
 * Converts player input into normalized steering/throttle intent.
 */
public class PlayerMovementStrategy implements MovementStrategy {

    private final float steeringSensitivity;
    private final float throttleSensitivity;

    public PlayerMovementStrategy() {
        this(1.0f, 1.0f);
    }

    public PlayerMovementStrategy(
            float steeringSensitivity,
            float throttleSensitivity) {
        this.steeringSensitivity = steeringSensitivity;
        this.throttleSensitivity = throttleSensitivity;
    }

    @Override
    public float getX(MovableEntity entity, float dt) {
        float steer = entity.getInputX(dt) * steeringSensitivity;
        return clamp(steer, -1f, 1f);
    }

    @Override
    public float getY(MovableEntity entity, float dt) {
        float drive = entity.getInputY(dt);

        if (drive > 0f) {
            drive *= throttleSensitivity;
        } else if (drive < 0f) {
            drive *= throttleSensitivity; // Assuming reverse sensitivity is the same as throttle sensitivity
        }

        return clamp(drive, -1f, 1f);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
