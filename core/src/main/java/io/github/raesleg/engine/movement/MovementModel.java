package io.github.raesleg.engine.movement;

public interface MovementModel {
    void step(MovableEntity e, float dt);
}
