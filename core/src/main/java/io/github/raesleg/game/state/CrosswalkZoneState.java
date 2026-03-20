package io.github.raesleg.game.state;

/**
 * Stores crosswalk-zone state only.
 * Does not depend on pedestrian entity movement state.
 */
public class CrosswalkZoneState {

    private boolean expired;
    private boolean playerInside;
    private boolean violationFired;
    private boolean crossingActive;

    public CrosswalkZoneState() {
        this.expired = false;
        this.playerInside = false;
        this.violationFired = false;
        this.crossingActive = false;
    }

    public boolean isExpired() {
        return expired;
    }

    public void markExpired() {
        expired = true;
    }

    public void setPlayerInside(boolean inside) {
        this.playerInside = inside;
        if (!inside) {
            this.violationFired = false;
        }
    }

    public boolean isPlayerInside() {
        return playerInside;
    }

    public boolean tryFireViolation() {
        if (violationFired) {
            return false;
        }
        violationFired = true;
        return true;
    }

    public void setCrossingActive(boolean crossingActive) {
        this.crossingActive = crossingActive;
    }

    public boolean isPedestrianCrossing() {
        return crossingActive;
    }
}