package io.github.raesleg.game.collision.handlers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.Entity;
import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.collision.GameCollisionHandler;
import io.github.raesleg.game.collision.listeners.TrafficViolationListener;
import io.github.raesleg.game.entities.vehicles.NPCCar;

/**
 * Handles player crashing into NPC vehicles.
 */

public class NPCCarCollisionHandler {

    private final SoundDevice soundManager;
    private TrafficViolationListener violationListener;

    private static final float CRASH_KNOCKBACK_MULTIPLIER = 8f;
    private static final long CRASH_COOLDOWN_MS = 1500; // Debounce multiple impacts
    private long lastCrashTimeMs = 0;

    public NPCCarCollisionHandler(SoundDevice soundManager) {
        this.soundManager = soundManager;
    }

    // Wire listener for crash penalties (DIP)
    public void setViolationListener(TrafficViolationListener listener) {
        this.violationListener = listener;
    }

    // Check if collision involves player and NPC car
    public boolean canHandle(Entity a, Entity b) {
        return GameCollisionHandler.extractEntity(a, b, NPCCar.class) != null
                && GameCollisionHandler.getPlayerEntity(a, b) != null;
    }

    // Apply knockback physics, sound, and record crash penalty (with cooldown)
    public void handleImpact(Entity entityA, Entity entityB, float force, Vector2 impactPoint) {
        NPCCar npc = GameCollisionHandler.extractEntity(entityA, entityB, NPCCar.class);
        MovableEntity player = GameCollisionHandler.getPlayerEntity(entityA, entityB);

        if (npc == null || player == null) return;

        // Prevent spam penalties from rapid sequential collisions
        long now = TimeUtils.millis();
        boolean penaltyAllowed = (now - lastCrashTimeMs >= CRASH_COOLDOWN_MS);

        Gdx.app.log("NPCCarCollisionHandler", "Car crash detected (penalty: " + penaltyAllowed + ")");

        // Push player away from NPC based on velocity direction
        PhysicsBody playerBody = player.getPhysicsBody();
        Vector2 playerVelocity = playerBody.getVelocity();

        Vector2 knockbackDirection;
        if (playerVelocity.len2() < 0.01f) {
            // Use position-based direction when moving slowly
            Vector2 npcPosMeters = new Vector2(
                    (npc.getX() + npc.getW() / 2f) / Constants.PPM,
                    (npc.getY() + npc.getH() / 2f) / Constants.PPM);
            knockbackDirection = playerBody.getPosition().cpy().sub(npcPosMeters).nor();
        } else {
            // Reverse impact for moving vehicle (bounces backward)
            knockbackDirection = playerVelocity.cpy().scl(-1).nor();
        }

        // Calculate and apply impulse to player physics body
        float knockbackMagnitude = Math.max(force * CRASH_KNOCKBACK_MULTIPLIER, 15f);
        Vector2 knockbackImpulse = knockbackDirection.scl(knockbackMagnitude);
        playerBody.applyImpulseAtCenter(knockbackImpulse);

        // Prevent player from flying off screen due to excessive velocity
        GameCollisionHandler.clampVelocity(playerBody, 30f);

        // Play crash audio feedback
        if (soundManager != null) {
            soundManager.playSound("explosion", 0.5f);
        }

        // Notify game system to record penalty (debounced)
        if (penaltyAllowed && violationListener != null) {
            lastCrashTimeMs = now;
            violationListener.onTrafficCrash();
        }
    }
}