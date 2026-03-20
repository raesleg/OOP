package io.github.raesleg.game.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.raesleg.engine.entity.IExpirable;
import io.github.raesleg.engine.entity.TextureObject;
import io.github.raesleg.engine.physics.PhysicsBody;

/**
 * Pedestrian entity only.
 * No movement logic, no crossing logic, no zone logic.
 */
public class Pedestrian extends TextureObject implements IExpirable {

    private final PhysicsBody body;
    private boolean expired;

    public Pedestrian(float x, float y, float w, float h, PhysicsBody body) {
        super("pedestrian.png", x, y, w, h);
        this.body = body;
        this.expired = false;

        if (body != null) {
            body.setUserData(this);
        }
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    public void markExpired() {
        this.expired = true;
    }

    public PhysicsBody getPhysicsBody() {
        return body;
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
}