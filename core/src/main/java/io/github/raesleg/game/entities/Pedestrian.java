package io.github.raesleg.game.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.IExpirable;
import io.github.raesleg.engine.entity.TextureObject;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.scene.RoadRenderer;

/**
 * Pedestrian — A sprite-based entity that crosses the road horizontally.
 * <p>
 * Pedestrians are part of the scrolling world (like NPCCar). They have a
 * kinematic Box2D body for collision detection with the player car.
 * The pedestrian walks from one side of the road to the other while
 * scrolling vertically with the road.
 * <p>
 * Implements {@link IExpirable} so
 * {@link io.github.raesleg.engine.entity.EntityManager}
 * auto-removes the pedestrian once it has crossed the road or scrolled off
 * screen.
 * <p>
 * <b>Design Pattern:</b> Flyweight (texture cache via TextureObject),
 * Observer (collision detected via CollisionManager → ICollisionListener).
 * <p>
 * <b>Engine/Game Boundary:</b> Extends engine's TextureObject. Lives
 * entirely in the game layer.
 */
public class Pedestrian extends TextureObject implements IExpirable {

    private final PhysicsBody body;
    private final float relativeY;
    private final float crossingDirection; // -1 = right to left, +1 = left to right
    private final float crossingSpeed;
    private boolean expired;
    private boolean activated;
    private float currentCrossingX;
    private boolean crossedSuccessfully;

    /* Flying state */
    private boolean isFlying;
    private float flyTimer;
    private float rotation;
    private static final float FLY_DURATION = 2.5f; // Fly for 2.5s before game over
    private static final float SPIN_SPEED = 720f; // Degrees / sec

    /**
     * Creates a pedestrian entity.
     *
     * @param relativeY         Y position relative to scroll offset (world coords)
     * @param crossingDirection -1 for right-to-left, +1 for left-to-right
     * @param crossingSpeed     horizontal speed in pixels per second
     * @param body              kinematic PhysicsBody for collision detection
     */
    public Pedestrian(float relativeY, float crossingDirection,
            float crossingSpeed, PhysicsBody body) {
        super("pedestrian.png", 0, 0, 80f, 80f);
        this.relativeY = relativeY;
        this.crossingDirection = crossingDirection;
        this.crossingSpeed = crossingSpeed;
        this.body = body;
        this.expired = false;
        this.activated = false;
        this.crossedSuccessfully = false;
        this.isFlying = false;
        this.flyTimer = 0f;
        this.rotation = 0f;

        // Start on the appropriate side of the road
        if (crossingDirection > 0) {
            this.currentCrossingX = RoadRenderer.ROAD_LEFT - getW();
        } else {
            this.currentCrossingX = RoadRenderer.ROAD_RIGHT + getW();
        }

        // Very high damping initially (test)
        body.setLinearDamping(999f);
        body.setUserData(this);
    }

    /**
     * Call when pedestrian is hit by player car
     * Switches from walking mode to flying mode
     */
    public void startFlying(Vector2 knockbackDirection, float force) {
        if (isFlying) return; // Already flying
        
        isFlying = true;
        flyTimer = FLY_DURATION;

        // Remove damping so physics takes over (test)
        body.setLinearDamping(0.5f);

        // Apply upward and outward force
        Vector2 flyImpulse = new Vector2(
            knockbackDirection.x * force * 0.5f, // Horizontal component
            Math.abs(force) * 2.5f // Strong upward component
        );

        body.applyImpulseAtCenter(flyImpulse);

        System.out.println("🚀 Pedestrian hit! Flying away with force: " + flyImpulse);
    }

    /**
     * Updates the pedestrian's position based on road scroll and crossing progress.
     *
     * @param scrollOffset current road scroll offset (pixels)
     * @param deltaTime    time since last frame (seconds)
     */
    public void updatePosition(float scrollOffset, float deltaTime) {
        // Calculate screen Y from world-relative position
        float screenY = relativeY + scrollOffset;
 
        if (isFlying) {
            // FLYING MODE: Let physics handle position, just sync visual
            updateFlyingMode(deltaTime);
        } else {
            // WALKING MODE: Manual position control
            updateWalkingMode(scrollOffset, screenY, deltaTime);
        }
    }
 
