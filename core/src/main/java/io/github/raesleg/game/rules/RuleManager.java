package io.github.raesleg.game.rules;

public class RuleManager {

    private int rulesBroken;
    private int redLightViolations;
    private int pedestrianHits;
    private int curbHits;

    public void recordRedLightViolation() {
        redLightViolations++;
        rulesBroken++;
    }

    public void recordPedestrianHit() {
        pedestrianHits++;
        rulesBroken += 2; // more serious
    }

    public void recordCurbHit() {
        curbHits++;
        rulesBroken++;
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

    public void reset() {
        rulesBroken = 0;
        redLightViolations = 0;
        pedestrianHits = 0;
        curbHits = 0;
    }

    /**
     * Returns 0..1 aggression scale for police AI.
     * 0 rules broken = calm
     * 8+ rules broken = max aggression
     */
    public float getPoliceAggression() {
        return Math.min(1f, rulesBroken / 8f);
    }
}