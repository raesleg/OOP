package io.github.raesleg.game.movement;

import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.movement.MovementStrategy;
import io.github.raesleg.game.GameConstants;

// converts AI perception into simple throttle/steering commands for NPC vehicles
// normalized forward throttle (0 to 1) and steering (-1 to 1)
public class NpcDrivingStrategy implements MovementStrategy {

    private AIPerceptionService perceptionService;
    private SensorComponent sensor;

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
        if (shouldStopForPedestrian(snapshot)) {
            return 0f;
        }
        if (shouldFollowVehicleSlowly(snapshot)) {
            return GameConstants.NPC_SLOW_SPEED;
        }
        if (shouldSlowForObstacle(snapshot)) {
            return GameConstants.NPC_OBSTACLE_SPEED;
        }

        return GameConstants.NPC_DEFAULT_SPEED;
    }

    private boolean shouldStopForPedestrian(PerceptionSnapshot snapshot) {
        return snapshot.pedestrianAhead()
                && snapshot.nearestPedestrianDistance() < sensor.getStopDistance();
    }

    private boolean shouldFollowVehicleSlowly(PerceptionSnapshot snapshot) {
        return snapshot.vehicleAhead()
                && snapshot.nearestVehicleDistance() < sensor.getFollowDistance();
    }

    private boolean shouldSlowForObstacle(PerceptionSnapshot snapshot) {
        return snapshot.obstacleAhead()
                && snapshot.nearestObstacleDistance() < sensor.getFollowDistance();
    }

}
