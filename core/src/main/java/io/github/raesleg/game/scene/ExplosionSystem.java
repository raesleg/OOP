package io.github.raesleg.game.scene;

import com.badlogic.gdx.math.Vector2;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.game.GameConstants;
import io.github.raesleg.game.entities.misc.ExplosionOverlay;
import io.github.raesleg.game.entities.misc.Particle;

/**
 * ExplosionSystem — Spawns explosion particles and overlay at a given
 * position, and plays the explosion sound.
 * <p>
 * Extracted from {@code BaseGameScene.triggerExplosionGameOver()} to
 * satisfy SRP: entity spawning is a separate responsibility from
 * level-end state management.
 */
public final class ExplosionSystem {

    private ExplosionSystem() {
    }

    /**
     * Spawns a ring of explosion particles, places a large overlay sprite,
     * and plays the explosion sound.
     *
     * @param entityManager manager to register new entities in
     * @param sound         audio device for the explosion sound
     * @param centreX       centre X of the explosion (pixels)
     * @param centreY       centre Y of the explosion (pixels)
     */
    public static void trigger(EntityManager entityManager, SoundDevice sound,
            float centreX, float centreY) {
        Particle.spawnExplosion(entityManager,
                new Vector2(centreX / Constants.PPM, centreY / Constants.PPM), 50f);

        entityManager.addEntity(new ExplosionOverlay(
                "explode.png",
                centreX - 100f, centreY - 100f,
                200f, 200f,
                GameConstants.EXPLOSION_DELAY));

        sound.playSound("explosion_big", 0.5f);
    }
}
