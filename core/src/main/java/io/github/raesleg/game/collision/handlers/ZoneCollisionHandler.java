package io.github.raesleg.game.collision.handlers;

import io.github.raesleg.engine.entity.Entity;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.movement.MovementModel;
import io.github.raesleg.game.zone.CrosswalkZone;
import io.github.raesleg.game.zone.MotionZone;

/**
 * Handles motion zone entry/exit (friction, ice, etc.)
 */
public class ZoneCollisionHandler {

    /** Helper record to normalize entity pairs (DRY). */
    private record ZoneCollision(MovableEntity movable, Object tuning) {
    }

    public boolean canHandle(Entity a, Entity b) {
        return extractZoneCollision(a, b) != null;
    }

    public void handleBegin(Entity entityA, Entity entityB) {
        ZoneCollision zc = extractZoneCollision(entityA, entityB);
        if (zc != null) {
            MovementModel model = zc.movable().getMovementModel();
            model.onEnterZone(zc.movable().getPhysicsBody(), zc.tuning());
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
        // Only apply surface effects to player-controlled vehicles, not NPCs
        if (a instanceof MovableEntity m && !m.isAIControlled() && b instanceof MotionZone z) {
            return new ZoneCollision(m, z.getTuning());
        }
        if (b instanceof MovableEntity m && !m.isAIControlled() && a instanceof MotionZone z) {
            return new ZoneCollision(m, z.getTuning());
        }

        if (a instanceof MovableEntity m && !m.isAIControlled() && b instanceof CrosswalkZone z) {
            return new ZoneCollision(m, z.getSurfaceEffect());
        }
        if (b instanceof MovableEntity m && !m.isAIControlled() && a instanceof CrosswalkZone z) {
            return new ZoneCollision(m, z.getSurfaceEffect());
        }

        return null;
    }
}