package io.github.raesleg.game.scene;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.game.GameConstants;
import io.github.raesleg.game.collision.listeners.Level2TrafficListener;
import io.github.raesleg.game.entities.IChaseEntity;
import io.github.raesleg.game.factory.PoliceCarFactory;
import io.github.raesleg.game.factory.RoadHazardSpawner;
import io.github.raesleg.game.io.Keyboard;
import io.github.raesleg.game.movement.SurfaceEffect;
import io.github.raesleg.game.zone.RoadHazard;

/**
 * Level2Scene — Highway Chase (Temple Run style).
 * <p>
 * The police car is on the player's tail from the very start.
 * The player must weave through heavy NPC traffic while maintaining
 * high speed to outrun the police. Crashing into NPC cars increases
 * police aggression (they close in faster).
 * <p>
 * <b>No crosswalks or pedestrians</b> — pure expressway survival.
 * <p>
 * <b>SRP Composition:</b> Delegates traffic spawning to
 * {@link TrafficSpawningSystem}, rain rendering to
 * {@link RainEffectSystem}, police creation to
 * {@link PoliceCarFactory}, and violation reactions to
 * {@link Level2TrafficListener}.
 * <p>
 * <b>DIP:</b> RuleManager and CommandHistory are injected from
 * {@link BaseGameScene}. Police car is created via factory.
 * Scene depends on {@link IChaseEntity} abstraction, not concrete
 * {@code PoliceCar}.
 */
public class Level2Scene extends BaseGameScene {

    /* ── Level-specific components ── */
    private TrafficSpawningSystem trafficSystem;
    private PoliceCarFactory policeFactory;
    private IChaseEntity policeCar;
    private boolean policeSpawned;
    private boolean sirenStarted;

    /* ── Road hazard spawners (puddles, mud) ── */
    private final List<RoadHazardSpawner> hazardSpawners = new ArrayList<>();

    /* ── Rain rendering system (SRP extraction) ── */
    private RainEffectSystem rainEffect;

    /* ── Police light glow (SRP extraction) ── */
    private PoliceLightSystem policeLightSystem;

    @Override
    protected float getMaxScrollPixelsPerSecond() {
        return GameConstants.L2_MAX_SCROLL;
    }

    @Override
    protected float getCameraZoom() {
        return GameConstants.L2_CAMERA_ZOOM;
    }

    @Override
    protected float getLevelLength() {
        return GameConstants.L2_LEVEL_LENGTH;
    }

    @Override
    protected float getMaxSpeed() {
        return GameConstants.L2_MAX_SPEED;
    }

    @Override
    protected float getAcceleration() {
        return GameConstants.L2_ACCELERATION;
    }

    @Override
    protected float getBrakeRate() {
        return GameConstants.L2_BRAKE_RATE;
    }

