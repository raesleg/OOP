package io.github.raesleg.game.collision;

import com.badlogic.gdx.math.Vector2;

import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.entities.Pedestrian;

/**
 * Collision aftermath behaviour for a pedestrian.
 * This is NOT part of the pedestrian entity itself.
 */
public class PedestrianHitReaction {

    private boolean active;
    private boolean finished;
    private float remainingTime;
    private float rotation;

    private static final float HIT_DURATION = 2.5f;
    private static final float SPIN_SPEED = 720f;

    public PedestrianHitReaction() {
        this.active = false;
        this.finished = false;
        this.remainingTime = 0f;
        this.rotation = 0f;
    }

    public void trigger(Pedestrian pedestrian, Vector2 knockbackDirection, float force) {
        if (active || finished || pedestrian == null) {
            return;
        }

        active = true;
        remainingTime = HIT_DURATION;
        rotation = 0f;

        PhysicsBody body = pedestrian.getPhysicsBody();
        if (body != null) {
            body.setLinearDamping(0.5f);

            Vector2 impulse = new Vector2(
                    knockbackDirection.x * force * 0.5f,
                    Math.abs(force) * 2.5f);

            body.applyImpulseAtCenter(impulse);
        }
    }

    public void update(Pedestrian pedestrian, float deltaTime) {
        if (!active || finished || pedestrian == null) {
            return;
        }

        remainingTime -= deltaTime;
        rotation += SPIN_SPEED * deltaTime;
        if (rotation >= 360f) {
            rotation -= 360f;
        }

        pedestrian.setRenderRotation(rotation);
        pedestrian.syncSpriteFromBody();

        if (remainingTime <= 0f) {
            active = false;
            finished = true;
            pedestrian.setRenderRotation(0f);
            pedestrian.markExpired();
        }
    }

    public boolean isActive() {
        return active;
    }

    public boolean isFinished() {
        return finished;
    }
}