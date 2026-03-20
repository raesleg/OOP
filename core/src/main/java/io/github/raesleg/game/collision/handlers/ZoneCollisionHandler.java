package io.github.raesleg.game.collision.handlers;

import io.github.raesleg.engine.entity.Entity;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.movement.MovementModel;
import io.github.raesleg.game.zone.MotionZone;

/**
 * Handles motion zone entry/exit (friction, ice, etc.)
 */
public class ZoneCollisionHandler {

    /** Helper record to normalize entity pairs (DRY). */
    private record ZoneCollision(MovableEntity movable, MotionZone zone) {
    }

    public boolean canHandle(Entity a, Entity b) {
        return extractZoneCollision(a, b) != null;
    }

    public void handleBegin(Entity entityA, Entity entityB) {
        ZoneCollision zc = extractZoneCollision(entityA, entityB);
        if (zc != null) {
            MovementModel model = zc.movable().getMovementModel();
            model.onEnterZone(zc.movable().getPhysicsBody(), zc.zone().getTuning());
        }
    }

    public void handleEnd(Entity entityA, Entity entityB) {
        ZoneCollision zc = extractZoneCollision(entityA, entityB);
        if (zc != null) {
            MovementModel model = zc.movable().getMovementModel();
            model.onExitZone(zc.movable().getPhysicsBody());
        }
    }

    private ZoneCollision extractZoneCollision(Entity a, Entity b) {
        if (a instanceof MovableEntity m && b instanceof MotionZone z)
            return new ZoneCollision(m, z);
        if (b instanceof MovableEntity m && a instanceof MotionZone z)
            return new ZoneCollision(m, z);
        return null;
    }
}