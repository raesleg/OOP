package io.github.raesleg.game.collision.listeners;

import com.badlogic.gdx.math.Vector2;

import io.github.raesleg.game.entities.misc.Pedestrian;

/**
 * Observer interface for traffic violation events
 */

public interface TrafficViolationListener {

    // Called when player enters a crosswalk while a pedestrian is crossing
    default void onCrosswalkViolation() {
    }

    // Called when player directly hits a pedestrian
    default void onPedestrianHit(Pedestrian pedestrian, Vector2 knockbackDirection, float knockbackForce) {
    }

    // Called when player collides with an NPC vehicle
    default void onTrafficCrash() {
    }

    // Called when player picks up a collectible
    default void onPickup() {
    }

}
