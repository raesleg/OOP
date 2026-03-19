package io.github.raesleg.engine.movement;

// added
public interface MovementStrategy {
    float getX(MovableEntity entity, float dt);
    float getY(MovableEntity entity, float dt);
}