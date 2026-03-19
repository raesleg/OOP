package io.github.raesleg.game.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.raesleg.engine.entity.IExpirable;
import io.github.raesleg.engine.entity.TextureObject;

/**
 * ExplosionOverlay — A temporary large explosion sprite shown at
 * the player's position on the 3rd NPC crash, before the results screen.
 */
public class ExplosionOverlay extends TextureObject implements IExpirable {

    private float lifetime;
    private final float maxLifetime;

    public ExplosionOverlay(String filename, float x, float y,
            float w, float h, float lifetime) {
        super(filename, x, y, w, h);
        this.lifetime = lifetime;
        this.maxLifetime = lifetime;
    }

    @Override
    public void update(float deltaTime) {
        lifetime -= deltaTime;
    }

    @Override
    public boolean isExpired() {
        return lifetime <= 0;
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (getTexture() == null)
            return;
        float alpha = Math.max(0f, lifetime / maxLifetime);
        Color old = batch.getColor().cpy();
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(getTexture(), getX(), getY(), getW(), getH());
        batch.setColor(old);
    }
}
