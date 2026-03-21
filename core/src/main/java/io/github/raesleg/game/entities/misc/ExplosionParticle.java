package io.github.raesleg.game.entities.misc;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.IExpirable;
import io.github.raesleg.engine.entity.EntityManager;
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

        // debug logging removed — ran 12× per particle per frame, causing I/O stalls
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (getTexture() != null && alpha > 0) {
            Color oldColor = batch.getColor().cpy(); // copy colour
            batch.setColor(1f, 1f, 1f, alpha);
            super.draw(batch); // reuse parent's centering logic (DRY)
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

    /**
     * Static factory — creates and registers a full ring of particles centred on
     * {@code impactPoint}.
     * <p>
     * Encapsulating the visual spawn math here
     * keeps {@link GameCollisionHandler} free of presentation-layer concerns.
     *
     * @param entityManager the manager that will own the new particles
     * @param impactPoint   centre of the explosion in physics (Box2D) metres
     * @param impactForce   magnitude of the impact (unused visually, reserved
     *                      for future scaling)
     */
    public static void spawnExplosion(EntityManager entityManager,
            Vector2 impactPoint, float impactForce) {
        int numParticles = 12;
        float particleSize = 16f;
        float particleLifetime = 1.0f;

        float explosionX = impactPoint.x * Constants.PPM;
        float explosionY = impactPoint.y * Constants.PPM;

        for (int i = 0; i < numParticles; i++) {
            // evenly spaced angles with small random scatter
            float angle = (float) (Math.PI * 2 * i / numParticles)
                    + (float) (Math.random() * 0.5f - 0.25f);

            float speed = 50f + (float) Math.random() * 100f;

            Vector2 particleVelocity = new Vector2(
                    (float) Math.cos(angle) * speed,
                    (float) Math.sin(angle) * speed);

            // random offset from exact impact point for scatter effect
            float offsetX = (float) (Math.random() * 10 - 5);
            float offsetY = (float) (Math.random() * 10 - 5);

            entityManager.addEntity(new ExplosionParticle(
                    "droplet.png",
                    explosionX + offsetX,
                    explosionY + offsetY,
                    particleSize,
                    particleSize,
                    particleVelocity,
                    particleLifetime));
        }
    }

}
