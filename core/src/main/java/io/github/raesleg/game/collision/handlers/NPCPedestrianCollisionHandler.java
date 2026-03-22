package io.github.raesleg.game.collision.handlers;

import io.github.raesleg.engine.entity.Entity;
import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.game.collision.GameCollisionHandler;
import io.github.raesleg.game.entities.misc.Pedestrian;

/**
 * Handles the case where an AI-controlled vehicle hits a pedestrian.
 * The pedestrian is silently expired (no game-over penalty).
 */
public class NPCPedestrianCollisionHandler {

    private final SoundDevice soundManager;

    public NPCPedestrianCollisionHandler(SoundDevice soundManager) {
        this.soundManager = soundManager;
    }

    public boolean canHandle(Entity a, Entity b) {
        Pedestrian ped = GameCollisionHandler.extractEntity(a, b, Pedestrian.class);
        if (ped == null || ped.isExpired())
            return false;
        MovableEntity mover = GameCollisionHandler.extractEntity(a, b, MovableEntity.class);
        return mover != null && mover.isAIControlled();
    }

    public void handleBegin(Entity entityA, Entity entityB) {
        Pedestrian ped = GameCollisionHandler.extractEntity(entityA, entityB, Pedestrian.class);
        if (ped != null && !ped.isExpired()) {
            ped.markExpired();
            if (soundManager != null) {
                soundManager.playSound("scream", 1.0f);
            }
        }
    }
}
