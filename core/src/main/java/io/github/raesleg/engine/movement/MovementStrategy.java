package io.github.raesleg.engine.movement;

public interface MovementStrategy {
    float getX(MovableEntity entity, float dt);
    float getY(MovableEntity entity, float dt);

    default void afterStep(MovableEntity entity, float dt) {
    }
}