package io.github.raesleg.game.collision.listeners;

/**
 * PickupListener — Observer for collectible pickup events.
 * <p>
 * Extracted from TrafficViolationListener to satisfy ISP:
 * pickup collection is a reward event, not a traffic violation.
 * Clients that only care about pickups should not depend on
 * violation methods they never use.
 */
public interface PickupListener {

    /** Called when the player picks up a collectible. */
    void onPickup();
}
