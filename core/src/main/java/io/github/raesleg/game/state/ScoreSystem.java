package io.github.raesleg.game.state;

import io.github.raesleg.game.GameConstants;

/**
 * ScoreSystem — Encapsulates passive score accumulation and bonus tracking.
 * <p>
 * Extracted from {@link io.github.raesleg.game.scene.BaseGameScene} to satisfy
 * SRP: the scene orchestrates gameplay; this class owns score arithmetic.
 */
public class ScoreSystem {

    private float scoreAccumulator;
    private int scoreBonus;

    public ScoreSystem() {
        this.scoreAccumulator = 0f;
        this.scoreBonus = 0;
    }

    /**
     * Accumulates passive score based on driving time.
     * Should be called every frame when the player is moving.
     *
     * @param deltaTime frame delta in seconds
     * @param isMoving  true if simulated speed > threshold
     */
    public void update(float deltaTime, boolean isMoving) {
        if (isMoving) {
            scoreAccumulator += deltaTime * GameConstants.SCORE_RATE_PER_SECOND;
        }
    }

    /** Adds (or subtracts) a one-time bonus to the score. */
    public void addBonus(int delta) {
        scoreBonus += delta;
    }

    /** Returns the total score (accumulated + bonus). */
    public int getScore() {
        return (int) scoreAccumulator + scoreBonus;
    }
}
