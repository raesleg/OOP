package io.github.raesleg.game.movement;

import io.github.raesleg.game.GameConstants;

// Movement logic for the police car during a chase sequence
// Vertical position is determined by the player's wanted-star count and speed
// Horizontal position tracks the player's lane
public class PoliceMovement {

    private float screenY;

    public PoliceMovement(float startY) {
        this.screenY = startY;
    }

    // Advances the chase vertically based on star count and player speed
    public float advance(float dt, float playerY, int starCount, int maxStars, float playerSpeed, float maxSpeed) {
        float targetY = computeTargetY(playerY, starCount, maxStars, playerSpeed, maxSpeed);
        screenY += (targetY - screenY) * GameConstants.POLICE_LERP_SPEED * dt;
        return screenY;
    }

    public float computeTargetY(float playerY, int starCount, int maxStars, float playerSpeed, float maxSpeed) {
        float starRatio = maxStars > 0 ? (float) starCount / maxStars : 0f;
        float speedRatio = maxSpeed > 0f ? clamp(playerSpeed / maxSpeed, 0f, 1f) : 0f;
        // When slow, reduce effective distance (police gets closer)
        float speedModifier = GameConstants.POLICE_SPEED_FACTOR
                + (1f - GameConstants.POLICE_SPEED_FACTOR) * speedRatio;
        float effectiveDistance = GameConstants.POLICE_STAR_MAX_DISTANCE
                * (1f - starRatio)
                * speedModifier;
        return playerY - effectiveDistance;
    }

    public float lerpX(float currentX, float targetX, float dt) {
        return currentX + (targetX - currentX) * GameConstants.POLICE_LANE_TRACK_SPEED * dt;
    }

    public float getScreenY() {
        return screenY;
    }

    public boolean hasCaught(float policeBottom, float policeHeight, float playerY) {
        return policeBottom + policeHeight >= playerY;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}