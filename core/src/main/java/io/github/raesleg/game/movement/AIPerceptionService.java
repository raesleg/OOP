package io.github.raesleg.game.movement;

import java.util.List;

import io.github.raesleg.engine.entity.Entity;
import io.github.raesleg.engine.entity.EntityManager;

import io.github.raesleg.game.entities.IPerceivable;

// Look at nearby entities and extract summarised information for NpcDrivingStrategy to use for decision making
public class AIPerceptionService {

    private final EntityManager entityManager;

    public AIPerceptionService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public PerceptionSnapshot scan(Entity self, SensorComponent sensor) {
        if (self == null || sensor == null) {
            return PerceptionSnapshot.clear(0f);
        }

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
            if (!isRelevantTarget(self, entity, selfCenterX, selfFrontY, sensor)) {
                continue;
            }

            float dy = selfFrontY - entity.getY();
            if (dy < nearestDistance) {
                nearestDistance = dy;
                nearestEntity = entity;
            }

            if (entity instanceof IPerceivable perceivable) {
                switch (perceivable.getPerceptionCategory()) {
                    case PEDESTRIAN -> nearestPedestrianDistance = Math.min(nearestPedestrianDistance, dy);
                    case VEHICLE -> nearestVehicleDistance = Math.min(nearestVehicleDistance, dy);
                    case OBSTACLE -> nearestObstacleDistance = Math.min(nearestObstacleDistance, dy);
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

    private boolean isRelevantTarget(
            Entity self,
            Entity candidate,
            float selfCenterX,
            float selfFrontY,
            SensorComponent sensor) {
        if (candidate == null || candidate == self) {
            return false;
        }

        float otherCenterX = candidate.getX() + candidate.getW() * 0.5f;
        float dx = Math.abs(otherCenterX - selfCenterX);
        if (dx > sensor.getSideRange()) {
            return false;
        }

        float dy = selfFrontY - candidate.getY();
        return dy >= 0f && dy <= sensor.getForwardRange();
    }
}
