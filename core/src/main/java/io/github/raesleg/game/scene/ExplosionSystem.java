package io.github.raesleg.game.scene;

import com.badlogic.gdx.math.Vector2;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.game.GameConstants;
import io.github.raesleg.game.entities.misc.ExplosionOverlay;
import io.github.raesleg.game.entities.misc.Particle;

/**
 * ExplosionSystem — Static factory for spawning explosion visual and audio effects at a game position.
 * Extracted to satisfy SRP: particle/overlay spawning is separate from level-end state management.
 * Provides a clean, reusable interface for any code that needs explosion feedback.
 */
public final class ExplosionSystem {

    // Prevent instantiation; this is a static utility class
    private ExplosionSystem() {
    }

    // Spawn explosion particles, overlay sprite, and sound at the given world position
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
