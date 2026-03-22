package io.github.raesleg.game.movement;

/**
 * Simple sensor profile for NPC traffic AI.
 * Kept to avoid breaking NPCCar and spawner wiring.
 */
public class SensorComponent {

    private final float forwardRange;
    private final float sideRange;
    private final float stopDistance;
    private final float followDistance;

    public SensorComponent(
            float forwardRange,
            float sideRange,
            float stopDistance,
            float followDistance) {
        this.forwardRange = forwardRange;
        this.sideRange = sideRange;
        this.stopDistance = stopDistance;
        this.followDistance = followDistance;
    }

    public float getForwardRange() {
        return forwardRange;
    }

    public float getSideRange() {
        return sideRange;
    }

    public float getStopDistance() {
        return stopDistance;
    }

    public float getFollowDistance() {
        return followDistance;
    }
}
