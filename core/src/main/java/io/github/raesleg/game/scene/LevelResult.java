// Package: io.github.raesleg.game.scene
package io.github.raesleg.game.scene;

/**
 * LevelResult — Immutable value object carrying the outcome of a completed
 * or failed level (Template Method data transfer).
 * <p>
 * Passed to {@link ResultsScene} via constructor injection so the results
 * screen can display summary information without holding any reference to
 * the gameplay scene that produced it.
 * <p>
 * <b>Encapsulation:</b> All fields are final — no mutable state can leak
 * between scenes.
 * <b>Engine/Game boundary:</b> Lives entirely in the game layer.
 */
public final class LevelResult {

    private final int score;
    private final float time;
    private final int rulesBroken;
    private final String levelName;
    private final boolean completed;
    private final String lossReason;

    public LevelResult(int score, float time, int rulesBroken,
            String levelName, boolean completed, String lossReason) {
        this.score = score;
        this.time = time;
        this.rulesBroken = rulesBroken;
        this.levelName = levelName;
        this.completed = completed;
        this.lossReason = (lossReason != null) ? lossReason : "";
    }

    public int getScore() {
        return score;
    }

    public float getTime() {
        return time;
    }

    public int getRulesBroken() {
        return rulesBroken;
    }

    public String getLevelName() {
        return levelName;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getLossReason() {
        return lossReason;
    }
}
