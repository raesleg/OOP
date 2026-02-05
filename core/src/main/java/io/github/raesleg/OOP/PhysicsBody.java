package io.github.raesleg.OOP;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

public class PhysicsBody {

    private final Body body;

    public PhysicsBody(PhysicsWorld physics, BodyDef.BodyType type, float x, float y) {
        BodyDef def = new BodyDef();
        def.type = type;
        def.position.set(x, y);

        body = physics.raw().createBody(def);

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

    public Vector2 getVelocity() {
        return body.getLinearVelocity();
    }

    public void setLinearDamping(float d) {
        body.setLinearDamping(d);
    }

    public void setVelocity(float vx, float vy) {
        body.setLinearVelocity(vx, vy);
    }

    public void applyForce(float fx, float fy) {
        body.applyForceToCenter(fx, fy, true);
    }

    public Vector2 getPosition() {
        return body.getPosition();
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
