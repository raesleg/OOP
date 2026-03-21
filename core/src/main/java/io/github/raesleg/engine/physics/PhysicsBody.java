package io.github.raesleg.engine.physics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

public class PhysicsBody {

    private final PhysicsWorld world;
    private Body body;

    PhysicsBody(PhysicsWorld world, Body body) {
        this.world = world;
        this.body = body;
    }

    private boolean destroyed;

    void _clearRaw() {
        body = null;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void destroy() {
        if (!destroyed && body != null) {
            destroyed = true;
            world.destroy(body);
            _clearRaw();
        }
    }

    /* setters and getters for movement */
    public void setLinearDamping(float d) {
        if (body == null)
            return;
        body.setLinearDamping(d);
    }

    public void setVelocity(float vx, float vy) {
        if (body == null)
            return;
        body.setLinearVelocity(vx, vy);
    }

    public Vector2 getVelocity() {
        if (body == null)
            return Vector2.Zero;
        return body.getLinearVelocity();
    }

    public Vector2 getPosition() {
        if (body == null)
            return Vector2.Zero;
        return body.getPosition();
    }

    public float getMass() {
        if (body == null)
            return 0f;
        return body.getMass();
    }

    public Vector2 getWorldVector(Vector2 localVector) {
        if (body == null)
            return Vector2.Zero;
        return body.getWorldVector(localVector);
    }

    public void applyLinearImpulse(Vector2 impulse) {
        if (body == null)
            return;
        body.applyLinearImpulse(impulse, body.getWorldCenter(), true);
    }

    // public float getAngleRadians() {
    // return body.getAngle();
    // }

    // public void setTransform(float x, float y, float angleRadians) {
    // body.setTransform(x, y, angleRadians);
    // }

    // public void setAngularDamping(float damping) {
    // body.setAngularDamping(damping);
    // }

    public void setAngularVelocity(float angularVelocity) {
        if (body == null)
            return;
        body.setAngularVelocity(angularVelocity); // added
    }

    public float getAngularVelocity() {
        if (body == null)
            return 0f;
        return body.getAngularVelocity(); // added
    }

    /* methods for collision */
    public void setUserData(Object data) {
        if (body == null)
            return;
        body.setUserData(data);
    }

    public void applyImpulseAtCenter(Vector2 impulse) {
        if (body == null)
            return;
        body.applyLinearImpulse(impulse, body.getWorldCenter(), true);
    }

    // added method for NPC cars - TBC
    public void setPosition(float x, float y) {
        if (body != null) {
            body.setTransform(x, y, body.getAngle());
        }
    }

    /**
     * Enables continuous collision detection (CCD) on this body.
     * Prevents fast-moving bodies from tunnelling through thin sensors.
     */
    public void setBullet(boolean bullet) {
        if (body != null) {
            body.setBullet(bullet);
        }
    }
}
