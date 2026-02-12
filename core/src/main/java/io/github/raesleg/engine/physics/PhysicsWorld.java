package io.github.raesleg.engine.physics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;

import io.github.raesleg.engine.movement.MotionProfile;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;

public class PhysicsWorld {

    private World world;
    private BodyDef bd = new BodyDef();
    private PolygonShape shape = new PolygonShape();
    private FixtureDef fd = new FixtureDef();


    public PhysicsWorld(Vector2 gravity) {
        this.world = new World(gravity, true);
    }

    public void step(float dt) {
        world.step(dt, 6, 2);
    }

    World raw() {
        return world;
    }

    public void setContactListener(ContactListener listener) {
        world.setContactListener(listener);
    }

    public void createBoundsPixels(float screenWidthPx, float screenHeightPx, float ppm) {
        float w = screenWidthPx / ppm;   // meters
        float h = screenHeightPx / ppm;  // meters
        float t = 0.2f;                  // wall thickness (meters)

        createWall(w / 2f, -t / 2f, w, t); //bot
        createWall(w / 2f, h + t / 2f, w, t); //top
        createWall(-t / 2f, h / 2f, t, h); //left
        createWall(w + t / 2f, h / 2f, t, h); //right
    }

    private void createWall(float centerX, float centerY, float halfWidthTimes2, float halfHeightTimes2) {
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(centerX, centerY);

        Body b = world.createBody(bd);

        shape.setAsBox(halfWidthTimes2 / 2f, halfHeightTimes2 / 2f);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.friction = 0.4f;
        fd.restitution = 0.2f; // bounce a little; set 0 for no bounce

        b.createFixture(fd);
    }

    public void createMotionZone(float centerX, float centerY, float halfW, float halfH, MotionProfile profile) {
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(centerX, centerY);

        Body zoneBody = raw().createBody(bd);

        shape.setAsBox(halfW, halfH);

        fd.shape = shape;
        fd.isSensor = true;

        Fixture f = zoneBody.createFixture(fd);
        f.setUserData(profile); 
    }

    public void dispose() {
        world.dispose();
        shape.dispose();
    }

}

