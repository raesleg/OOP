package io.github.raesleg.game.movement;

import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.movement.MovementStrategy;

public class PlayerMovement implements MovementStrategy {

    private final float steeringSensitivity;
    private final float throttleSensitivity;
    private final float reverseSensitivity;

    public PlayerMovement() {
        this(1.0f, 1.0f, 0.65f);
    }

    public PlayerMovement(float steeringSensitivity,
                          float throttleSensitivity,
                          float reverseSensitivity) {
        this.steeringSensitivity = steeringSensitivity;
        this.throttleSensitivity = throttleSensitivity;
        this.reverseSensitivity = reverseSensitivity;
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
            drive *= reverseSensitivity;
        }

        return clamp(drive, -1f, 1f);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}