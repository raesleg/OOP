package io.github.raesleg.game.state;

import com.badlogic.gdx.Gdx;

import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.game.GameConstants;
import io.github.raesleg.game.entities.IChaseEntity;
import io.github.raesleg.game.entities.vehicles.PlayerCar;
import io.github.raesleg.game.factory.PoliceCarFactory;
import io.github.raesleg.game.rules.RuleManager;

/**
 * ChaseDirector — SRP extraction for Level 2 police chase orchestration.
 * <p>
 * Owns police spawn timing, chase AI updates, siren volume scaling,
 * dashboard distance reporting, and police-light intensity — logic
 * previously inlined inside {@link io.github.raesleg.game.scene.Level2Scene}.
 * <p>
 * <b>SRP:</b> Sole responsibility is managing the police-chase lifecycle.
 * <b>DIP:</b> Depends on {@link IChaseEntity} abstraction, not concrete
 * {@code PoliceCar}.
 */
public final class ChaseDirector {

    private final PoliceCarFactory policeFactory;
    private final RuleManager ruleManager;
    private final SoundDevice sound;

    private IChaseEntity policeCar;
    private boolean policeSpawned;
    private boolean sirenStarted;

    public ChaseDirector(PoliceCarFactory policeFactory,
            RuleManager ruleManager,
            SoundDevice sound) {
        this.policeFactory = policeFactory;
        this.ruleManager = ruleManager;
        this.sound = sound;
    }

    /**
     * Per-frame update — spawns police on first violation, then
     * updates chase AI / siren / dashboard / lights.
     *
     * @return normalised police distance [0–1], or 1.0 if police not yet spawned
     */
    public float update(float deltaTime,
            PlayerCar player,
            float simSpeed, float maxSpeed,
            DashboardUI dashboard,
            io.github.raesleg.game.scene.PoliceLightSystem lights) {

        /* Spawn police on first rule break */
        if (!policeSpawned && ruleManager.getRulesBroken() >= 1) {
            policeCar = policeFactory.spawn();
            policeSpawned = true;
            Gdx.app.log("ChaseDirector", "Police spawned — chase begins!");
        }

        if (policeCar == null)
            return 1.0f;

        policeCar.updateChase(
                deltaTime,
                player.getX(), player.getY(),
                ruleManager.getRulesBroken(),
                GameConstants.MAX_WANTED_STARS,
                simSpeed, maxSpeed);

        /* Siren — start on first frame, volume scales with distance */
        if (!sirenStarted) {
            sound.loopSound("policesiren");
            sirenStarted = true;
        }

        float distance = player.getY() - policeCar.getScreenY();
        float normDist = Math.min(1f, Math.max(0f,
                distance / GameConstants.POLICE_MAX_DISTANCE));

        float sirenVol = 1.0f - normDist;
        sirenVol = Math.max(GameConstants.SIREN_MIN_VOLUME, sirenVol);
        sound.setSoundVolume("policesiren", sirenVol);

        dashboard.onPoliceDistanceUpdated(normDist);

        lights.setNormalisedDistance(normDist);
        lights.update(deltaTime);

        return normDist;
    }

    /** Whether the police car has caught the player (game-over condition). */
    public boolean hasCaughtPlayer() {
        return policeCar != null && policeCar.hasCaughtPlayer();
    }

    /** Whether the police car has been spawned yet. */
    public boolean isPoliceSpawned() {
        return policeSpawned;
    }
}
