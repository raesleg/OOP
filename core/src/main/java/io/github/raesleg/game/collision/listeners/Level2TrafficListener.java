package io.github.raesleg.game.collision.listeners;

import io.github.raesleg.engine.io.CommandHistory;
import io.github.raesleg.game.GameConstants;
import io.github.raesleg.game.rules.BreakRuleCommand;
import io.github.raesleg.game.rules.RuleManager;

/**
 * Level2TrafficListener — Standalone listener for Level 2 traffic violations.
 */

public final class Level2TrafficListener implements TrafficViolationListener {

    @FunctionalInterface
    public interface ScoreCallback {
        void addScore(int delta);
    }

    @FunctionalInterface
    public interface CrashCallback {
        void incrementCrashCount();
    }

    @FunctionalInterface
    public interface SpeedPenaltyCallback {
        void applyPenalty();
    }

    private final RuleManager ruleManager;
    private final CommandHistory commandHistory;
    private final ScoreCallback scoreCallback;
    private final CrashCallback crashCallback;
    private final SpeedPenaltyCallback speedPenaltyCallback;

    public Level2TrafficListener(RuleManager ruleManager,
            CommandHistory commandHistory,
            ScoreCallback scoreCallback,
            CrashCallback crashCallback,
            SpeedPenaltyCallback speedPenaltyCallback) {
        this.ruleManager = ruleManager;
        this.commandHistory = commandHistory;
        this.scoreCallback = scoreCallback;
        this.crashCallback = crashCallback;
        this.speedPenaltyCallback = speedPenaltyCallback;
    }

    @Override
    public void onTrafficCrash() {
        commandHistory.executeAndRecord(
                new BreakRuleCommand(ruleManager, "TRAFFIC_CRASH",
                        GameConstants.TRAFFIC_CRASH_STARS));
        crashCallback.incrementCrashCount();
        scoreCallback.addScore(GameConstants.SCORE_PENALTY);
        speedPenaltyCallback.applyPenalty();
    }
}
