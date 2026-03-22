package io.github.raesleg.game.rules;

public class RuleManager {

    private int rulesBroken;
    private int redLightViolations;
    private int speedingViolations;
    private int pedestrianHits;
    private int curbHits;
    private boolean instantFail;

    public void setRedLightViolation() {
        redLightViolations++;
        rulesBroken++;
    }

    public void setPedestrianHit() {
        pedestrianHits++;
        instantFail = true;
    }

    public void setCurbHit() {
        curbHits++;
        rulesBroken++;
    }

    public void recordCrosswalkViolation() {
        rulesBroken++;
    }

    /**
     * Generic violation recorder — used by BreakRuleCommand for any violation type.
     */
    public void recordViolation() {
        rulesBroken++;
    }

    public void setSpeedingViolation() {
        speedingViolations++;
        rulesBroken++;
    }

    public void undoLastViolation() { // ????
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

    public int getSpeedingViolations() {
        return speedingViolations;
    }

    public int getPedestrianHits() {
        return pedestrianHits;
    }

    public int getCurbHits() {
        return curbHits;
    }

    /**
     * Returns 0..1 aggression scale for police AI.
     * 0 rules broken = calm
     * 5 rules broken = max aggression
     */ // dont know if supposed to be here
    public float getPoliceAggression() {
        return Math.min(1f, rulesBroken / 5f);
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
        speedingViolations = 0;
        pedestrianHits = 0;
        curbHits = 0;
        instantFail = false;
    }
}