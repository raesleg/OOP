package io.github.raesleg.demo;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.IExpirable;
import io.github.raesleg.engine.entity.TextureObject;

public class ExplosionParticle extends TextureObject implements IExpirable {
    private float lifetime;
    private float maxLifetime;
    private Vector2 velocity;
    private float alpha; // transparency

    public ExplosionParticle(String filename, float x, float y, float width, float height, Vector2 velocity,
            float lifetime) {
        super(filename, x, y, width, height);
        this.velocity = velocity;
        this.lifetime = lifetime;
        this.maxLifetime = lifetime;
        this.alpha = 1f;
    }

    @Override
    public void update(float deltaTime) {
        // move particle
        setX(getX() + velocity.x * deltaTime * Constants.PPM);
        setY(getY() + velocity.y * deltaTime * Constants.PPM);

        // apply gravity effect
        velocity.y -= 9.8f * deltaTime * 10f;

        // particles fade away over time
        lifetime -= deltaTime;
        alpha = Math.max(0, lifetime / maxLifetime); // 1.0 -> 0.0

        System.out.println("Particle at (" + getX() + "," + getY() + ") alpha=" + alpha); // logging for debug
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (getTexture() != null && alpha > 0) {
            Color oldColor = batch.getColor().cpy(); // copy colour
            batch.setColor(1f, 1f, 1f, alpha);

            batch.draw(
                    getTexture(),
                    getX() - getW() / 2f,
                    getY() - getH() / 2f,
                    getW(),
                    getH());
            batch.setColor(oldColor); // restore original color
        }
    }

    public boolean isDead() {
        return lifetime <= 0;
    }

    // generic removal of dead entities
    @Override
    public boolean isExpired() {
        return isDead();
    }

}
