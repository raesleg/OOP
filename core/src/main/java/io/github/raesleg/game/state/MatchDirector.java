package io.github.raesleg.game.state;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.badlogic.gdx.Gdx;

import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.scene.Scene;
import io.github.raesleg.engine.scene.SceneManager;
import io.github.raesleg.game.GameConstants;
import io.github.raesleg.game.scene.ExplosionSystem;
import io.github.raesleg.game.scene.ILevelEndCondition;
import io.github.raesleg.game.scene.LevelResult;
import io.github.raesleg.game.scene.ResultsScene;

/**
 * MatchDirector — Single-responsibility owner of all match-level state
 * and win/loss evaluation for a gameplay session.
 * <p>
 * Extracted from {@link io.github.raesleg.game.scene.BaseGameScene} so
 * that the scene acts as a rendering/input terminal while this class
 * owns timers, counters, score, end-condition evaluation, and scene
 * transitions.
 * <p>
 * <b>SRP:</b> Owns match state and lifecycle — nothing else.
 * <b>OCP:</b> End conditions are registered via
 * {@link #addEndCondition(ILevelEndCondition)} without modifying this class.
 * <b>DIP:</b> Depends on engine abstractions (SceneManager, EntityManager,
 * SoundDevice) injected via constructor.
 */
public final class MatchDirector {

    /* ── Dependencies (injected) ── */
    private final SceneManager sceneManager;
    private final EntityManager entityManager;
    private final SoundDevice sound;

    /* ── Configuration (set once via configure()) ── */
    private String levelName;
    private Supplier<Scene> retryFactory;
    private Supplier<List<String>> violationLogSupplier;

    /* ── Match state ── */
    private float gameTime;
    private float scoreAccumulator;
    private int scoreBonus;
    private int rulesBroken;
    private int crashCount;
    private boolean instantFail;
    private String instantFailReason;

    /* ── Explosion game-over delay ── */
    private boolean gameOverPending;
    private float gameOverTimer;
    private LevelResult pendingResult;

    /* ── End conditions (OCP) ── */
    private final List<ILevelEndCondition> endConditions = new ArrayList<>();

    public MatchDirector(SceneManager sceneManager,
            EntityManager entityManager,
            SoundDevice sound) {
        this.sceneManager = sceneManager;
        this.entityManager = entityManager;
        this.sound = sound;
        this.gameTime = 0f;
        this.scoreAccumulator = 0f;
        this.scoreBonus = 0;
        this.rulesBroken = 0;
        this.crashCount = 0;
        this.instantFail = false;
        this.instantFailReason = "";
        this.gameOverPending = false;
        this.gameOverTimer = 0f;
    }

    /**
     * One-time configuration called by the scene after construction.
     *
     * @param levelName            display name shown on the results screen
     * @param retryFactory         supplier that creates a fresh level instance
     * @param violationLogSupplier supplier for the current violation log
     */
    public void configure(String levelName,
            Supplier<Scene> retryFactory,
            Supplier<List<String>> violationLogSupplier) {
        this.levelName = levelName;
        this.retryFactory = retryFactory;
        this.violationLogSupplier = violationLogSupplier;
    }

    /*
     * ════════════════════════════════════════════
     * Per-frame updates
     * ════════════════════════════════════════════
     */

    public void advanceTime(float deltaTime) {
        gameTime += deltaTime;
    }

    public void updateScore(float deltaTime, boolean isMoving) {
        if (isMoving) {
            scoreAccumulator += deltaTime * GameConstants.SCORE_RATE_PER_SECOND;
        }
    }

    /*
     * ════════════════════════════════════════════
     * Explosion delay
     * ════════════════════════════════════════════
     */

    public boolean isExplosionPending() {
        return gameOverPending;
    }

