package io.github.raesleg.engine.movement;

import io.github.raesleg.engine.physics.PhysicsBody;

public interface MovementModel {
    void step(PhysicsBody body, float x, float y, float dt);

    // called when entity enters motion zone
    default void onEnterZone(PhysicsBody body, Object zoneTuning) {
        // override in zone-aware movement models
    }

    // called when entity exits motion zone
    default void onExitZone(PhysicsBody body) {
        // override in zone-aware movement models
    }
}
