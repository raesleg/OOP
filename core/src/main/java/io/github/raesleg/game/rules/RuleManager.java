package io.github.raesleg.game.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RuleManager — tracks traffic-rule violations using a single generic counter.
 * <p>
 * All violation types are recorded through {@link #recordViolation()} (called
 * by {@link BreakRuleCommand}). Per-type counters were removed to satisfy OCP:
 * adding a new violation type no longer requires a new method here.
 * <p>
 * <b>SRP:</b> Only manages violation state — does not decide consequences.
 * <b>OCP:</b> New violation types only require a new {@code ICommand}; this
 * class stays closed for modification.
 */
public class RuleManager {

    private int rulesBroken;
    private final List<String> violationLog = new ArrayList<>();

    /**
     * Generic violation recorder — used by BreakRuleCommand for any violation type.
     */
    public void recordViolation() {
        rulesBroken++;
    }

    /**
     * Records a violation with a human-readable type label for the log.
     */
    public void recordViolation(String type) {
        rulesBroken++;
        violationLog.add(type);
    }

    /**
     * Reverses one violation (used by {@code BreakRuleCommand.undo()}).
     */
    public void undoLastViolation() {
        if (rulesBroken > 0) {
            rulesBroken--;
        }
        if (!violationLog.isEmpty()) {
            violationLog.remove(violationLog.size() - 1);
        }
    }

    public int getRulesBroken() {
        return rulesBroken;
    }

    /**
     * Returns an unmodifiable view of all recorded violation labels.
     */
    public List<String> getViolationLog() {
        return Collections.unmodifiableList(violationLog);
    }

    /**
     * Returns 0..1 aggression scale for police AI.
     * 0 rules broken = calm, 5 rules broken = max aggression.
     */
    public float getPoliceAggression() {
        return Math.min(1f, rulesBroken / 5f);
    }

    public void reset() {
        rulesBroken = 0;
        violationLog.clear();
    }
}