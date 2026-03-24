package io.github.raesleg.game.collision.handlers;

import io.github.raesleg.engine.entity.Entity;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.game.collision.GameCollisionHandler;
import io.github.raesleg.game.collision.listeners.PickupListener;
import io.github.raesleg.game.entities.misc.Pickupable;

/**
 * Handles collectible pickup.
 */

public class PickupCollisionHandler {

    private PickupListener pickupListener;

    public void setPickupListener(PickupListener listener) {
        this.pickupListener = listener;
    }
    // Check if collision involves player and collectible
    public boolean canHandle(Entity a, Entity b) {
        Pickupable pickup = GameCollisionHandler.extractEntity(a, b, Pickupable.class);
        MovableEntity player = GameCollisionHandler.getPlayerEntity(a, b);
        return pickup != null && player != null && !pickup.isExpired();
    }
    // Trigger pickup process
    public void handleBegin(Entity entityA, Entity entityB) {
        Pickupable pickup = GameCollisionHandler.extractEntity(entityA, entityB, Pickupable.class);
        MovableEntity player = GameCollisionHandler.getPlayerEntity(entityA, entityB);

        if (pickup != null && player != null && !pickup.isExpired()) {
            pickup.markExpired();

            if (pickupListener != null) {
                pickupListener.onPickup();
            }
        }
    }
}