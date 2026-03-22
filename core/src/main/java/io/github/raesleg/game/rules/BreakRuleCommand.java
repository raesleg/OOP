package io.github.raesleg.game.rules;

import com.badlogic.gdx.Gdx;

import io.github.raesleg.engine.io.ICommand;

/**
 * BreakRuleCommand — Concrete command that increments the rule-break counter
 * (Command Pattern).
 * <p>
 * Each execution adds one rule violation to the scene's counter.
 * {@link #undo()} reverses the penalty (e.g., for a future "grace period"
 * power-up).
 * <p>
 * <b>SRP:</b> Encapsulates exactly one reversible action — incrementing
 * a penalty counter.
 * <b>OCP:</b> New penalty types (e.g., double penalty for pedestrian hits)
 * become new ICommand implementations — this class never changes.
 * <b>DIP:</b> The engine's {@link io.github.raesleg.engine.io.CommandHistory}
 * depends only on {@link ICommand}, never on this concrete class.
 * <p>
 * <b>Engine/Game Boundary:</b> Implements engine's ICommand interface.
 * Lives entirely in the game layer.
 */
public class BreakRuleCommand implements ICommand {

    private final RuleManager ruleManager;
    private final String violationType;
    private final int weight;

    /**
     * Creates a rule-break command with weight 1.
     *
     * @param ruleManager   the rule manager that tracks violations
     * @param violationType a label for logging (e.g., "CROSSWALK_VIOLATION")
     */
    public BreakRuleCommand(RuleManager ruleManager, String violationType) {
        this(ruleManager, violationType, 1);
    }

    /**
     * Creates a rule-break command with a custom weight.
     *
     * @param ruleManager   the rule manager that tracks violations
     * @param violationType a label for logging
     * @param weight        how many stars this violation adds
     */
    public BreakRuleCommand(RuleManager ruleManager, String violationType, int weight) {
        this.ruleManager = ruleManager;
        this.violationType = violationType;
        this.weight = weight;
    }

    @Override
    public void execute() {
        for (int i = 0; i < weight; i++) {
            ruleManager.recordViolation();
        }
        Gdx.app.log("BreakRuleCommand",
                "Rule broken: " + violationType + " (+" + weight + ")"
                        + " | Total: " + ruleManager.getRulesBroken());
    }

    @Override
    public void undo() {
        for (int i = 0; i < weight; i++) {
            ruleManager.undoLastViolation();
        }
        Gdx.app.log("BreakRuleCommand",
                "Rule undone: " + violationType
                        + " | Total: " + ruleManager.getRulesBroken());
    }
}
