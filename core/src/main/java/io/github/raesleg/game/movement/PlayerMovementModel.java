package io.github.raesleg.game.movement;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import io.github.raesleg.engine.movement.MovementModel;
import io.github.raesleg.engine.physics.PhysicsBody;

public class PlayerMovementModel implements MovementModel {

    private MotionTuning tuning = MotionTuning.DEFAULT;

    // arcade lane-steering feel
    private float maxLateralSpeed = 12.5f;
    private float steeringResponse = 22f;
    private float steeringDeadzone = 0.08f;

    // Slide recovery — puddle effect lingers after leaving the zone
    private boolean isSliding = false;
    private float slideTimer = 0f;
    private static final float SLIDE_RECOVERY_TIME = 1.5f;

    @Override
    public void step(PhysicsBody body, float x, float y, float dt) {
        if (body == null)
            return;

        float inputX = MathUtils.clamp(x, -1f, 1f);

        if (Math.abs(inputX) < steeringDeadzone) {
            inputX = 0f;
        }

        Vector2 velocity = body.getVelocity().cpy();

        float targetVx = inputX * maxLateralSpeed;

        // Compute grip ratio — blend back from LOW_TRACTION after leaving puddle
        float gripRatio;
        if (isSliding) {
            slideTimer -= dt;
            if (slideTimer <= 0) {
                isSliding = false;
                slideTimer = 0;
                gripRatio = 1.0f;
            } else {
                float blend = slideTimer / SLIDE_RECOVERY_TIME;
                float lowGrip = MotionTuning.LOW_TRACTION.getLateralGrip()
                        / MotionTuning.DEFAULT.getLateralGrip();
                gripRatio = lowGrip + (1.0f - lowGrip) * (1f - blend);
            }
        } else {
            gripRatio = tuning.getLateralGrip() / MotionTuning.DEFAULT.getLateralGrip();
        }

        float effectiveResponse = steeringResponse * gripRatio;

        // smooth but responsive horizontal control
        float vx = approach(velocity.x, targetVx, effectiveResponse * dt);

        // DO NOT use player body Y speed for forward road motion anymore
        float vy = 0f;

        body.setLinearDamping(tuning.getLinearDamping());
        body.setAngularVelocity(0f);
        body.setVelocity(vx, vy);
    }

    @Override
    public void onEnterZone(PhysicsBody body, Object zoneTuning) {
        if (zoneTuning instanceof MotionTuning mt) {
            this.tuning = mt;
            if (body != null) {
                body.setLinearDamping(mt.getLinearDamping());
            }
        }
    }

    @Override
    public void onExitZone(PhysicsBody body) {
        if (tuning == MotionTuning.LOW_TRACTION) {
            isSliding = true;
            slideTimer = SLIDE_RECOVERY_TIME;
        }
        this.tuning = MotionTuning.DEFAULT;
        if (body != null) {
            body.setLinearDamping(tuning.getLinearDamping());
        }
    }

    private float approach(float current, float target, float amount) {
        if (current < target)
            return Math.min(current + amount, target);
        if (current > target)
            return Math.max(current - amount, target);
        return target;
    }
}