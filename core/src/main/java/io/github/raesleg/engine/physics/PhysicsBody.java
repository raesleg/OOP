package io.github.raesleg.engine.physics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

import io.github.raesleg.engine.Constants;

/**
 * PhysicsBody \u2014 Wraps a Box2D {@link Body} with convenience helpers.
 *
 * <h3>Fix: Ghost Shapes (Data Integrity)</h3>
 * The constructor now accepts {@code widthPx} and {@code heightPx}
 * (in render-pixels) and converts them to Box2D meters using
 * {@link Constants#PPM}. A {@link PolygonShape} sized with
 * {@code setAsBox(halfW, halfH)} replaces the old hard-coded
 * {@code CircleShape.setRadius(0.25f)}, so the physics hitbox
 * actually matches the entity\u2019s sprite dimensions.
 */
public class PhysicsBody {

    private final PhysicsWorld physicsWorld;
    private Body body;

    public PhysicsBody(PhysicsWorld physicsWorld, BodyDef.BodyType type,
            float x, float y, float widthPx, float heightPx) {
        this.physicsWorld = physicsWorld;

        BodyDef def = new BodyDef();
        def.type = type;
        def.position.set(x, y);

        body = physicsWorld.raw().createBody(def);

        // Shape sized to match the entity sprite (pixels → meters)
        float halfW = (widthPx / Constants.PPM) / 2f;
        float halfH = (heightPx / Constants.PPM) / 2f;

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(halfW, halfH);

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
