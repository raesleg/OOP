package io.github.raesleg.engine;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;

public class PhysicsWorld {

    private World world;

    public PhysicsWorld(Vector2 gravity) {
        world = new World(gravity, true);
    }

    public void step(float dt) {
        world.step(dt, 6, 2);
    }

    World raw() {
        return world;
    }

    public void dispose() {
        world.dispose();
    }

    public void createBoundsPixels(float screenWidthPx, float screenHeightPx, float ppm) {
        float w = screenWidthPx / ppm;   // meters
        float h = screenHeightPx / ppm;  // meters
        float t = 0.2f;                  // wall thickness (meters)

        // Bottom
        createWall(w / 2f, -t / 2f, w, t);
        // Top
        createWall(w / 2f, h + t / 2f, w, t);
        // Left
        createWall(-t / 2f, h / 2f, t, h);
        // Right
        createWall(w + t / 2f, h / 2f, t, h);
    }

    private void createWall(float centerX, float centerY, float halfWidthTimes2, float halfHeightTimes2) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(centerX, centerY);

        Body b = world.createBody(bd);

        PolygonShape box = new PolygonShape();
        box.setAsBox(halfWidthTimes2 / 2f, halfHeightTimes2 / 2f);

        FixtureDef fd = new FixtureDef();
        fd.shape = box;
        fd.friction = 0.4f;
        fd.restitution = 0.2f; // bounce a little; set 0 for no bounce

        b.createFixture(fd);
        box.dispose();
    }

    public void createMotionZone(float centerX, float centerY, float halfW, float halfH, MotionProfile profile) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(centerX, centerY);

        Body zoneBody = raw().createBody(bd);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(halfW, halfH);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.isSensor = true;

        Fixture f = zoneBody.createFixture(fd);
        f.setUserData(profile); 

        shape.dispose();
    }
}

