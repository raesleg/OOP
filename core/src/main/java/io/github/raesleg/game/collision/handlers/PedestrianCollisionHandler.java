package io.github.raesleg.game.collision.handlers;

import com.badlogic.gdx.math.Vector2;

import io.github.raesleg.engine.entity.Entity;
import io.github.raesleg.engine.entity.IFlashable;
import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.collision.GameCollisionHandler;
import io.github.raesleg.game.collision.listeners.TrafficViolationListener;
import io.github.raesleg.game.entities.misc.Pedestrian;

/**
 * Handles player hitting pedestrians.
 */
public class PedestrianCollisionHandler {

    private final SoundDevice soundManager;
    private TrafficViolationListener violationListener;

    public PedestrianCollisionHandler(SoundDevice soundManager) {
        this.soundManager = soundManager;
    }

    public void setViolationListener(TrafficViolationListener listener) {
        this.violationListener = listener;
    }

    public boolean canHandle(Entity a, Entity b) {
        Pedestrian ped = GameCollisionHandler.extractEntity(a, b, Pedestrian.class);
        MovableEntity player = GameCollisionHandler.getPlayerEntity(a, b);
        return ped != null && player != null && !ped.isExpired();
    }

    public void handleBegin(Entity entityA, Entity entityB) {
        Pedestrian ped = GameCollisionHandler.extractEntity(entityA, entityB, Pedestrian.class);
        MovableEntity player = GameCollisionHandler.getPlayerEntity(entityA, entityB);

        if (ped == null || player == null || ped.isExpired()) return;

        // Trigger flash on player
        if (player instanceof IFlashable flashable) {
            flashable.triggerDamageFlash();
        }

        // Calculate knockback direction for pedestrian
        PhysicsBody playerBody = player.getPhysicsBody();
        Vector2 playerVelocity = playerBody.getVelocity();

        Vector2 knockbackDir;
        if (playerVelocity.len2() > 0.01f) {
            // Send pedestrian in player's movement direction
            knockbackDir = playerVelocity.cpy().nor();
        } else {
            // If player stationary, use position-based direction
            Vector2 pedPos = ped.getPhysicsBody().getPosition();
            Vector2 playerPos = playerBody.getPosition();
            knockbackDir = pedPos.cpy().sub(playerPos).nor();
        }

        // Pass knockback info to the scene-level reaction system.
        float knockbackForce = Math.max(20f, playerVelocity.len() * 15f);

        // Play impact sound
        if (soundManager != null) {
            soundManager.playSound("explosion", 1.0f);
        }

        // Notify observer (delayed game over)
        if (violationListener != null) {
            violationListener.onPedestrianHit(ped, knockbackDir, knockbackForce);
        }
    }
}