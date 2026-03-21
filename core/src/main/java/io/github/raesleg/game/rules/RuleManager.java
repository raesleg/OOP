package io.github.raesleg.game.rules;

/**
 * RuleManager — tracks all player violations during a level.
 *
 * Each violation type has its own counter for the results screen,
 * plus a shared rulesBroken total used for game-over and police aggression.
 *
 * getPoliceAggression() lives here because aggression is derived purely
 * from rules state — it is not a movement or AI concern.
 *
 * Note: undoLastViolation() is kept for Command pattern support
 * (called by CommandHistory.undo() via BreakRuleCommand).
 * Do not call it directly — always go through CommandHistory.
 */
public class RuleManager {

    private int rulesBroken;
    private int redLightViolations;
    private int speedingViolations;
    private int pedestrianHits;
    private int curbHits;
    private int trafficCrashes;

    public void recordRedLightViolation() {
        redLightViolations++;
        rulesBroken++;
    }

    public void recordPedestrianHit() {
        pedestrianHits++;
        rulesBroken++;
    }

    public void recordCurbHit() {
        curbHits++;
        rulesBroken++;
    }

    public void recordCrosswalkViolation() {
        rulesBroken++;
    }

    public void recordSpeedingViolation() {
        speedingViolations++;
        rulesBroken++;
    }

    public void recordTrafficCrash() {
        trafficCrashes++;
        rulesBroken++;
    }

    /** Called by CommandHistory.undo() via BreakRuleCommand — do not call directly. */
    public void undoLastViolation() {
        if (rulesBroken > 0) rulesBroken--;
    }

    public int getRulesBroken()        { return rulesBroken; }
    public int getRedLightViolations() { return redLightViolations; }
    public int getSpeedingViolations() { return speedingViolations; }
    public int getPedestrianHits()     { return pedestrianHits; }
    public int getCurbHits()           { return curbHits; }
    public int getTrafficCrashes()     { return trafficCrashes; }

    public void reset() {
        rulesBroken        = 0;
        redLightViolations = 0;
        speedingViolations = 0;
        pedestrianHits     = 0;
        curbHits           = 0;
        trafficCrashes     = 0;
    }
}