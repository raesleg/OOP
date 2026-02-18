package io.github.raesleg.engine.movement;

import com.badlogic.gdx.math.Vector2;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.TextureObject;
import io.github.raesleg.engine.io.ControlSource;
import io.github.raesleg.engine.physics.PhysicsBody;

public class MovableEntity extends TextureObject implements IMovable {

    private ControlSource controls;
    private PhysicsBody body;
    private MovementModel movementModel;

    public MovableEntity(
            String filename,
            float x, float y,
            float width, float height,
            ControlSource controls,
            MovementModel movementModel,
            PhysicsBody body) {
        super(filename, x, y, width, height);
        this.controls = controls;
        this.movementModel = movementModel;

        this.body = body;
        this.body.setUserData(this);
    }

    /**
     * Delegates to the ControlSource interface (polymorphic dispatch)
     * instead of storing a flag from instanceof.
     */
    public boolean isAIControlled() {
        return !controls.isPlayerControlled();
    }

    /* getter functions for collision handler and resolver */
    public PhysicsBody getPhysicsBody() {
        return body;
    }

    public ControlSource getControls() {
        return controls;
    }

    /**
     * Delegates to ControlSource — avoids Law of Demeter violations
     * in callers that only need the input axis values.
     */
    public float getInputX(float dt) {
        return controls.getX(dt);
    }

    public float getInputY(float dt) {
        return controls.getY(dt);
    }

    public MovementModel getMovementModel() {
        return movementModel;
    }

    public boolean isMoving() {
        Vector2 v = body.getVelocity();
        return Math.abs(v.x) > 0.05f || Math.abs(v.y) > 0.05f;
    }

    @Override
    public void move(float dt) {
        if (movementModel != null) {
            movementModel.step(this, dt);
        }
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        syncPosition(); // sync even when not actively moving
    }

    /* private methods */
    private void syncPosition() {
        Vector2 p = body.getPosition();
        setX(p.x * Constants.PPM - getW() / 2f);
        setY(p.y * Constants.PPM - getH() / 2f);
    }
}
