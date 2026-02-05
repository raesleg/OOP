package io.github.raesleg.OOP;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;

public class MovableEntity extends TextureObject implements IMovable {

    private static final float PPM = 100f; // pixels per meter (render scale)
    private static final float MAX_SPEED = 3.5f; // meters/sec

    private final Controls.ControlSource controls;
    private final PhysicsBody body;

    public MovableEntity(
            PhysicsWorld physicsWorld,
            float startXPixels,
            float startYPixels,
            Controls.ControlSource controls,
            String filename,
            float width,
            float height) {
        super(filename, startXPixels, startYPixels, width, height); // Entity speed not used; physics controls speed
        this.controls = controls;

        // convert pixels -> meters for Box2D
        float xm = startXPixels / PPM;
        float ym = startYPixels / PPM;

        this.body = new PhysicsBody(physicsWorld, BodyDef.BodyType.DynamicBody, xm, ym);
    }

    @Override
    public void move(float dt) {
        Controls.ControlState c = controls.get(dt);
        System.out.println("move called dt=" + dt);

        // Use control direction to set velocity (simple top-down physics)
        float vx = c.xAxis() * MAX_SPEED;
        float vy = c.yAxis() * MAX_SPEED;

        body.setVelocity(vx, vy);

        // Sync Entity position from physics body for drawing
        Vector2 p = body.getPosition();
        setX(p.x * PPM);
        setY(p.y * PPM);
    }
}
