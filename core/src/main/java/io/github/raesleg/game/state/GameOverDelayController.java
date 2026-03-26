package io.github.raesleg.game.state;

import io.github.raesleg.game.scene.LevelResult;

/**
 * GameOverDelayController — Manages the timed delay between an explosion
 * event and the transition to the results screen.
 * <p>
 * Extracted from {@link io.github.raesleg.game.scene.BaseGameScene} to satisfy
 * SRP: the scene should not track timer/state-machine logic for game-over
 * delays.
 */
public class GameOverDelayController {

    private boolean pending;
    private float timer;
    private LevelResult pendingResult;

    public GameOverDelayController() {
        this.pending = false;
        this.timer = 0f;
    }

    /**
     * Starts the game-over delay countdown.
     *
     * @param delaySec seconds to wait before the transition fires
     * @param result   the LevelResult to deliver when the timer expires
     */
    public void trigger(float delaySec, LevelResult result) {
        this.pending = true;
        this.timer = delaySec;
        this.pendingResult = result;
    }

    /**
     * Ticks the delay timer. Returns {@code true} when the timer has expired
     * and the scene should transition to results.
     */
    public boolean update(float deltaTime) {
        if (!pending)
            return false;
        timer -= deltaTime;
        return timer <= 0f;
    }

    public boolean isPending() {
        return pending;
    }

    public LevelResult getPendingResult() {
        return pendingResult;
    }
}
