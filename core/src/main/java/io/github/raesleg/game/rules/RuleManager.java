package io.github.raesleg.game.rules;

public class RuleManager {

    private int rulesBroken;
    private int redLightViolations;
    private int pedestrianHits;
    private int curbHits;
    private boolean instantFail;

    public void recordRedLightViolation() {
        redLightViolations++;
        rulesBroken++;
    }

    public void recordPedestrianHit() {
        pedestrianHits++;
        instantFail = true;
    }

    public void recordCurbHit() {
        curbHits++;
        rulesBroken++;
    }

    public void recordCrosswalkViolation() {
        rulesBroken++;
    }

    /**
     * Reverses the most recent generic violation (+1).
     * Used by {@link BreakRuleCommand#undo()} to support the Command pattern.
     */
    public void undoLastViolation() {
        if (rulesBroken > 0) {
            rulesBroken--;
        }
    }

    public int getRulesBroken() {
        return rulesBroken;
    }

    public int getRedLightViolations() {
        return redLightViolations;
    }

    public int getPedestrianHits() {
        return pedestrianHits;
    }

    public int getCurbHits() {
        return curbHits;
    }

    public boolean isInstantFail() {
        return instantFail;
    }

    public void clearInstantFail() {
        instantFail = false;
    }

    public void reset() {
        rulesBroken = 0;
        redLightViolations = 0;
        pedestrianHits = 0;
        curbHits = 0;
        instantFail = false;
    }

    /**
     * Returns 0..1 aggression scale for police AI.
     * 0 rules broken = calm
     * 5 rules broken = max aggression
     */
    public float getPoliceAggression() {
        return Math.min(1f, rulesBroken / 5f);
    }
}