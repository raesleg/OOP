package io.github.raesleg.game.movement;

/**
 * Base movement stats for a vehicle type.
 */
public final class VehicleProfile {

    private final float maxLateralSpeed;
    private final float steeringResponse;
    private final float steeringDeadzone;
    private final float maxForwardSpeed;
    private final float acceleration;
    private final float brakeStrength;
    private final float linearDamping;
    private final boolean allowForwardMotion;
    private final boolean allowReverseMotion;
    private final boolean puddleSlideRecovery;
    private final float slideRecoveryTime;

    public VehicleProfile(
            float maxLateralSpeed,
            float steeringResponse,
            float steeringDeadzone,
            float maxForwardSpeed,
            float acceleration,
            float brakeStrength,
            float linearDamping,
            boolean allowForwardMotion,
            boolean allowReverseMotion,
            boolean puddleSlideRecovery,
            float slideRecoveryTime) {
        this.maxLateralSpeed = maxLateralSpeed;
        this.steeringResponse = steeringResponse;
        this.steeringDeadzone = steeringDeadzone;
        this.maxForwardSpeed = maxForwardSpeed;
        this.acceleration = acceleration;
        this.brakeStrength = brakeStrength;
        this.linearDamping = linearDamping;
        this.allowForwardMotion = allowForwardMotion;
        this.allowReverseMotion = allowReverseMotion;
        this.puddleSlideRecovery = puddleSlideRecovery;
        this.slideRecoveryTime = slideRecoveryTime;
    }

    public float getMaxLateralSpeed() {
        return maxLateralSpeed;
    }

    public float getSteeringResponse() {
        return steeringResponse;
    }

    public float getSteeringDeadzone() {
        return steeringDeadzone;
    }

    public float getMaxForwardSpeed() {
        return maxForwardSpeed;
    }

    public float getAcceleration() {
        return acceleration;
    }

    public float getBrakeStrength() {
        return brakeStrength;
    }

    public float getLinearDamping() {
        return linearDamping;
    }

    public boolean allowsForwardMotion() {
        return allowForwardMotion;
    }

    public boolean allowsReverseMotion() {
        return allowReverseMotion;
    }

    public boolean usesPuddleSlideRecovery() {
        return puddleSlideRecovery;
    }

    public float getSlideRecoveryTime() {
        return slideRecoveryTime;
    }

    public static VehicleProfile playerArcade() {
        return new VehicleProfile(
                12.5f,
                22f,
                0.08f,
                0f,
                0f,
                0f,
                6f,
                false,
                false,
                true,
                2.8f
        );
    }

    public static VehicleProfile npcTraffic() {
        return new VehicleProfile(
                2.5f,
                10f,
                0.05f,
                8f,
                10f,
                18f,
                8f,
                true,
                false,
                false,
                0f
        );
    }
}
