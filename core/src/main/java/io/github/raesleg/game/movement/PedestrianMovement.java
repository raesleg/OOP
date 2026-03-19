// package io.github.raesleg.game.movement;

// import com.badlogic.gdx.math.MathUtils;
// import io.github.raesleg.engine.movement.MovementModel;
// import io.github.raesleg.engine.physics.PhysicsBody;

// public class PedestrianMovement implements MovementModel {

//     private final float direction; // -1 = right to left, +1 = left to right
//     private float crossingSpeed;
//     private float maxCrossingSpeed;
//     private float acceleration;
//     private float linearDamping;

//     public PedestrianMovement(float direction) {
//         this(direction, 1.4f, 2.4f, 4.5f, 3.0f);
//     }

//     public PedestrianMovement(float direction,
//                               float crossingSpeed,
//                               float maxCrossingSpeed,
//                               float acceleration,
//                               float linearDamping) {
//         this.direction = Math.signum(direction) == 0f ? 1f : Math.signum(direction);
//         this.crossingSpeed = crossingSpeed;
//         this.maxCrossingSpeed = maxCrossingSpeed;
//         this.acceleration = acceleration;
//         this.linearDamping = linearDamping;
//     }

//     @Override
//     public void step(PhysicsBody body, float x, float y, float dt) {
//         if (body == null) return;

//         // Pedestrians mainly cross sideways in your game.
//         // Vertical world motion should be handled by scene scroll/spawn logic, not here.
//         crossingSpeed = Math.min(maxCrossingSpeed, crossingSpeed + acceleration * dt * 0.2f);

//         float vx = direction * crossingSpeed;
//         float vy = 0f;

//         body.setLinearDamping(linearDamping);
//         body.setAngularVelocity(0f);
//         body.setVelocity(vx, vy);
//     }

//     @Override
//     public void onEnterZone(PhysicsBody body, Object zoneTuning) {
//         if (zoneTuning instanceof MotionTuning tuning) {
//             // puddles / friction zones can slow crossings slightly
//             crossingSpeed = MathUtils.clamp(crossingSpeed * 0.85f, 0.6f, maxCrossingSpeed);
//             if (body != null) {
//                 body.setLinearDamping(Math.max(linearDamping, tuning.getLinearDamping()));
//             }
//         }
//     }

//     @Override
//     public void onExitZone(PhysicsBody body) {
//         if (body != null) {
//             body.setLinearDamping(linearDamping);
//         }
//     }

//     public void setCrossingSpeed(float crossingSpeed) {
//         this.crossingSpeed = MathUtils.clamp(crossingSpeed, 0.2f, maxCrossingSpeed);
//     }

//     public void setMaxCrossingSpeed(float maxCrossingSpeed) {
//         this.maxCrossingSpeed = Math.max(0.5f, maxCrossingSpeed);
//     }
// }