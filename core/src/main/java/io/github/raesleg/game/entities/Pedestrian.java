package io.github.raesleg.game.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

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

        // Start on the appropriate side of the road
        if (crossingDirection > 0) {
            this.currentCrossingX = RoadRenderer.ROAD_LEFT - getW();
        } else {
            this.currentCrossingX = RoadRenderer.ROAD_RIGHT + getW();
        }

        body.setUserData(this);
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

        // Sync kinematic body position (pixels to metres)
        float bodyX = currentCrossingX / io.github.raesleg.engine.Constants.PPM;
        float bodyY = (screenY + getH() / 2f) / io.github.raesleg.engine.Constants.PPM;
        if (body != null) {
            body.setPosition(bodyX, bodyY);
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

    /** True while the pedestrian is on-screen and still crossing the road. */
    public boolean isCrossing() {
        return activated && !expired;
    }

    /** True if the pedestrian made it across the road without being hit. */
    public boolean hasCrossedSuccessfully() {
        return crossedSuccessfully;
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
        if (getTexture() != null) {
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
