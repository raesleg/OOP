package io.github.raesleg.game.collision;

import com.badlogic.gdx.math.Vector2;

import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.entities.misc.Pedestrian;

/**
 * Collision aftermath behaviour for a pedestrian.
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

    // Initiate hit reaction: apply knockback impulse and start spin animation for 2.5 seconds
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

    // Update spin rotation and countdown timer; finish reaction and mark pedestrian expired when complete
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

    // Check if hit reaction is currently playing
    public boolean isActive() {
        return active;
    }

    // Check if hit reaction has completed and pedestrian has been marked for removal
    public boolean isFinished() {
        return finished;
    }
}