package io.github.raesleg.game.movement;

/**
 * PoliceMovement — screen-space chase logic for the police car.
 *
 * Does NOT implement MovementModel because police movement is
 * screen-space, not physics-space. MovementManager must not touch it.
 *
 * Net screen Y movement per frame = approachSpeed - scrollSpeed
 *   0 violations + fast player  → police falls behind (negative net, capped at MIN)
 *   more violations             → approachSpeed climbs → gap closes
 *   player stopped              → scrollSpeed = 0 → closes at full approachSpeed
*/
public class PoliceMovement {

    private static final float BASE_APPROACH_SPEED = 5f;
    private static final float SPEED_PER_VIOLATION = 5f;
    private static final float MIN_APPROACH_SPEED  = 5f;

    public float update(float dt, float playerX, float currentScreenY,
            float rulesBroken, float scrollSpeed) {

        float approachSpeed = BASE_APPROACH_SPEED + rulesBroken * SPEED_PER_VIOLATION;

        // Net = police gains on player this much per second
        // Clamped so police always creeps forward even at max player speed
        float netYSpeed = Math.max(MIN_APPROACH_SPEED, approachSpeed - scrollSpeed);

        return currentScreenY + netYSpeed * dt;
    }
}