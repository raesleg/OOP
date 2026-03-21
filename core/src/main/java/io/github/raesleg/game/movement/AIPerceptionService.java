// package io.github.raesleg.game.movement;

// import java.util.List;

// import io.github.raesleg.engine.entity.Entity;
// import io.github.raesleg.engine.entity.EntityManager;
// import io.github.raesleg.engine.movement.MovableEntity;
// import io.github.raesleg.game.entities.misc.Pedestrian;
// import io.github.raesleg.game.entities.misc.StopSign;
// import io.github.raesleg.game.entities.misc.Tree;
// import io.github.raesleg.game.entities.vehicles.NPCCar;

// public class AIPerceptionService {

//     private final EntityManager entityManager;

//     public AIPerceptionService(EntityManager entityManager) {
//         this.entityManager = entityManager;
//     }

//     public PerceptionSnapshot scan(NPCCar self) {
//         SensorComponent sensor = self.getSensor();
//         float bestDistance = sensor.getForwardRange();
//         Entity nearest = null;

//         float nearestPedestrianDistance = sensor.getForwardRange();
//         float nearestVehicleDistance = sensor.getForwardRange();
//         float nearestObstacleDistance = sensor.getForwardRange();

//         List<Entity> entities = entityManager.getSnapshot();

//         float selfCenterX = self.getX() + self.getW() / 2f;
//         float selfFrontY = self.getY() + self.getH();

//         for (Entity entity : entities) {
//             if (entity == self) continue;

//             float otherCenterX = entity.getX() + entity.getW() / 2f;
//             float dx = Math.abs(otherCenterX - selfCenterX);

//             if (dx > sensor.getSideRange()) continue;

//             float dy = entity.getY() - selfFrontY;
//             if (dy < 0f || dy > sensor.getForwardRange()) continue;

//             if (dy < bestDistance) {
//                 bestDistance = dy;
//                 nearest = entity;
//             }

//             if (entity instanceof Pedestrian && dy < nearestPedestrianDistance) {
//                 nearestPedestrianDistance = dy;
//             } else if (entity instanceof NPCCar && dy < nearestVehicleDistance) {
//                 nearestVehicleDistance = dy;
//             } else if ((entity instanceof Tree || entity instanceof StopSign) && dy < nearestObstacleDistance) {
//                 nearestObstacleDistance = dy;
//             }
//         }

//         boolean pedestrianAhead = nearestPedestrianDistance < sensor.getForwardRange();
//         boolean vehicleAhead = nearestVehicleDistance < sensor.getForwardRange();
//         boolean obstacleAhead = nearestObstacleDistance < sensor.getForwardRange();

//         return new PerceptionSnapshot(
//                 pedestrianAhead,
//                 vehicleAhead,
//                 obstacleAhead,
//                 bestDistance,
//                 nearest,
//                 nearestPedestrianDistance,
//                 nearestVehicleDistance,
//                 nearestObstacleDistance
//         );
//     }
// }

package io.github.raesleg.game.movement;

import java.util.List;

import io.github.raesleg.engine.entity.Entity;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.game.entities.misc.Pedestrian;
import io.github.raesleg.game.entities.misc.StopSign;
import io.github.raesleg.game.entities.misc.Tree;
import io.github.raesleg.game.entities.vehicles.NPCCar;

/**
 * Reads nearby entities and builds a lightweight perception snapshot for NPC AI.
 */
public class AIPerceptionService {

    private final EntityManager entityManager;

    public AIPerceptionService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public PerceptionSnapshot scan(NPCCar self) {
        SensorComponent sensor = self.getSensor();
        float defaultDistance = sensor.getForwardRange();

        float nearestPedestrianDistance = defaultDistance;
        float nearestVehicleDistance = defaultDistance;
        float nearestObstacleDistance = defaultDistance;
        float nearestDistance = defaultDistance;
        Entity nearestEntity = null;

        List<Entity> entities = entityManager.getSnapshot();

        float selfCenterX = self.getX() + self.getW() * 0.5f;
        float selfFrontY = self.getY() + self.getH();

        for (Entity entity : entities) {
            if (entity == self) {
                continue;
            }

            float otherCenterX = entity.getX() + entity.getW() * 0.5f;
            float dx = Math.abs(otherCenterX - selfCenterX);
            if (dx > sensor.getSideRange()) {
                continue;
            }

            float dy = entity.getY() - selfFrontY;
            if (dy < 0f || dy > sensor.getForwardRange()) {
                continue;
            }

            if (dy < nearestDistance) {
                nearestDistance = dy;
                nearestEntity = entity;
            }

            if (entity instanceof Pedestrian && dy < nearestPedestrianDistance) {
                nearestPedestrianDistance = dy;
            } else if (entity instanceof NPCCar && dy < nearestVehicleDistance) {
                nearestVehicleDistance = dy;
            } else if ((entity instanceof Tree || entity instanceof StopSign) && dy < nearestObstacleDistance) {
                nearestObstacleDistance = dy;
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
                nearestObstacleDistance
        );
    }
}
