package io.github.raesleg.game.collision.listeners;

/**
 * Observer interface for traffic violation events
 */

public interface TrafficViolationListener {

    // Called when player enters a crosswalk while a pedestrian is crossing
    void onCrosswalkViolation();

    // Called when player directly hits a pedestrian
    void onPedestrianHit();

    // Called when player collides with an NPC vehicle
    void onTrafficCrash();

    // Called when player picks up a collectible
    default void onPickup() {
        // Default empty
    }
    
}
