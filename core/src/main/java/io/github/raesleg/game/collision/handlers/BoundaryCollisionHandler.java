package io.github.raesleg.game.collision.handlers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.Entity;
import io.github.raesleg.engine.entity.IFlashable;
import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.collision.CollisionEntityUtils;
import io.github.raesleg.game.scene.RoadRenderer;

/**
 * Handles player collisions with road boundaries.
 */

public class BoundaryCollisionHandler {

    private final SoundDevice soundManager;
    private static final float BOUNDARY_BOUNCE_FORCE = 15f;

    public BoundaryCollisionHandler(SoundDevice soundManager) {
        this.soundManager = soundManager;
    }

    // Checks if this handler can process the collision.
    public boolean canHandle(Entity a, Entity b) {
        boolean oneIsNull = (a == null) != (b == null); // XOR: exactly one is null
        MovableEntity player = CollisionEntityUtils.getPlayerEntity(a, b);
        // boolean hasNull = (a == null || b == null);
        return player != null && oneIsNull;
    }

    // Handles the boundary collision.
    public void handleBegin(Entity entityA, Entity entityB) {
        MovableEntity player = CollisionEntityUtils.getPlayerEntity(entityA, entityB);
        if (player == null)
            return;

        // Trigger flash effect
        if (player instanceof IFlashable flashable) {
            flashable.triggerDamageFlash();
        }

        // Calculate bounce direction
        PhysicsBody body = player.getPhysicsBody();
        Vector2 velocity = body.getVelocity();

        Vector2 bounceDirection;
        if (velocity.len2() > 0.01f) {
            // Bounce opposite to movement direction
            bounceDirection = velocity.cpy().nor().scl(-1f);
        } else {
            // If stationary, push toward center of road
            Vector2 playerPos = body.getPosition();
            float roadCenterX = (RoadRenderer.ROAD_LEFT + RoadRenderer.ROAD_RIGHT) / 2f / Constants.PPM;
            bounceDirection = new Vector2(roadCenterX - playerPos.x, 0f).nor();
        }

        // Apply bounce impulse
        Vector2 bounceImpulse = bounceDirection.scl(BOUNDARY_BOUNCE_FORCE);
        body.applyImpulseAtCenter(bounceImpulse);

        // Play sound effect
        if (soundManager != null) {
            soundManager.playSound("boundary_hit", 0.8f);
        }

        Gdx.app.log("BoundaryCollisionHandler", "Boundary bounce applied");
    }
}