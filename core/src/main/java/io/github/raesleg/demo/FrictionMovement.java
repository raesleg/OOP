package io.github.raesleg.demo;

import com.badlogic.gdx.math.Vector2;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.movement.MovementModel;
import io.github.raesleg.engine.physics.PhysicsBody;

public class FrictionMovement implements MovementModel {

    private final Vector2 v1 = new Vector2();
    private final Vector2 v2 = new Vector2();

    private MotionTuning base;
    private MotionTuning tuning;
    
    private int zoneContacts = 0;

    public FrictionMovement(MotionTuning base) {
        this.base = base;
        this.tuning = base;
    }

    private void applyMotionProfile(PhysicsBody body, MotionTuning t) {
        if (t == null) return;
        this.tuning = t;
        body.setLinearDamping(t.linearDamping);
    }

    public void onEnterZone(PhysicsBody body, MotionTuning zoneTuning) {
        zoneContacts++;
        applyMotionProfile(body, zoneTuning);
    }

    public void onExitZone(PhysicsBody body) {
        zoneContacts--;
        if (zoneContacts <= 0) {
            zoneContacts = 0;
            applyMotionProfile(body, base);
        }
    }

    @Override
    public void step(MovableEntity e, float dt) {
        PhysicsBody body = e.getPhysicsBody();

        float xAxis = e.getControls().getX(dt);
        float yAxis = e.getControls().getY(dt);

        v1.set(xAxis, yAxis);
        if (v1.len2() > 1f) v1.nor();

        // target velocity
        v2.set(v1).scl(tuning.maxSpeed);

        Vector2 curVel = body.getVelocity();
        v1.set(v2).sub(curVel);

        float maxDelta = tuning.maxForce * dt;
        if (v1.len2() > maxDelta * maxDelta) v1.nor().scl(maxDelta);

        body.setVelocity(curVel.x + v1.x, curVel.y + v1.y);

        applyLateralGrip(body);
    }

    private void applyLateralGrip(PhysicsBody body) {
        body.getWorldVector(v1.set(0, 1));
        Vector2 vel = body.getVelocity();

        float lateralSpeed = vel.dot(v1);
        v2.set(v1).scl(lateralSpeed);

        float grip = tuning.lateralGrip;

        v2.scl(-body.getMass() * grip);

        float maxImpulse = 1.5f * grip;
        if (v2.len2() > maxImpulse * maxImpulse) v2.nor().scl(maxImpulse);

        body.applyLinearImpulse(v2);
    }
}

