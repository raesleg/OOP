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

    void _clearRaw() {
        body = null;
    }

    public void destroy() {
        if (body != null) {
            world.destroy(body);
            _clearRaw();
        }
    }

    /* setters and getters for movement */
    public void setLinearDamping(float d) {
        body.setLinearDamping(d);
    }

    public void setVelocity(float vx, float vy) {
        body.setLinearVelocity(vx, vy);
    }

    public Vector2 getVelocity() {
        return body.getLinearVelocity();
    }

    public Vector2 getPosition() {
        return body.getPosition();
    }

    public float getMass() {
        return body.getMass();
    }

    public Vector2 getWorldVector(Vector2 localVector) {
        return body.getWorldVector(localVector);
    }

    public void applyLinearImpulse(Vector2 impulse) {
        body.applyLinearImpulse(impulse, body.getWorldCenter(), true);
    }

    /* methods for collision */
    public void setUserData(Object data) {
        body.setUserData(data);
    }

    public void applyImpulseAtCenter(Vector2 impulse) {
        body.applyLinearImpulse(impulse, body.getWorldCenter(), true);
    }
}
