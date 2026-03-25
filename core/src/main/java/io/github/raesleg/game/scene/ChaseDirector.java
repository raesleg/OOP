package io.github.raesleg.game.scene;

import com.badlogic.gdx.Gdx;

import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.game.GameConstants;
import io.github.raesleg.game.entities.IChaseEntity;
import io.github.raesleg.game.entities.vehicles.PlayerCar;
import io.github.raesleg.game.factory.PoliceCarFactory;
import io.github.raesleg.game.rules.RuleManager;
import io.github.raesleg.game.state.DashboardUI;

/**
 * ChaseDirector — Orchestrates police chase mechanics for Level 2.
 * <p>
 * Owns the police spawn trigger, chase update delegation, siren
 * volume scaling, dashboard distance-meter updates, and police
 * light glow intensity. Extracted from Level2Scene to satisfy SRP:
 * the scene no longer contains distance math or audio scaling code.
 */
public final class ChaseDirector {

    private final PoliceCarFactory policeFactory;
    private final SoundDevice sound;
    private final DashboardUI dashboard;
    private final PoliceLightSystem policeLightSystem;

    private IChaseEntity policeCar;
    private boolean policeSpawned;
    private boolean sirenStarted;

    public ChaseDirector(PoliceCarFactory policeFactory, SoundDevice sound,
            DashboardUI dashboard, PoliceLightSystem policeLightSystem) {
        this.policeFactory = policeFactory;
        this.sound = sound;
        this.dashboard = dashboard;
        this.policeLightSystem = policeLightSystem;
    }

    /**
     * Spawns police on first rule break and updates chase state each frame.
     */
    public void update(float deltaTime, PlayerCar player,
            RuleManager ruleManager, float simulatedSpeed, float maxSpeed) {

        if (!policeSpawned && ruleManager.getRulesBroken() >= 1) {
            policeCar = policeFactory.spawn();
            policeSpawned = true;
            Gdx.app.log("ChaseDirector", "Police spawned — chase begins!");
        }

        if (policeCar == null)
            return;

        policeCar.updateChase(
                deltaTime,
                player.getX(), player.getY(),
                ruleManager.getRulesBroken(),
                GameConstants.MAX_WANTED_STARS,
                simulatedSpeed, maxSpeed);

        /* Siren — start on first frame, volume scales with distance */
        if (!sirenStarted) {
            sound.loopSound("policesiren");
            sirenStarted = true;
        }

        float distance = player.getY() - policeCar.getScreenY();
        float sirenVol = 1.0f - Math.min(1f, Math.max(0f,
                distance / GameConstants.POLICE_MAX_DISTANCE));
        sirenVol = Math.max(GameConstants.SIREN_MIN_VOLUME, sirenVol);
        sound.setSoundVolume("policesiren", sirenVol);

        /* Dashboard distance meter */
        float normDist = Math.min(1f, Math.max(0f,
                distance / GameConstants.POLICE_MAX_DISTANCE));
        dashboard.onPoliceDistanceUpdated(normDist);

        /* Police light glow */
        policeLightSystem.setNormalisedDistance(normDist);
        policeLightSystem.update(deltaTime);
    }

    /** Returns the active chase entity (may be null before spawn). */
    public IChaseEntity getPoliceCar() {
        return policeCar;
    }

    /** Whether the police have been spawned this level. */
    public boolean isPoliceSpawned() {
        return policeSpawned;
    }
}