    /**
     * Ticks the explosion delay timer. Returns {@code true} when the
     * timer expires and transitions to the results screen.
     */
    public boolean tickExplosionDelay(float deltaTime) {
        if (!gameOverPending)
            return false;
        gameOverTimer -= deltaTime;
        if (gameOverTimer > 0f)
            return false;
        sceneManager.set(new ResultsScene(pendingResult, retryFactory));
        return true;
    }

    /*
     * ════════════════════════════════════════════
     * End-condition system (OCP)
     * ════════════════════════════════════════════
     */

    public void addEndCondition(ILevelEndCondition condition) {
        endConditions.add(condition);
    }

    /**
     * Iterates registered end conditions every frame.
     * Stops at the first that fires.
     */
    public void checkLevelEnd() {
        if (gameOverPending)
            return;
        for (ILevelEndCondition c : endConditions) {
            if (c.evaluate())
                return;
        }
    }

    /*
     * ════════════════════════════════════════════
     * Built-in evaluators
     * ════════════════════════════════════════════
     */

    public boolean evaluateWin(float progress) {
        if (progress < 1.0f)
            return false;
        Gdx.app.log("MatchDirector", "Level complete! Score: " + getScore());
        transitionToResults(true, "");
        return true;
    }

    public boolean evaluateCrashExplosion(float playerCenterX, float playerCenterY) {
        if (crashCount < GameConstants.CRASH_EXPLOSION_THRESHOLD)
            return false;
        Gdx.app.log("MatchDirector", "3rd crash! Triggering explosion...");
        triggerExplosionGameOver("Crashed into too many vehicles", playerCenterX, playerCenterY);
        return true;
    }

    public boolean evaluateInstantFail() {
        if (!instantFail)
            return false;
        Gdx.app.log("MatchDirector", "Instant fail! Reason: " + instantFailReason);
        sound.playSound("negative", 1.0f);
        transitionToResults(false, instantFailReason);
        return true;
    }

    public boolean evaluateSubclassGameOver(boolean isGameOver, String reason) {
        if (!isGameOver)
            return false;
        Gdx.app.log("MatchDirector", "Game Over! Reason: " + reason);
        sound.playSound("negative", 1.0f);
        transitionToResults(false, reason);
        return true;
    }

    /*
     * ════════════════════════════════════════════
     * Explosion game-over
     * ════════════════════════════════════════════
     */

    public void triggerExplosionGameOver(String reason,
            float playerCenterX,
            float playerCenterY) {
        gameOverPending = true;
        gameOverTimer = GameConstants.EXPLOSION_DELAY;
        pendingResult = buildResult(false, reason);
        ExplosionSystem.trigger(entityManager, sound, playerCenterX, playerCenterY);
        sound.stopSound("drive");
    }

    /*
     * ════════════════════════════════════════════
     * State accessors / mutators
     * ════════════════════════════════════════════
     */

    public int getScore() {
        return (int) scoreAccumulator + scoreBonus;
    }

    public void addBonus(int delta) {
        this.scoreBonus += delta;
    }

    public float getGameTime() {
        return gameTime;
    }

    public int getRulesBroken() {
        return rulesBroken;
    }

    public void setRulesBroken(int n) {
        this.rulesBroken = n;
    }

    public int getCrashCount() {
        return crashCount;
    }

    public void incrementCrashCount() {
        this.crashCount++;
    }

    public boolean isInstantFail() {
        return instantFail;
    }

    public void setInstantFail(boolean flag, String reason) {
        this.instantFail = flag;
        this.instantFailReason = reason;
    }

    /*
     * ════════════════════════════════════════════
     * Private helpers
     * ════════════════════════════════════════════
     */

    private void transitionToResults(boolean won, String reason) {
        sceneManager.set(new ResultsScene(buildResult(won, reason), retryFactory));
    }

    private LevelResult buildResult(boolean won, String reason) {
        List<String> violations = (violationLogSupplier != null)
                ? violationLogSupplier.get()
                : List.of();
        return new LevelResult(
                getScore(), gameTime, rulesBroken,
                levelName, won, reason, violations);
    }
}
