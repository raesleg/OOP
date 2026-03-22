package io.github.raesleg.game.movement;

import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.movement.MovementStrategy;
import io.github.raesleg.game.GameConstants;

public class NpcDrivingStrategy implements MovementStrategy {

    private final AIPerceptionService perceptionService;
    private final SensorComponent sensor;

    public NpcDrivingStrategy(AIPerceptionService perceptionService, SensorComponent sensor) {
        this.perceptionService = perceptionService;
        this.sensor = sensor;
    }

    @Override
    public float getX(MovableEntity entity, float dt) {
        return 0f; // always straight
    }

    @Override
    public float getY(MovableEntity entity, float dt) {
        PerceptionSnapshot snapshot = perceptionService.scan(entity, sensor);

        if (snapshot == null) {
            return 0f;
        }

        // Stop for pedestrians
        if (snapshot.pedestrianAhead()
                && snapshot.nearestPedestrianDistance() < GameConstants.NPC_PEDESTRIAN_STOP_DIST) {
            return 0f;
        }

        // Slow down if another vehicle is too close ahead
        if (snapshot.vehicleAhead() && snapshot.nearestVehicleDistance() < GameConstants.NPC_VEHICLE_SLOW_DIST) {
            return GameConstants.NPC_SLOW_SPEED;
        }

        // Slow down for general obstacles ahead
        if (snapshot.obstacleAhead() && snapshot.nearestObstacleDistance() < GameConstants.NPC_OBSTACLE_SLOW_DIST) {
            return GameConstants.NPC_OBSTACLE_SPEED;
        }

        return GameConstants.NPC_DEFAULT_SPEED;
    }
}
