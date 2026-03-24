package io.github.raesleg.game.movement;

import io.github.raesleg.game.GameConstants;

// Base movement + sensing stats for vehicle types 
public final class VehicleProfile {

    // Movement parameters
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

    // Sensing parameters
    private final float pedestrianStopDistance;
    private final float vehicleFollowDistance;
    private final float obstacleStopDistance;
    
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
            float slideRecoveryTime,
            float pedestrianStopDistance,
            float vehicleFollowDistance,
            float obstacleStopDistance) {
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
        this.pedestrianStopDistance = pedestrianStopDistance;
        this.vehicleFollowDistance = vehicleFollowDistance;
        this.obstacleStopDistance = obstacleStopDistance;
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

    public float getPedestrianStopDistance() {
        return pedestrianStopDistance;
    }

    public float getVehicleFollowDistance() {
        return vehicleFollowDistance;
    }

    public float getObstacleStopDistance() {
        return obstacleStopDistance;
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
                2.8f,
                0f,
                0f,
                0f
            );
    }

    public static VehicleProfile npcTraffic() {
        return new VehicleProfile(
                2.5f,
                10f,
                0.05f,
                0f,
                0f,
                0f,
                8f,
                false,
                false,
                false,
                0f,
                GameConstants.NPC_PEDESTRIAN_STOP_DIST,
                GameConstants.NPC_VEHICLE_SLOW_DIST,
                GameConstants.NPC_OBSTACLE_SLOW_DIST
            );
    }
}
