package io.github.raesleg.game.collision.handlers;

import com.badlogic.gdx.math.Vector2;

import io.github.raesleg.engine.entity.Entity;
import io.github.raesleg.engine.entity.IFlashable;
import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.collision.CollisionEntityUtils;
import io.github.raesleg.game.collision.listeners.TrafficViolationListener;
import io.github.raesleg.game.entities.misc.Pedestrian;

/**
 * Handles player hitting pedestrians on crosswalk.
 */

public class PedestrianCollisionHandler {

    private final SoundDevice soundManager;
    private TrafficViolationListener violationListener;

    public PedestrianCollisionHandler(SoundDevice soundManager) {
        this.soundManager = soundManager;
    }

    // Wire listener for game-over penalty (DIP)
    public void setViolationListener(TrafficViolationListener listener) {
        this.violationListener = listener;
    }

    // Check if collision involves player and an alive pedestrian
    public boolean canHandle(Entity a, Entity b) {
        Pedestrian ped = CollisionEntityUtils.extractEntity(a, b, Pedestrian.class);
        MovableEntity player = CollisionEntityUtils.getPlayerEntity(a, b);
        return ped != null && player != null && !ped.isExpired();
    }

    // Trigger damage flash, calculate knockback, and notify game for instant fail
    public void handleBegin(Entity entityA, Entity entityB) {
        Pedestrian ped = CollisionEntityUtils.extractEntity(entityA, entityB, Pedestrian.class);
        MovableEntity player = CollisionEntityUtils.getPlayerEntity(entityA, entityB);

        if (ped == null || player == null || ped.isExpired())
            return;

        // Visual damage feedback on player vehicle
        if (player instanceof IFlashable flashable) {
            flashable.triggerDamageFlash();
        }

        // Calculate knockback direction (velocity-based or position-based fallback)
        PhysicsBody playerBody = player.getPhysicsBody();
        Vector2 playerVelocity = playerBody.getVelocity();

        Vector2 knockbackDir;
        if (playerVelocity.len2() > 0.01f) {
            // Pedestrian flies in player's direction of travel
            knockbackDir = playerVelocity.cpy().nor();
        } else {
            // Use position direction when player stationary (away from car)
            Vector2 pedPos = ped.getPhysicsBody().getPosition();
            Vector2 playerPos = playerBody.getPosition();
            knockbackDir = pedPos.cpy().sub(playerPos).nor();
        }

        // Scale knockback by player speed (faster impact = more knockback)
        float knockbackForce = Math.max(20f, playerVelocity.len() * 15f);

        // Audio impact feedback
        if (soundManager != null) {
            soundManager.playSound("explosion", 1.0f);
        }

        // Delegate reaction system to handle physics animation and instant fail (SRP)
        if (violationListener != null) {
            violationListener.onPedestrianHit(ped, knockbackDir, knockbackForce);
        }
    }
}