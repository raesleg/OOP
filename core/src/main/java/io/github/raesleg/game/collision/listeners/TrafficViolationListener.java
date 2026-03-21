package io.github.raesleg.game.collision.listeners;

import com.badlogic.gdx.math.Vector2;

import io.github.raesleg.game.entities.Pedestrian;

/**
 * Observer interface for traffic violation events
 */

public interface TrafficViolationListener {

    // Called when player enters a crosswalk while a pedestrian is crossing
    void onCrosswalkViolation();

    // Called when player directly hits a pedestrian
    void onPedestrianHit(Pedestrian pedestrian, Vector2 knockbackDirection, float knockbackForce);

    // Called when player collides with an NPC vehicle
    void onTrafficCrash();

    // Called when player picks up a collectible
    default void onPickup() {
        // Default empty
    }
    
}
