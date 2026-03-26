package io.github.raesleg.game.scene;

import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.game.entities.misc.Particle;
import io.github.raesleg.game.entities.vehicles.PlayerCar;
import io.github.raesleg.game.movement.CarMovementModel;
import io.github.raesleg.game.movement.SurfaceEffect;

/**
 * HazardEffectSystem — SRP extraction for surface-effect particle spawning.
 * <p>
 * Reads entry/trail/exit signals from the player's {@link CarMovementModel}
 * and spawns the appropriate visual particles (water splash, mud splatter).
 * Previously inlined inside {@link Level2Scene}.
 * <p>
 * <b>SRP:</b> Sole responsibility is mapping surface-effect signals to
 * particle spawns.
 */
public final class HazardEffectSystem {

    private final EntityManager entityManager;
    private final PlayerCar playerCar;

    public HazardEffectSystem(EntityManager entityManager, PlayerCar playerCar) {
        this.entityManager = entityManager;
        this.playerCar = playerCar;
    }

    /** Poll movement model signals and spawn particles accordingly. */
    public void update() {
        CarMovementModel model = playerCar.getCarMovementModel();
        SurfaceEffect effect = model.getSurfaceEffect();

        if (model.consumeEntryEffectSignal()) {
            spawnEntryEffect(effect);
        }
        if (model.didEmitTrailEffectThisStep()) {
            spawnTrailEffect(effect);
        }
        if (model.consumeExitEffectSignal()) {
            spawnExitEffect(effect);
        }
    }

    private float centreX() {
        return playerCar.getX() + playerCar.getW() * 0.5f;
    }

    private float centreY() {
        return playerCar.getY() + playerCar.getH() * 0.5f;
    }

    private void spawnEntryEffect(SurfaceEffect effect) {
        if (effect == SurfaceEffect.LOW_FRICTION) {
            Particle.spawnWaterSplash(entityManager, centreX(), centreY(), 12);
        } else if (effect == SurfaceEffect.HIGH_FRICTION) {
            Particle.spawnMudSplatter(entityManager, centreX(), centreY(), 8);
        }
    }

    private void spawnTrailEffect(SurfaceEffect effect) {
        if (effect == SurfaceEffect.LOW_FRICTION) {
            Particle.spawnContinuousSplash(entityManager, centreX(), centreY());
        } else if (effect == SurfaceEffect.HIGH_FRICTION) {
            if (Math.random() > 0.5) {
                Particle.spawnMudSplatter(entityManager, centreX(), centreY(), 2);
            }
        }
    }

    private void spawnExitEffect(SurfaceEffect effect) {
        if (effect == SurfaceEffect.LOW_FRICTION) {
            Particle.spawnWaterSplash(entityManager, centreX(), centreY(), 6);
        } else if (effect == SurfaceEffect.HIGH_FRICTION) {
            Particle.spawnMudSplatter(entityManager, centreX(), centreY(), 4);
        }
    }
}
