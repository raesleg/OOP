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

    @Override
    public void step(PhysicsBody body, float x, float y, float dt) {
        if (body == null) return;

        float inputX = MathUtils.clamp(x, -1f, 1f);

        if (Math.abs(inputX) < steeringDeadzone) {
            inputX = 0f;
        }

        Vector2 velocity = body.getVelocity().cpy();

        float targetVx = inputX * maxLateralSpeed;

        // smooth but responsive horizontal control
        float vx = approach(velocity.x, targetVx, steeringResponse * dt);

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
        this.tuning = MotionTuning.DEFAULT;
        if (body != null) {
            body.setLinearDamping(tuning.getLinearDamping());
        }
    }

    private float approach(float current, float target, float amount) {
        if (current < target) return Math.min(current + amount, target);
        if (current > target) return Math.max(current - amount, target);
        return target;
    }
}