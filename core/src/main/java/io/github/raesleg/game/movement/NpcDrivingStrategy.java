// // package io.github.raesleg.game.movement;

// // import io.github.raesleg.engine.movement.MovableEntity;
// // import io.github.raesleg.engine.movement.MovementStrategy;
// // import io.github.raesleg.game.entities.vehicles.NPCCar;

// // public class NpcDrivingStrategy implements MovementStrategy {

// //     private final AIPerceptionService perceptionService;
// //     private DrivingDecision lastDecision = DrivingDecision.cruise();
// //     private MovableEntity lastEntity;
// //     private boolean decisionPendingForYAxis;

// //     public NpcDrivingStrategy(AIPerceptionService perceptionService) {
// //         this.perceptionService = perceptionService;
// //         this.lastEntity = null;
// //         this.decisionPendingForYAxis = false;
// //     }

// //     @Override
// //     public float getX(MovableEntity entity, float dt) {
// //         ensureDecision(entity);
// //         return lastDecision.steer();
// //     }

// //     @Override
// //     public float getY(MovableEntity entity, float dt) {
// //         ensureDecision(entity);
// //         decisionPendingForYAxis = false;
// //         return lastDecision.throttle();
// //     }

// //     private void ensureDecision(MovableEntity entity) {
// //         if (entity == lastEntity && decisionPendingForYAxis) {
// //             return;
// //         }

// //         lastEntity = entity;
// //         decisionPendingForYAxis = true;

// //         if (!(entity instanceof NPCCar npc)) {
// //             lastDecision = DrivingDecision.stop();
// //             return;
// //         }

// //         PerceptionSnapshot snapshot = perceptionService.scan(npc);

// //         float stopDistance = npc.getSensor().getStopDistance();
// //         float followDistance = npc.getSensor().getFollowDistance();

// //         boolean pedestrianTooClose = snapshot.pedestrianAhead()
// //                 && snapshot.nearestPedestrianDistance() <= stopDistance;

// //         boolean emergencyAnyObject = snapshot.nearestDistance() <= stopDistance * 0.7f;

// //         boolean shouldFollow =
// //                 (snapshot.vehicleAhead() && snapshot.nearestVehicleDistance() <= followDistance)
// //                         || (snapshot.obstacleAhead() && snapshot.nearestObstacleDistance() <= followDistance);

// //         if (pedestrianTooClose || emergencyAnyObject) {
// //             lastDecision = DrivingDecision.stop();
// //         } else if (shouldFollow) {
// //             lastDecision = DrivingDecision.slow();
// //         } else {
// //             lastDecision = DrivingDecision.cruise();
// //         }
// //     }
// // }

// package io.github.raesleg.game.movement;

// import io.github.raesleg.engine.movement.MovableEntity;
// import io.github.raesleg.engine.movement.MovementStrategy;
// import io.github.raesleg.game.entities.vehicles.NPCCar;

// /**
//  * AI strategy decides desired steer/throttle from perception.
//  * Movement execution is still handled by CarMovementModel.
//  */
// public class NpcDrivingStrategy implements MovementStrategy {

//     private final AIPerceptionService perceptionService;

//     private MovableEntity lastEntity;
//     private boolean decisionConsumedForY;
//     private float steer;
//     private float throttle;

//     public NpcDrivingStrategy(AIPerceptionService perceptionService) {
//         this.perceptionService = perceptionService;
//     }

//     @Override
//     public float getX(MovableEntity entity, float dt) {
//         ensureDecision(entity);
//         return steer;
//     }

//     @Override
//     public float getY(MovableEntity entity, float dt) {
//         ensureDecision(entity);
//         decisionConsumedForY = false;
//         return throttle;
//     }

//     private void ensureDecision(MovableEntity entity) {
//         if (entity == lastEntity && decisionConsumedForY) {
//             return;
//         }

//         lastEntity = entity;
//         decisionConsumedForY = true;

//         if (!(entity instanceof NPCCar npc)) {
//             steer = 0f;
//             throttle = 0f;
//             return;
//         }

//         PerceptionSnapshot snapshot = perceptionService.scan(npc);

//         float stopDistance = npc.getSensor().getStopDistance();
//         float followDistance = npc.getSensor().getFollowDistance();

//         boolean pedestrianTooClose = snapshot.pedestrianAhead()
//                 && snapshot.nearestPedestrianDistance() <= stopDistance;

//         boolean obstacleEmergency = snapshot.nearestDistance() <= stopDistance * 0.7f;

//         boolean shouldFollow =
//                 (snapshot.vehicleAhead() && snapshot.nearestVehicleDistance() <= followDistance)
//                         || (snapshot.obstacleAhead() && snapshot.nearestObstacleDistance() <= followDistance);

//         steer = 0f;

//         if (pedestrianTooClose || obstacleEmergency) {
//             throttle = 0f;
//         } else if (shouldFollow) {
//             throttle = 0.4f;
//         } else {
//             throttle = 1f;
//         }
//     }
// }

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
