package io.github.raesleg.game.zone;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.physics.box2d.BodyDef;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.IExpirable;
import io.github.raesleg.engine.physics.PhysicsWorld;
import io.github.raesleg.game.movement.MotionTuning;

/**
 * Puddle — A scrollable MotionZone that applies LOW_TRACTION when
 * the player drives over it, causing a sliding effect.
 * Uses puddle.png for visual rendering.
 */
public class Puddle extends MotionZone implements IExpirable {

    private static Texture sharedTexture;

    private final float relativeY;
    private boolean expired;

    public Puddle(PhysicsWorld world, float centreXPx, float relativeY,
            float wPx, float hPx) {
        super(centreXPx - wPx / 2f, 0, wPx, hPx,
                MotionTuning.LOW_TRACTION,
                new Color(0.3f, 0.5f, 0.9f, 0.35f),
                world.createBody(
                        BodyDef.BodyType.KinematicBody,
                        centreXPx / Constants.PPM,
                        relativeY / Constants.PPM,
                        (wPx / Constants.PPM) / 2f,
                        (hPx / Constants.PPM) / 2f,
                        0f, 0f, true, null));
        this.relativeY = relativeY;
        this.expired = false;

        if (sharedTexture == null) {
            sharedTexture = new Texture("puddle.png");
        }
    }

    public void updatePosition(float scrollOffset) {
        float screenY = relativeY + scrollOffset;
        setY(screenY);
        getBody().setPosition(
                (getX() + getW() / 2f) / Constants.PPM,
                (screenY + getH() / 2f) / Constants.PPM);
        if (screenY < -getH() * 3f) {
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
        // no-op — puddles are drawn by PuddleSpawner.render() before
        // the entity pass so they appear under the player car
    }

    /** Explicit draw called by PuddleSpawner for correct z-order. */
    public void drawPuddle(SpriteBatch batch) {
        if (sharedTexture != null) {
            batch.draw(sharedTexture, getX(), getY(), getW(), getH());
        }
    }

    @Override
    public void draw(ShapeRenderer sr) {
        // no-op — visual handled by draw(SpriteBatch)
    }

    @Override
    public void dispose() {
        getBody().destroy();
    }
}
