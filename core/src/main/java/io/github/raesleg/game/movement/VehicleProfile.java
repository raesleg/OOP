package io.github.raesleg.game.movement;

// Base movement + sensing stats for vehicle types 
public final class VehicleProfile {

    // Movement parameters
    private final float maxLateralSpeed;
    private final float steeringResponse;
    private final float maxForwardSpeed;
    private final float acceleration;
    private final float linearDamping;
    private final float slideRecoveryTime;
    
    public VehicleProfile(
            float maxLateralSpeed,
            float steeringResponse,
            float maxForwardSpeed,
            float acceleration,
            float linearDamping,
            float slideRecoveryTime) {
        this.maxLateralSpeed = maxLateralSpeed;
        this.steeringResponse = steeringResponse;
        this.maxForwardSpeed = maxForwardSpeed;
        this.acceleration = acceleration;
        this.linearDamping = linearDamping;
        this.slideRecoveryTime = slideRecoveryTime;
    }

    public float getMaxLateralSpeed() {
        return maxLateralSpeed;
    }

    public float getSteeringResponse() {
        return steeringResponse;
    }

    public float getMaxForwardSpeed() {
        return maxForwardSpeed;
    }

    public float getAcceleration() {
        return acceleration;
    }

    public float getLinearDamping() {
        return linearDamping;
    }

    public float getSlideRecoveryTime() {
        return slideRecoveryTime;
    }

    public static VehicleProfile playerArcade() {
        return new VehicleProfile(
                12.5f,
                22f,
                0f,
                0f,
                6f,
                2.8f
                );
    }

    public static VehicleProfile npcTraffic() {
        return new VehicleProfile(
                2.5f,
                10f,
                0f,
                0f,
                8f,
                2.8f
            );
    }
}