    /**
     * Walking mode: Pedestrian crosses the road manually (kinematic-like).
     */
    private void updateWalkingMode(float scrollOffset, float screenY, float deltaTime) {
        // Activate when the crossing scrolls into the visible area
        if (!activated && screenY > -100f && screenY < 800f) {
            activated = true;
        }
 
        // Only advance horizontal crossing once activated
        if (activated) {
            currentCrossingX += crossingDirection * crossingSpeed * deltaTime;
        }
 
        setX(currentCrossingX - getW() / 2f);
        setY(screenY);
 
        // Sync body position (manual control like kinematic)
        float bodyX = currentCrossingX / Constants.PPM;
        float bodyY = (screenY + getH() / 2f) / Constants.PPM;
        if (body != null) {
            body.setPosition(bodyX, bodyY);
            body.setVelocity(0f, 0f); // Keep stationary (kinematic-like)
        }
 
        // Expire only when scrolled below the screen (already passed the player)
        if (screenY < -getH() * 2f) {
            expired = true;
        }
        // Also expire if fully crossed to the other side
        if (activated) {
            if (crossingDirection > 0 && currentCrossingX > RoadRenderer.ROAD_RIGHT + getW() * 2f) {
                crossedSuccessfully = true;
                expired = true;
            } else if (crossingDirection < 0 && currentCrossingX < RoadRenderer.ROAD_LEFT - getW() * 2f) {
                crossedSuccessfully = true;
                expired = true;
            }
        }
    }
 
    /**
     * Flying mode: Pedestrian is physics-driven, spinning through the air.
     */
    private void updateFlyingMode(float deltaTime) {
        // Update fly timer
        flyTimer -= deltaTime;
        
        // Spin animation
        rotation += SPIN_SPEED * deltaTime;
        if (rotation >= 360f) rotation -= 360f;
        
        // Sync visual position with physics body
        Vector2 bodyPos = body.getPosition();
        setX(bodyPos.x * Constants.PPM - getW() / 2f);
        setY(bodyPos.y * Constants.PPM - getH() / 2f);
        
        // Expire after flying duration ends
        if (flyTimer <= 0f) {
            expired = true;
        }
    }

    /** True while the pedestrian is on-screen and still crossing the road. */
    public boolean isCrossing() {
        return activated && !expired;
    }

    /** True if the pedestrian made it across the road without being hit. */
    public boolean hasCrossedSuccessfully() {
        return crossedSuccessfully;
    }

    /** True if the pedestrian is currently flying through the air after being hit */
    public boolean isFlying() {
        return isFlying;
    }

    /** Returns remaining fly time (for game over delay) */
    public float getFlyTimeRemaining() {
        return flyTimer;
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    public void markExpired() {
        expired = true;
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (getTexture() == null) return;
        
        if (isFlying) {
            // Draw with rotation (spinning effect)
            // Correct signature: draw(texture, x, y, originX, originY, width, height, scaleX, scaleY, rotation, srcX, srcY, srcWidth, srcHeight, flipX, flipY)
            batch.draw(
                getTexture(),
                getX(), getY(),              // x, y position
                getW() / 2f, getH() / 2f,    // originX, originY (rotation center)
                getW(), getH(),              // width, height
                1f, 1f,                      // scaleX, scaleY
                rotation,                    // rotation (degrees)
                0, 0,                        // srcX, srcY (texture region start)
                getTexture().getWidth(),     // srcWidth (full texture width)
                getTexture().getHeight(),    // srcHeight (full texture height)
                false, false                 // flipX, flipY
            );
        } else {
            // Normal drawing
            batch.draw(getTexture(), getX(), getY(), getW(), getH());
        }
    }

    @Override
    public void dispose() {
        if (body != null) {
            body.destroy();
        }
    }

    public PhysicsBody getPhysicsBody() {
        return body;
    }
}
