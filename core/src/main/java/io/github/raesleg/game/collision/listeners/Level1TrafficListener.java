package io.github.raesleg.game.collision.listeners;

import com.badlogic.gdx.math.Vector2;

import io.github.raesleg.engine.io.CommandHistory;
import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.game.GameConstants;
import io.github.raesleg.game.entities.misc.Pedestrian;
import io.github.raesleg.game.rules.BreakRuleCommand;
import io.github.raesleg.game.rules.RuleManager;
import io.github.raesleg.game.scene.CrosswalkEncounterSystem;

/**
 * Level1TrafficListener — Standalone listener for Level 1 traffic violations.
 * Violation reaction logic (score penalties, sound effects, rule recording)
 */

public final class Level1TrafficListener implements TrafficViolationListener {

    // Callback for score changes — avoids coupling to BaseGameScene
    @FunctionalInterface
    public interface ScoreCallback {
        void addScore(int delta);
    }

    // Callback for crash count — avoids coupling to BaseGameScene
    @FunctionalInterface
    public interface CrashCallback {
        void incrementCrashCount();
    }

    private final RuleManager ruleManager;
    private final CommandHistory commandHistory;
    private final SoundDevice sound;
    private final CrosswalkEncounterSystem crosswalkSystem;
    private final ScoreCallback scoreCallback;
    private final CrashCallback crashCallback;

    public Level1TrafficListener(RuleManager ruleManager,
            CommandHistory commandHistory,
            SoundDevice sound,
            CrosswalkEncounterSystem crosswalkSystem,
            ScoreCallback scoreCallback,
            CrashCallback crashCallback) {
        this.ruleManager = ruleManager;
        this.commandHistory = commandHistory;
        this.sound = sound;
        this.crosswalkSystem = crosswalkSystem;
        this.scoreCallback = scoreCallback;
        this.crashCallback = crashCallback;
    }

    @Override
    public void onCrosswalkViolation() {
        commandHistory.executeAndRecord(
                new BreakRuleCommand(ruleManager, "CROSSWALK",
                        GameConstants.CROSSWALK_VIOLATION_STARS));
        sound.playSound("negative", 1.0f);
        scoreCallback.addScore(GameConstants.SCORE_PENALTY);
    }

    @Override
    public void onTrafficCrash() {
        commandHistory.executeAndRecord(
                new BreakRuleCommand(ruleManager, "TRAFFIC_CRASH",
                        GameConstants.TRAFFIC_CRASH_STARS));
        crashCallback.incrementCrashCount();
        scoreCallback.addScore(GameConstants.SCORE_PENALTY);
    }

    @Override
    public void onPedestrianHit(Pedestrian pedestrian, Vector2 knockbackDirection,
            float knockbackForce) {
        commandHistory.executeAndRecord(
                new io.github.raesleg.game.rules.PedestrianHitCommand(ruleManager));
        sound.playSound("pedestrain_hit", 1.0f);
        sound.playSound("scream", 1.0f);
        crosswalkSystem.triggerPedestrianHit(pedestrian, knockbackDirection, knockbackForce);
    }
}
