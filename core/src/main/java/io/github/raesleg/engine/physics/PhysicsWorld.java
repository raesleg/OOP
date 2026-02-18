package io.github.raesleg.engine.physics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

public class PhysicsWorld {

    // referencing from box2d 
    private final BodyDef bd = new BodyDef();
    private final FixtureDef fix = new FixtureDef();
    private final PolygonShape shape = new PolygonShape();    
    private final World world;

    public PhysicsWorld(Vector2 gravity) {
        this.world = new World(gravity, true);
    }

    public void step(float dt) {
        world.step(dt, 6, 2);
    }

    public void setContactListener(ContactListener listener) {
        world.setContactListener(listener);
    }

    public PhysicsBody createBody(
            BodyDef.BodyType type,
            float xM, float yM,
            float halfW, float halfH,
            float density,
            float friction,
            boolean isSensor,
            Object userData
    ) {
        bd.type = type;
        bd.position.set(xM, yM);

        Body body = world.createBody(bd);

        shape.setAsBox(halfW, halfH);

        fix.shape = shape;
        fix.density = density;
        fix.friction = friction;
        fix.isSensor = isSensor;
        fix.restitution = 0f;  

        body.createFixture(fix);
        body.setUserData(userData);

        return new PhysicsBody(this, body);
    }

    public void destroy(Body body) {
        world.destroyBody(body);
    }

    public void dispose() {
        world.dispose();
        shape.dispose();
    }
}


