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
 * that the scene acts as a "dumb terminal" (render + input routing)
 * while this class owns timers, counters, score, end-condition
 * evaluation, and scene transitions.
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
    private int rulesBroken;
    private int crashCount;
    private boolean instantFail;
    private String instantFailReason;

    /* ── Composed sub-systems ── */
    private final ScoreSystem scoreSystem;
    private final GameOverDelayController gameOverDelay;

    /* ── End conditions (OCP) ── */
    private final List<ILevelEndCondition> endConditions = new ArrayList<>();

    public MatchDirector(SceneManager sceneManager,
            EntityManager entityManager,
            SoundDevice sound) {
        this.sceneManager = sceneManager;
        this.entityManager = entityManager;
        this.sound = sound;
        this.scoreSystem = new ScoreSystem();
        this.gameOverDelay = new GameOverDelayController();
        this.gameTime = 0f;
        this.rulesBroken = 0;
        this.crashCount = 0;
        this.instantFail = false;
        this.instantFailReason = "";
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
        scoreSystem.update(deltaTime, isMoving);
    }

    /*
     * ════════════════════════════════════════════
     * Explosion delay
     * ════════════════════════════════════════════
     */

    public boolean isExplosionPending() {
        return gameOverDelay.isPending();
    }

    /**
     * Ticks the explosion delay timer. When the delay expires the
     * director transitions to the results screen automatically.
     *
     * @return {@code true} if the delay expired this frame
     */
    public boolean tickExplosionDelay(float deltaTime) {
        if (!gameOverDelay.update(deltaTime))
            return false;
        sceneManager.set(new ResultsScene(gameOverDelay.getPendingResult(), retryFactory));
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
     * Stops at the first condition that fires.
     */
    public void checkLevelEnd() {
        if (gameOverDelay.isPending())
            return;
        for (ILevelEndCondition c : endConditions) {
            if (c.evaluate())
                return;
        }
    }

    /*
     * ════════════════════════════════════════════
     * Built-in evaluators (registered as conditions by the scene)
     * ════════════════════════════════════════════
     */

    /**
     * Evaluates whether the player has reached the end of the level.
     *
     * @param progress 0.0–1.0 fraction of level completed
     * @return {@code true} if the level is complete (transition initiated)
     */
    public boolean evaluateWin(float progress) {
        if (progress < 1.0f)
            return false;
        Gdx.app.log("MatchDirector", "Level complete! Score: " + scoreSystem.getScore());
        transitionToResults(true, "");
        return true;
    }

    /**
     * Evaluates whether crash count has reached the explosion threshold.
     * Triggers an explosion game-over if the threshold is met.
     */
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

    /**
     * Evaluates a subclass-defined game-over condition.
     *
     * @param isGameOver whether the subclass considers the game over
     * @param reason     human-readable reason string
     * @return {@code true} if game over (transition initiated)
     */
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
     * Explosion game-over (delegates to ExplosionSystem)
     * ════════════════════════════════════════════
     */

    /**
     * Triggers an explosion at the given position and schedules a
     * delayed transition to the results screen.
     */
    public void triggerExplosionGameOver(String reason,
            float playerCenterX,
            float playerCenterY) {
        LevelResult result = buildResult(false, reason);
        gameOverDelay.trigger(GameConstants.EXPLOSION_DELAY, result);
        ExplosionSystem.trigger(entityManager, sound, playerCenterX, playerCenterY);
        sound.stopSound("drive");
    }

    /*
     * ════════════════════════════════════════════
     * State accessors / mutators
     * ════════════════════════════════════════════
     */

    public int getScore() {
        return scoreSystem.getScore();
    }

    public void addBonus(int delta) {
        scoreSystem.addBonus(delta);
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
        LevelResult result = buildResult(won, reason);
        sceneManager.set(new ResultsScene(result, retryFactory));
    }

    private LevelResult buildResult(boolean won, String reason) {
        List<String> violations = (violationLogSupplier != null)
                ? violationLogSupplier.get()
                : List.of();
        return new LevelResult(
                scoreSystem.getScore(), gameTime, rulesBroken,
                levelName, won, reason, violations);
    }
}
