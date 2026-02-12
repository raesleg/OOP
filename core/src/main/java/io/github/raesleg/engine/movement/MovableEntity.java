package io.github.raesleg.engine.movement;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.TextureObject;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.engine.physics.PhysicsWorld;

public class MovableEntity extends TextureObject implements IMovable {

    private IControllable controls;
    private PhysicsBody body;

    private Vector2 v1 = new Vector2();
    private Vector2 v2 = new Vector2();

    private MotionProfile base;
    private MotionProfile profile;

    private int zoneContacts = 0;

    public MovableEntity(
            PhysicsWorld physicsWorld,
            String filename,
            float x,
            float y,
            float width,
            float height,
            IControllable controls,
            MotionProfile base) {
        super(filename, x, y, width, height);
        this.controls = controls;

        float xm = (x + width / 2f) / Constants.PPM;
        float ym = (y + height / 2f) / Constants.PPM;

        this.body = new PhysicsBody(physicsWorld, BodyDef.BodyType.DynamicBody, xm, ym, width, height);
        this.body.setUserData(this); // link entity to physics body

        this.base = base;
        applyMotionProfile(base);
    }

    /* getter functions for collision */
    public PhysicsBody getPhysicsBody() {
        return body;
    }

    public IControllable getControls() {    
        return controls;
    }

    public void applyMotionProfile(MotionProfile p) {
        if (p == null)
            return;
        this.profile = p;
        body.setLinearDamping(p.linearDamping);
    }

    public void onEnterZone(MotionProfile mp) {
        zoneContacts++;
        System.out.println("ENTER zoneContacts=" + zoneContacts + " mp=" + mp);
        applyMotionProfile(mp);
    }

    public void onExitZone() {
        zoneContacts--;
        System.out.println("EXIT zoneContacts=" + zoneContacts);
        if (zoneContacts <= 0) {
            zoneContacts = 0;
            applyMotionProfile(base);
        }
    }

    @Override
    public void move(float dt) {
        ControlState s = controls.get(dt);

        // input direction
        v1.set(s.getxAxis(), s.getyAxis());
        if (v1.len2() > 1f) {
            v1.nor();
        }

        // target velocity (m/s)
        v2.set(v1).scl(profile.maxSpeed);

        // current velocity
        Vector2 curVel = body.getVelocity();

        // v1 = target - curr
        v1.set(v2).sub(curVel);

        float maxDelta = profile.maxForce * dt;
        if (v1.len2() > maxDelta * maxDelta) {
            v1.nor().scl(maxDelta);
        }

        // new velocity = current + delta
        body.setVelocity(curVel.x + v1.x, curVel.y + v1.y);
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        applyLateralGrip();
        syncPosition(); // sync even when not actively moving
    }

    private void syncPosition() {
        Vector2 p = body.getPosition();
        setX(p.x * Constants.PPM - getW() / 2f);
        setY(p.y * Constants.PPM - getH() / 2f);
    }

    private void applyLateralGrip() {
        // world-space right axis (0,1) rotated by body angle
        body.getWorldVector(v1.set(0, 1));

        Vector2 vel = body.getVelocity();

        // how much velocity is sideways
        float lateralSpeed = vel.dot(v1);

        // sideways velocity vector
        v2.set(v1).scl(lateralSpeed);

        float grip = profile.lateralGrip;

        // impulse to cancel sideways motion
        v2.scl(-body.getMass() * grip);

        // clamp
        float maxImpulse = 1.5f * grip;
        if (v2.len2() > maxImpulse * maxImpulse) {
            v2.nor().scl(maxImpulse);
        }

        body.applyLinearImpulse(v2);
    }
}
