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
    private boolean playerPassed;

    public CrosswalkZoneState() {
        this.expired = false;
        this.playerInside = false;
        this.violationFired = false;
        this.crossingActive = false;
        this.playerPassed = false;
    }

    public boolean isExpired() {
        return expired;
    }

    public void markExpired() {
        expired = true;
    }

    public void setPlayerInside(boolean inside) {
        boolean wasInside = this.playerInside;
        this.playerInside = inside;
        if (!inside) {
            this.violationFired = false;
            if (wasInside) {
                this.playerPassed = true;
            }
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

    public boolean hasPlayerPassed() {
        return playerPassed;
    }
}