    @Override
    protected String getBgmPath() {
        return GameConstants.BGM_DEFAULT;
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * Level-specific initialisation
     * ══════════════════════════════════════════════════════════════
     */

    @Override
    protected void initLevelData() {
        /*
         * Traffic spawning system (NPC cars, pickups, trees) — no crosswalk exclusions
         */
        trafficSystem = new TrafficSpawningSystem(
                getEntityManager(), getWorld(), GameConstants.L2_SPAWN_SCREEN_HEIGHT,
                GameConstants.L2_NPC_SPAWN_SEC, GameConstants.L2_PICKUP_SPAWN_SEC, 2.5f);

        /* Road hazards (puddles, mud) — NPC spawner provides lane occupancy */
        hazardSpawners.add(new RoadHazardSpawner(
                getEntityManager(), getWorld(), GameConstants.L2_SPAWN_SCREEN_HEIGHT,
                GameConstants.L2_PUDDLE_INTERVAL,
                trafficSystem.getNpcSpawner(), null,
                SurfaceEffect.PUDDLE, "puddle.png"));

        hazardSpawners.add(new RoadHazardSpawner(
                getEntityManager(), getWorld(), GameConstants.L2_SPAWN_SCREEN_HEIGHT,
                GameConstants.L2_MUD_INTERVAL,
                trafficSystem.getNpcSpawner(), null,
                SurfaceEffect.MUD, "mud.png"));

        trafficSystem.getNpcSpawner().setHazardOccupancy(hazardSpawners.get(1));

        /* Police factory (SRP — creation extracted from scene) */
        policeFactory = new PoliceCarFactory(getWorld(), getEntityManager());

        /* Wire collision listeners — extracted standalone class (SRP + DIP) */
        getCollisionHandler().setTrafficViolationListener(
                new Level2TrafficListener(
                        getRuleManager(), getCommandHistory(),
                        this::addScore, this::incrementCrashCount,
                        () -> getSpeedScrollController().applySpeedPenalty(
                                GameConstants.L2_CRASH_SPEED_PENALTY)));
        getCollisionHandler().setPickupListener(this::handlePickup);

        policeSpawned = false;

        /* Rain effect system (SRP — rendering extracted from scene) */
        rainEffect = new RainEffectSystem();

        /* Police light glow system (SRP) */
        policeLightSystem = new PoliceLightSystem();

        /* Level-specific sounds (paths from GameConstants) */
        try {
            getSound().addSound("boundary_hit", GameConstants.SFX_HIT_SOUND);
        } catch (Exception e) {
            Gdx.app.log("Level2Scene", "Sound: " + e.getMessage());
        }
        try {
            getSound().addSound("policesiren", GameConstants.SFX_POLICE_SIREN);
        } catch (Exception e) {
            Gdx.app.log("Level2Scene", "Sound: " + e.getMessage());
        }
        try {
            getSound().addSound("rain", GameConstants.SFX_RAIN);
        } catch (Exception e) {
            Gdx.app.log("Level2Scene", "Sound: " + e.getMessage());
        }

        try {
            getSound().addSound("scream", GameConstants.SFX_SCREAM);
        } catch (Exception e) {
            Gdx.app.log("Level2Scene", "Sound: " + e.getMessage());
        }

        // try {
        // getSound().addSound("puddle", "puddlesound.wav");
        // } catch (Exception e) {
        // Gdx.app.log("Level2Scene", "Could not load puddle sound: " + e.getMessage());
        // }

        // try {
        // getSound().addSound("mud", "mudsound.wav");
        // } catch (Exception e) {
        // Gdx.app.log("Level2Scene", "Could not load rain sound: " + e.getMessage());
        // }

        /* Dashboard — enable police distance mode */
        getDashboard().setPoliceDistanceMode(true);

        /* Start rain ambience */
        getSound().loopSound("rain");
        getSound().setSoundVolume("rain", 1.0f);

        Gdx.app.log("Level2Scene", "Level 2 initialised — highway chase");
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * Level-specific per-frame logic
     * ══════════════════════════════════════════════════════════════
     */

    @Override
    protected void updateGame(float deltaTime) {
        /* Delegate NPC/pickup/tree spawning to composed system */
        trafficSystem.setFrameState(
                getNpcScrollSpeedPixelsPerSecond(), getScrollOffset(), getSimulatedSpeedKmh());
        trafficSystem.update(deltaTime);

        /* Update all hazard spawners */
        for (RoadHazardSpawner spawner : hazardSpawners)
            spawner.update(deltaTime, getScrollOffset());

        setRulesBroken(getRuleManager().getRulesBroken());

        /*
         * Player vertical movement — direct input control within bounds.
         * UP/DOWN keys move the car on-screen; no reverse (scroll always forward).
         */
        Keyboard kb = getIOManager().getInputs(Keyboard.class);
        float currentY = getPlayerCar().getY();
        if (kb.isHeld(Constants.UP)) {
            currentY += GameConstants.L2_PLAYER_VERTICAL_SPEED * deltaTime;
        }
        if (kb.isHeld(Constants.DOWN)) {
            currentY -= GameConstants.L2_PLAYER_VERTICAL_SPEED * deltaTime;
        }
        currentY = Math.max(GameConstants.PLAYER_MIN_Y,
                Math.min(GameConstants.PLAYER_MAX_Y, currentY));
        getPlayerCar().setY(currentY);

        /* Sync physics body to match visual position */
        var playerBody = getPlayerCar().getPhysicsBody();
        if (playerBody != null) {
            playerBody.setPosition(playerBody.getPosition().x,
                    (currentY + getPlayerCar().getH() / 2f) / Constants.PPM);
        }

        /* Spawn police on first rule break (not immediately) */
        if (!policeSpawned && getRuleManager().getRulesBroken() >= 1) {
            policeCar = policeFactory.spawn();
            policeSpawned = true;
            Gdx.app.log("Level2Scene", "Police spawned — chase begins!");
        }

        if (policeCar != null) {
            policeCar.updateChase(
                    deltaTime,
                    getPlayerCar().getX(),
                    getPlayerCar().getY(),
                    getRuleManager().getRulesBroken(),
                    GameConstants.MAX_WANTED_STARS,
                    getSimulatedSpeedKmh(),
                    getMaxSpeed());

            /* Police siren — start on first frame, volume scales with distance */
            if (!sirenStarted) {
                getSound().loopSound("policesiren");
                sirenStarted = true;
            }
            float distance = getPlayerCar().getY() - policeCar.getScreenY();
            float sirenVol = 1.0f - Math.min(1f, Math.max(0f,
                    distance / GameConstants.POLICE_MAX_DISTANCE));
            sirenVol = Math.max(GameConstants.SIREN_MIN_VOLUME, sirenVol);
            getSound().setSoundVolume("policesiren", sirenVol);

            /* Dashboard distance meter */
            float normDist = Math.min(1f, Math.max(0f,
                    distance / GameConstants.POLICE_MAX_DISTANCE));
            getDashboard().onPoliceDistanceUpdated(normDist);

            /* Police light glow — intensity grows as police closes in */
            policeLightSystem.setNormalisedDistance(normDist);
            policeLightSystem.update(deltaTime);
        }
    }

    @Override
    protected boolean isGameOver() {
        if (getPlayerCar().isFlashing())
            return false;
        if (getRulesBroken() >= GameConstants.MAX_WANTED_STARS)
            return true;
        return policeCar != null && policeCar.hasCaughtPlayer();
    }

    @Override
    protected String getGameOverReason() {
        if (getRulesBroken() >= GameConstants.MAX_WANTED_STARS)
            return "Too many violations \u2014 " + GameConstants.MAX_WANTED_STARS + " wanted stars";
        return "Police caught you";
    }

    @Override
    protected String getLevelName() {
        return "Level 2 \u2014 Highway Chase";
    }

    @Override
    protected java.util.List<String> getViolationLog() {
        return (getRuleManager() != null) ? getRuleManager().getViolationLog() : java.util.List.of();
    }

    @Override
    protected BaseGameScene createRetryScene() {
        return new Level2Scene();
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * Level2-specific rendering — delegated to systems
     * ══════════════════════════════════════════════════════════════
     */

    @Override
    protected void renderLevelEffects(ShapeRenderer sr, SpriteBatch batch) {
        /* Rain overlay (SRP — delegated to RainEffectSystem) */
        rainEffect.render(sr, batch, getVisMinX(), getVisMinY(), getVisMaxX(), getVisMaxY());

        /* Police red/blue glow at bottom edge of visible area */
        policeLightSystem.render(sr, getVisMinX(), getVisMinY(), getVisMaxX());

        /* Render hazards at correct z-order (under entities) */
        batch.begin();
        for (RoadHazardSpawner spawner : hazardSpawners) {
            for (RoadHazard h : spawner.getActiveHazards()) {
                if (!h.isExpired())
                    h.drawHazard(batch);
            }
        }
        batch.end();
    }

    @Override
    protected void disposeLevelData() {
        getSound().stopSound("rain");
        getSound().stopSound("policesiren");
        if (trafficSystem != null) {
            trafficSystem.dispose();
            trafficSystem = null;
        }
        for (RoadHazardSpawner s : hazardSpawners)
            s.clearAll();
        hazardSpawners.clear();
        policeCar = null;
        policeFactory = null;
        rainEffect = null;
        if (policeLightSystem != null) {
            policeLightSystem.dispose();
            policeLightSystem = null;
        }
        Gdx.app.log("Level2Scene", "Level 2 data disposed");
    }
}
