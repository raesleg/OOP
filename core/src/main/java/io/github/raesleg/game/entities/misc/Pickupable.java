package io.github.raesleg.game.entities.misc;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.IExpirable;
import io.github.raesleg.engine.entity.TextureObject;
import io.github.raesleg.engine.physics.PhysicsBody;

/**
 * Pickupable — A collectable battery that awards +50 score.
 * Uses a DYNAMIC sensor body for reliable collision detection.
 * Extends TextureObject to participate in the Flyweight texture cache.
 * Implements IExpirable for automatic removal by EntityManager.
 */
public class Pickupable extends TextureObject implements IExpirable {

    private final PhysicsBody body;
    private final float relativeY;
    private boolean expired;

    public Pickupable(PhysicsBody body, float centreXPx, float relativeY,
            float wPx, float hPx) {
        super("charge.png", centreXPx - wPx / 2f, 0, wPx, hPx);
        this.body = body;
        this.relativeY = relativeY;
        this.expired = false;

        if (body != null) {
            body.setUserData(this);
        }
    }

    public void updatePosition(float scrollOffset) {
        float screenY = relativeY + scrollOffset;
        setY(screenY);

        // Manually set body position every frame (kinematic-like behavior)
        if (body != null) {
            float bodyX = (getX() + getW() / 2f) / Constants.PPM;
            float bodyY = (screenY + getH() / 2f) / Constants.PPM;

            body.setPosition(bodyX, bodyY);
            body.setVelocity(0f, 0f); // Zero velocity to prevent drift
        }

        // Expire when scrolled off screen
        if (screenY < -getH() * 3f) {
            expired = true;
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (getTexture() != null && !expired) {
            batch.setColor(Color.WHITE);
            batch.draw(getTexture(), getX(), getY(), getW(), getH());
        }
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    public void markExpired() {
        expired = true;
    }

    public float getRelativeY() {
        return relativeY;
    }

    @Override
    public void dispose() {
        if (body != null) {
            body.destroy();
        }
    }
}