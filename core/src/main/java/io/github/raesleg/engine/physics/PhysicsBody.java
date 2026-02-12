package io.github.raesleg.engine.physics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

public class PhysicsBody {

    private PhysicsWorld physicsWorld;
    private Body body;

    public PhysicsBody(PhysicsWorld physicsWorld, BodyDef.BodyType type, float x, float y) {
        BodyDef def = new BodyDef();
        def.type = type;
        def.position.set(x, y);

        body = physicsWorld.raw().createBody(def);

        CircleShape shape = new CircleShape();
        //this line affects hitbox
        shape.setRadius(0.25f); // meters

        FixtureDef fix = new FixtureDef();
        fix.shape = shape;
        fix.density = 1f;
        fix.friction = 0.3f;

        body.createFixture(fix);
        shape.dispose();
    }

    public void destroy() {
        if (body != null) {
            physicsWorld.raw().destroyBody(body);
            body = null;
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
    // explosion queries
    public Body getBody() {
        return body;
    }
}
