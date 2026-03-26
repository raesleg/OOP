package io.github.raesleg.game.collision.listeners;

/**
 * PickupListener — Observer for collectible pickup events.
 */

public interface PickupListener {

    // Called when the player picks up a collectible
    void onPickup();
}
