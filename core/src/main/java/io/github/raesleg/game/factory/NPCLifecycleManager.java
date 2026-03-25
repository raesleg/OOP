package io.github.raesleg.game.factory;

import java.util.List;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.entities.vehicles.NPCCar;

/**
 * NPCLifecycleManager — Manages per-frame NPC movement and expiry.
 * <p>
 * Extracted from {@link NPCCar#updateLifeCycle} and
 * {@link NPCCarSpawner#update}
 * to satisfy SRP: the entity should not own its own lifecycle state machine,
 * and the spawner should not drive per-frame movement.
 * <p>
 * Uses {@code PhysicsBody.setVelocity()} instead of {@code setPosition()} to
 * comply with the engine's physics-facade contract.
 */
public class NPCLifecycleManager {

    private static final float MAX_LIFETIME = 10f;
    private static final float PREVIEW_DURATION = 1.5f;

    /**
     * Updates all active NPCs: moves them via velocity, transitions from
     * preview to active phase, and marks expired NPCs.
     *
     * @param activeNPCs            mutable list — expired NPCs are removed
     * @param scrollPixelsPerSecond current road scroll speed
     * @param deltaTime             frame delta
     * @param screenHeight          screen height for visibility checks
     */
    public void update(List<NPCCar> activeNPCs, float scrollPixelsPerSecond,
            float deltaTime, float screenHeight) {
        activeNPCs.removeIf(npc -> {
            if (npc.isExpired())
                return true;
            updateSingle(npc, scrollPixelsPerSecond, deltaTime, screenHeight);
            return npc.isExpired();
        });
    }

    private void updateSingle(NPCCar npc, float scrollPixelsPerSecond,
            float deltaTime, float screenHeight) {
        PhysicsBody body = npc.getPhysicsBody();
        if (body == null || body.isDestroyed()) {
            npc.markExpired();
            return;
        }

        npc.tickLifeTimer(deltaTime);

        if (npc.isInPreview()) {
            // Preview phase: NPC sits stationary so the player can see which lane it
            // occupies
            body.setVelocity(0f, 0f);

            if (npc.getLifeTimer() >= PREVIEW_DURATION) {
                npc.exitPreview();
            }
        } else {
            // Active phase: drive upward at combined scroll + approach speed
            float totalSpeed = scrollPixelsPerSecond + npc.getApproachSpeed();
            body.setVelocity(0f, totalSpeed / Constants.PPM);

            // Expire when off-screen (top or bottom) or lifetime exceeded
            if (npc.getY() > screenHeight + npc.getH()
                    || npc.getY() < -npc.getH() * 2f
                    || npc.getLifeTimer() > MAX_LIFETIME) {
                npc.markExpired();
            }
        }
    }
}
