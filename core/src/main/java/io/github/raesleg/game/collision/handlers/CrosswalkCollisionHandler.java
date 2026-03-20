package io.github.raesleg.game.collision.handlers;

import io.github.raesleg.engine.entity.Entity;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.game.collision.GameCollisionHandler;
import io.github.raesleg.game.collision.listeners.TrafficViolationListener;
import io.github.raesleg.game.zone.CrosswalkZone;

/**
 * Handles crosswalk zone detection.
 */
public class CrosswalkCollisionHandler {

    private TrafficViolationListener violationListener;

    public void setViolationListener(TrafficViolationListener listener) {
        this.violationListener = listener;
    }

    public boolean canHandle(Entity a, Entity b) {
        return GameCollisionHandler.extractEntity(a, b, CrosswalkZone.class) != null
                && GameCollisionHandler.getPlayerEntity(a, b) != null;
    }

    public void handleBegin(Entity entityA, Entity entityB) {
        CrosswalkZone zone = GameCollisionHandler.extractEntity(entityA, entityB, CrosswalkZone.class);
        MovableEntity player = GameCollisionHandler.getPlayerEntity(entityA, entityB);

        if (zone != null && player != null) {
            zone.setPlayerInside(true);

            // Fire violation if pedestrian is crossing (TBC if want to implement)
            if (zone.isPedestrianCrossing() && zone.tryFireViolation() && violationListener != null) {
                // violationListener.onCrosswalkViolation();
            }
        }
    }

    public void handleEnd(Entity entityA, Entity entityB) {
        CrosswalkZone zone = GameCollisionHandler.extractEntity(entityA, entityB, CrosswalkZone.class);
        MovableEntity player = GameCollisionHandler.getPlayerEntity(entityA, entityB);

        if (zone != null && player != null) {
            zone.setPlayerInside(false);
        }
    }
}