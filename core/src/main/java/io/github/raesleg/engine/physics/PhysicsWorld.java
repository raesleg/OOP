package io.github.raesleg.engine.physics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import io.github.raesleg.engine.movement.MotionProfile;

public class PhysicsWorld implements IPhysics {

    private final World world;
    // reuse these objects to avoid repeated new
    private final BodyDef bd = new BodyDef();
    private final FixtureDef fix = new FixtureDef();
    private final PolygonShape shape = new PolygonShape();

    private static BodyDef.BodyType map(IPhysics.BodyType t) {
        return switch (t) {
            case STATIC -> BodyDef.BodyType.StaticBody;
            case DYNAMIC -> BodyDef.BodyType.DynamicBody;
            case KINEMATIC -> BodyDef.BodyType.KinematicBody;
        };
    }

    public PhysicsWorld(Vector2 gravity) {
        this.world = new World(gravity, true);
    }

    @Override
    public void step(float dt) {
        world.step(dt, 6, 2);
    }

    @Override
    public void setContactListener(ContactListener listener) {
        world.setContactListener(listener);
    }

    @Override
    public PhysicsBody createBody(
            IPhysics.BodyType type,
            float xM, float yM,
            float halfW, float halfH,
            float density,
            float friction,
            boolean isSensor,
            Object userData
    ) {
        bd.type = map(type);
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

    @Override
    public void destroy(PhysicsBody pb) {
        if (pb == null) return;

        Body b = pb._raw();
        if (b != null) {
            world.destroyBody(b);
            pb._clearRaw();
        }
    }

    @Override
    public Vector2 getGravity() {
        return world.getGravity();
    }

    @Override
    public void dispose() {
        world.dispose();
        shape.dispose();
    }

    // Optional helper: bounds (safe, no shared fields)
    public void createBoundsPixels(float screenWidthPx, float screenHeightPx, float ppm) {
        float w = screenWidthPx / ppm;
        float h = screenHeightPx / ppm;
        float t = 0.2f;

        createWall(w / 2f, -t / 2f, w / 2f, t / 2f);      // bottom
        createWall(w / 2f, h + t / 2f, w / 2f, t / 2f);   // top
        createWall(-t / 2f, h / 2f, t / 2f, h / 2f);      // left
        createWall(w + t / 2f, h / 2f, t / 2f, h / 2f);   // right
    }

    private void createWall(float centerX, float centerY, float halfW, float halfH) {
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(centerX, centerY);

        Body b = world.createBody(bd);

        shape.setAsBox(halfW, halfH);

        fix.shape = shape;
        fix.density = 0f;
        fix.friction = 0.4f;
        fix.restitution = 0.2f;
        fix.isSensor = false;

        b.createFixture(fix);
    }

    public PhysicsBody createMotionZone(float centerX, float centerY,
                                        float halfW, float halfH,
                                        MotionProfile profile) {

        return createBody(
                IPhysics.BodyType.STATIC,
                centerX, centerY,
                halfW, halfH,
                0f, 0f,
                true,      
                profile     
        );
    }
}


