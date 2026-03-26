package io.github.raesleg.game.collision;

import com.badlogic.gdx.math.Vector2;

import io.github.raesleg.engine.entity.Entity;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.physics.PhysicsBody;

/**
 * CollisionEntityUtils — Stateless utility methods shared by collision
 * handlers.
 * <p>
 * Extracted from {@link GameCollisionHandler} to satisfy SRP:
 * the coordinator should only coordinate, not provide utility logic.
 */
public final class CollisionEntityUtils {

    private CollisionEntityUtils() {
    }

    /**
     * Returns the user-controlled MovableEntity from a collision pair, or null.
     */
    public static MovableEntity getPlayerEntity(Entity a, Entity b) {
        if (a instanceof MovableEntity ma && !ma.isAIControlled()) {
            return ma;
        }
        if (b instanceof MovableEntity mb && !mb.isAIControlled()) {
            return mb;
        }
        return null;
    }

    /**
     * Extracts a specific entity type from a collision pair.
     */
    public static <T> T extractEntity(Entity a, Entity b, Class<T> type) {
        if (type.isInstance(a))
            return type.cast(a);
        if (type.isInstance(b))
            return type.cast(b);
        return null;
    }

    /**
     * Clamps velocity to prevent excessive speed.
     */
    public static void clampVelocity(PhysicsBody body, float maxSpeed) {
        Vector2 vel = body.getVelocity();
        float currentSpeed = vel.len();

        if (currentSpeed > maxSpeed) {
            vel.nor().scl(maxSpeed);
            body.setVelocity(vel.x, vel.y);
        }
    }
}
