// package io.github.raesleg.game.movement;

// import java.util.function.IntSupplier;
// import java.util.function.Supplier;

// import com.badlogic.gdx.math.MathUtils;
// import com.badlogic.gdx.math.Vector2;

// import io.github.raesleg.engine.movement.MovementModel;
// import io.github.raesleg.engine.physics.PhysicsBody;

// public class PoliceMovement implements MovementModel {

//     private final Supplier<Vector2> targetPositionSupplier;
//     private final IntSupplier rulesBrokenSupplier;

//     private final float baseForwardSpeed;
//     private final float maxForwardSpeed;
//     private final float baseLateralSpeed;
//     private final float maxLateralSpeed;
//     private final float behindOffset;
//     private final float snapDistance;

//     public PoliceMovement(Supplier<Vector2> targetPositionSupplier,
//                           IntSupplier rulesBrokenSupplier) {
//         this(targetPositionSupplier, rulesBrokenSupplier,
//                 3.2f, 9.5f,
//                 2.8f, 8.0f,
//                 2.2f, 0.12f);
//     }

//     public PoliceMovement(Supplier<Vector2> targetPositionSupplier,
//                           IntSupplier rulesBrokenSupplier,
//                           float baseForwardSpeed,
//                           float maxForwardSpeed,
//                           float baseLateralSpeed,
//                           float maxLateralSpeed,
//                           float behindOffset,
//                           float snapDistance) {
//         this.targetPositionSupplier = targetPositionSupplier;
//         this.rulesBrokenSupplier = rulesBrokenSupplier;
//         this.baseForwardSpeed = baseForwardSpeed;
//         this.maxForwardSpeed = maxForwardSpeed;
//         this.baseLateralSpeed = baseLateralSpeed;
//         this.maxLateralSpeed = maxLateralSpeed;
//         this.behindOffset = behindOffset;
//         this.snapDistance = snapDistance;
//     }

//     @Override
//     public void step(PhysicsBody body, float x, float y, float dt) {
//         if (body == null || targetPositionSupplier == null || rulesBrokenSupplier == null) return;

//         Vector2 target = targetPositionSupplier.get();
//         if (target == null) return;

//         Vector2 policePos = body.getPosition();
//         int rulesBroken = Math.max(0, rulesBrokenSupplier.getAsInt());

//         float aggression = getAggression01(rulesBroken);

//         // Ramp police pressure with rules broken
//         float forwardSpeed = MathUtils.lerp(baseForwardSpeed, maxForwardSpeed, aggression);
//         float lateralSpeed = MathUtils.lerp(baseLateralSpeed, maxLateralSpeed, aggression);
//         float steeringSharpness = MathUtils.lerp(2.8f, 6.0f, aggression);

//         // Police wants same lane as player
//         float dx = target.x - policePos.x;

//         float vx;
//         if (Math.abs(dx) < snapDistance) {
//             vx = 0f;
//         } else {
//             float desiredVx = Math.signum(dx) * lateralSpeed;
//             vx = approach(body.getVelocity().x, desiredVx, steeringSharpness * dt);
//         }

//         // Police stays behind at first, but becomes more aggressive and closes in harder
//         float desiredY = target.y - behindOffset + aggression * 1.2f;
//         float dy = desiredY - policePos.y;

//         float vy;
//         if (dy > 0f) {
//             // catch up
//             vy = forwardSpeed + Math.min(dy * 1.25f, 3.0f + aggression * 3.5f);
//         } else {
//             // if already too close, don't overshoot too much
//             vy = Math.max(1.2f, forwardSpeed * 0.55f);
//         }

//         body.setLinearDamping(MathUtils.lerp(2.5f, 1.2f, aggression));
//         body.setAngularVelocity(0f);
//         body.setVelocity(vx, vy);
//     }

//     private float getAggression01(int rulesBroken) {
//         // 0 rules = calm
//         // 8+ rules = max aggression
//         return Math.min(1f, rulesBroken / 8f);
//     }

//     private float approach(float current, float target, float amount) {
//         if (current < target) return Math.min(current + amount, target);
//         if (current > target) return Math.max(current - amount, target);
//         return target;
//     }
// }