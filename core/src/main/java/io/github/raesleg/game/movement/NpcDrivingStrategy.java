package io.github.raesleg.game.movement;

import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.movement.MovementStrategy;
import io.github.raesleg.game.entities.vehicles.NPCCar;

public class NpcDrivingStrategy implements MovementStrategy {

    private final AIPerceptionService perceptionService;

    public NpcDrivingStrategy(AIPerceptionService perceptionService) {
        this.perceptionService = perceptionService;
    }

    @Override
    public float getX(MovableEntity entity, float dt) {
        return 0f; // always straight
    }

    @Override
    public float getY(MovableEntity entity, float dt) {
        if (!(entity instanceof NPCCar npc)) {
            return 0f;
        }

        PerceptionSnapshot snapshot = perceptionService.scan(npc);

        if (snapshot == null) {
            return 0f;
        }

        // Stop for pedestrians
        if (snapshot.pedestrianAhead() && snapshot.nearestPedestrianDistance() < 90f) {
            return 0f;
        }

        // Slow down if another vehicle is too close ahead
        if (snapshot.vehicleAhead() && snapshot.nearestVehicleDistance() < 110f) {
            return 0.25f;
        }

        // Slow down for general obstacles ahead
        if (snapshot.obstacleAhead() && snapshot.nearestObstacleDistance() < 85f) {
            return 0.3f;
        }

        return 0.45f; // slower than player, straight line
    }
}
