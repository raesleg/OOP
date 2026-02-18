package io.github.raesleg.engine.movement;

import io.github.raesleg.engine.physics.PhysicsBody;

/**
 * Abstraction for any entity that can be moved by a {@link MovementModel}.
 * <p>
 * Declaring these methods here lets {@link MovementModel#step} depend only
 * on this interface rather than on the concrete {@link MovableEntity} class
 * (Dependency Inversion Principle).
 */
public interface IMovable {
    void move(float deltaTime);

    /** Returns the physics body that the movement model drives. */
    PhysicsBody getPhysicsBody();

    /** Returns the horizontal axis input value for this frame. */
    float getInputX(float dt);

    /** Returns the vertical axis input value for this frame. */
    float getInputY(float dt);
}
