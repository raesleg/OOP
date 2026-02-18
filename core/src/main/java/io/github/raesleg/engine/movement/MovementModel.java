package io.github.raesleg.engine.movement;

import io.github.raesleg.engine.physics.PhysicsBody;

public interface MovementModel {
    /**
     * Advances the movement simulation by one frame.
     * <p>
     * The parameter is typed as {@link IMovable} rather than the concrete
     * {@link MovableEntity} so this engine-level interface does not depend
     * on a specific implementation (Dependency Inversion Principle).
     */
    void step(IMovable e, float dt);

    /**
     * Called when the entity enters a motion zone.
     * Default no-op — override in zone-aware movement models.
     */
    default void onEnterZone(PhysicsBody body, Object zoneTuning) {
        // no-op by default
    }

    /**
     * Called when the entity exits a motion zone.
     * Default no-op — override in zone-aware movement models.
     */
    default void onExitZone(PhysicsBody body) {
        // no-op by default
    }
}
