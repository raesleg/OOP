package io.github.raesleg.game.movement;

/**
 * Encapsulates the police-car chase algorithm.
 * <p>
 * Approach speed scales with aggression; maintaining high player speed
 * slows (or reverses) the chase. Horizontal position lerps toward the
 * player's lane.
 */
public class PoliceMovement {

    private static final float BASE_APPROACH_SPEED = 70f;
    private static final float AGGRESSION_BONUS = 130f;
    private static final float LANE_TRACK_SPEED = 4f;

    private float screenY;

    public PoliceMovement(float startY) {
        this.screenY = startY;
    }

    /**
     * Advances the chase vertically.
     *
     * @return updated screenY
     */
    public float advance(float dt, float playerSpeed, float maxSpeed, float aggression) {
        float approachSpeed = BASE_APPROACH_SPEED + aggression * AGGRESSION_BONUS;
        float speedRatio = (maxSpeed > 0) ? playerSpeed / maxSpeed : 0f;
        float speedFactor = 1.0f - speedRatio * 1.15f;
        speedFactor = Math.max(-0.3f, speedFactor);
        screenY += approachSpeed * speedFactor * dt;
        return screenY;
    }

    /**
     * Smoothly tracks the player's X lane.
     *
     * @return interpolated X position
     */
    public float lerpX(float currentX, float targetX, float dt) {
        return currentX + (targetX - currentX) * LANE_TRACK_SPEED * dt;
    }

    public float getScreenY() {
        return screenY;
    }

    public boolean hasCaught(float policeBottom, float policeHeight, float playerY) {
        return policeBottom + policeHeight >= playerY;
    }
}