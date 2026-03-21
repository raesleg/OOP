package io.github.raesleg.game.entities.misc;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.raesleg.engine.entity.IExpirable;
import io.github.raesleg.engine.entity.TextureObject;

/**
 * StopSign — A static sprite entity that scrolls with the road.
 * <p>
 * Placed on the grass shoulder beside the crosswalk to give the player
 * a visual cue that braking is required. Has no physics body — it is
 * purely decorative. Implements {@link IExpirable} for automatic cleanup
 * when scrolled off screen.
 * <p>
 * <b>Design Pattern:</b> Flyweight (shared texture via TextureObject cache).
 * <b>Engine/Game Boundary:</b> Extends engine's TextureObject, lives in game
 * layer.
 */
public class StopSign extends TextureObject implements IExpirable {

    private final float relativeY;
    private boolean expired;

    /**
     * Creates a stop sign at the given world-relative Y position.
     *
     * @param pixelX    fixed X position in pixels (on the road shoulder)
     * @param relativeY Y position relative to scroll offset
     */
    public StopSign(float pixelX, float relativeY) {
        super("stopsign.png", pixelX, 0, 120f, 120f);
        this.relativeY = relativeY;
        this.expired = false;
    }

    /**
     * Updates the stop sign's screen position based on the road scroll offset.
     *
     * @param scrollOffset current road scroll offset (pixels)
     */
    public void updatePosition(float scrollOffset) {
        float screenY = relativeY + scrollOffset;
        setY(screenY);

        // Expire only when scrolled below the screen (already passed the player)
        if (screenY < -getH() * 2f) {
            expired = true;
        }
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
}
