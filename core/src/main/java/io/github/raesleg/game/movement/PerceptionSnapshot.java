package io.github.raesleg.game.movement;

import io.github.raesleg.engine.entity.Entity;

public record PerceptionSnapshot(
        boolean pedestrianAhead,
        boolean vehicleAhead,
        boolean obstacleAhead,
        float nearestDistance,
        Entity nearestEntity,
        float nearestPedestrianDistance,
        float nearestVehicleDistance,
        float nearestObstacleDistance) {

    public static PerceptionSnapshot clear(float defaultDistance) {
        return new PerceptionSnapshot(
                false,
                false,
                false,
                defaultDistance,
                null,
                defaultDistance,
                defaultDistance,
                defaultDistance
        );
    }
}
