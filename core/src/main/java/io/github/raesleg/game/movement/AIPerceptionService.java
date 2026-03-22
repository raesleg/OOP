package io.github.raesleg.game.movement;

import java.util.List;

import io.github.raesleg.engine.entity.Entity;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.game.entities.IPerceivable;

/**
 * Reads nearby entities and builds a lightweight perception snapshot for NPC
 * AI.
 */
public class AIPerceptionService {

    private final EntityManager entityManager;

    public AIPerceptionService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public PerceptionSnapshot scan(Entity self, SensorComponent sensor) {
        float defaultDistance = sensor.getForwardRange();

        float nearestPedestrianDistance = defaultDistance;
        float nearestVehicleDistance = defaultDistance;
        float nearestObstacleDistance = defaultDistance;
        float nearestDistance = defaultDistance;
        Entity nearestEntity = null;

        List<Entity> entities = entityManager.getSnapshot();

        float selfCenterX = self.getX() + self.getW() * 0.5f;
        float selfFrontY = self.getY();

        for (Entity entity : entities) {
            if (entity == self) {
                continue;
            }

            float otherCenterX = entity.getX() + entity.getW() * 0.5f;
            float dx = Math.abs(otherCenterX - selfCenterX);
            if (dx > sensor.getSideRange()) {
                continue;
            }

            float dy = selfFrontY - entity.getY();
            if (dy < 0f || dy > sensor.getForwardRange()) {
                continue;
            }

            if (dy < nearestDistance) {
                nearestDistance = dy;
                nearestEntity = entity;
            }

            if (entity instanceof IPerceivable perceivable) {
                switch (perceivable.getPerceptionCategory()) {
                    case PEDESTRIAN -> {
                        if (dy < nearestPedestrianDistance)
                            nearestPedestrianDistance = dy;
                    }
                    case VEHICLE -> {
                        if (dy < nearestVehicleDistance)
                            nearestVehicleDistance = dy;
                    }
                    case OBSTACLE -> {
                        if (dy < nearestObstacleDistance)
                            nearestObstacleDistance = dy;
                    }
                }
            }
        }

        return new PerceptionSnapshot(
                nearestPedestrianDistance < defaultDistance,
                nearestVehicleDistance < defaultDistance,
                nearestObstacleDistance < defaultDistance,
                nearestDistance,
                nearestEntity,
                nearestPedestrianDistance,
                nearestVehicleDistance,
                nearestObstacleDistance);
    }
}
