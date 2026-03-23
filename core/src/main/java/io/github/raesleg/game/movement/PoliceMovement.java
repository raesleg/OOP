package io.github.raesleg.game.movement;

import io.github.raesleg.game.GameConstants;

/**
 * Encapsulates the police-car chase algorithm.
 * <p>
 * The police car's vertical position is determined by the player's
 * current wanted-star count: more stars = closer to the player.
 * At {@link GameConstants#MAX_WANTED_STARS} the police is on top of
 * the player (caught). Horizontal position lerps toward the player's lane.
 */
public class PoliceMovement {

    private float screenY;

    public PoliceMovement(float startY) {
        this.screenY = startY;
    }

    /**
     * Advances the chase vertically based on star count and player speed.
     * Target Y = playerY - maxDistance * (1 - stars/maxStars) * speedModifier.
     * When the player is slow, the police closes the gap faster.
     *
     * @param playerSpeed current scroll speed (px/s or km/h)
     * @param maxSpeed    maximum possible speed for normalisation
     * @return updated screenY
     */
    public float advance(float dt, float playerY, int starCount, int maxStars,
            float playerSpeed, float maxSpeed) {
        float starRatio = (maxStars > 0) ? (float) starCount / maxStars : 0f;

        // speedRatio: 1 at max speed, 0 when stopped
        float speedRatio = (maxSpeed > 0) ? Math.min(1f, Math.max(0f, playerSpeed / maxSpeed)) : 0f;
        // When slow, reduce effective distance (police gets closer)
        float speedModifier = GameConstants.POLICE_SPEED_FACTOR
                + (1f - GameConstants.POLICE_SPEED_FACTOR) * speedRatio;

        float effectiveDistance = GameConstants.POLICE_STAR_MAX_DISTANCE
                * (1f - starRatio) * speedModifier;
        float targetY = playerY - effectiveDistance;

        screenY += (targetY - screenY) * GameConstants.POLICE_LERP_SPEED * dt;
        return screenY;
    }

    /**
     * Smoothly tracks the player's X lane.
     *
     * @return interpolated X position
     */
    public float lerpX(float currentX, float targetX, float dt) {
        return currentX + (targetX - currentX) * GameConstants.POLICE_LANE_TRACK_SPEED * dt;
    }

    public float getScreenY() {
        return screenY;
    }

    public boolean hasCaught(float policeBottom, float policeHeight, float playerY) {
        return policeBottom + policeHeight >= playerY;
    }
}