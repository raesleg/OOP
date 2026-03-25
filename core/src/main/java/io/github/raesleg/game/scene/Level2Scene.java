package io.github.raesleg.game.scene;

import java.util.ArrayList;
import java.util.List;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import io.github.raesleg.game.GameConstants;
import io.github.raesleg.game.collision.listeners.Level2TrafficListener;
import io.github.raesleg.game.factory.PoliceCarFactory;
import io.github.raesleg.game.factory.RoadHazardSpawner;
import io.github.raesleg.game.io.Keyboard;
import io.github.raesleg.game.movement.SurfaceEffect;
import io.github.raesleg.game.state.ChaseDirector;
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
    private PlayerController playerController;
    private ChaseDirector chaseDirector;
    private HazardEffectSystem hazardEffects;

    /* ── Road hazard spawners (puddles, mud) ── */
    private final List<RoadHazardSpawner> hazardSpawners = new ArrayList<>();

    /* ── Rain rendering system (SRP extraction) ── */
    private RainEffectSystem rainEffect;

    /* ── Police light glow (SRP extraction) ── */
    private PoliceLightSystem policeLightSystem;

    /* ── Surface particle dispatch (SRP — replaces if/else chains) ── */
    private SurfaceParticleDispatcher surfaceParticleDispatcher;

    /* ── Player vertical movement (SRP extraction) ── */
    private PlayerController playerController;

    /* ── Police chase orchestration (SRP extraction) ── */
    private ChaseDirector chaseDirector;

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
        PoliceCarFactory policeFactory = new PoliceCarFactory(getWorld(), getEntityManager());

        /* Wire collision listeners — extracted standalone class (SRP + DIP) */
        getCollisionHandler().setTrafficViolationListener(
                new Level2TrafficListener(
                        getRuleManager(), getCommandHistory(),
                        this::addScore, this::incrementCrashCount,
                        () -> getSpeedScrollController().applySpeedPenalty(
                                GameConstants.L2_CRASH_SPEED_PENALTY)));
        getCollisionHandler().setPickupListener(this::handlePickup);

        /* Player vertical movement controller (SRP extraction) */
        Keyboard kb = getIOManager().getInputs(Keyboard.class);
        playerController = new PlayerController(kb, getPlayerCar());

        /* Chase director (SRP extraction — police spawn/AI/siren/distance) */
        chaseDirector = new ChaseDirector(policeFactory, getRuleManager(), getSound());

        /* Hazard particle effect system (SRP extraction) */
        hazardEffects = new HazardEffectSystem(getEntityManager(), getPlayerCar());

        /* Rain effect system (SRP — rendering extracted from scene) */
        rainEffect = new RainEffectSystem();

        /* Police light glow system (SRP) */
        policeLightSystem = new PoliceLightSystem();

        /* Surface particle dispatcher (SRP — replaces if/else chains) */
        surfaceParticleDispatcher = new SurfaceParticleDispatcher();

        /* Player vertical movement controller (SRP extraction) */
        Keyboard kb = getIOManager().getInputs(Keyboard.class);
        playerController = new PlayerController(kb);

        /* Chase director — police spawn, siren, dashboard distance (SRP extraction) */
        chaseDirector = new ChaseDirector(policeFactory, getSound(), getDashboard(), policeLightSystem);

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
                getNpcScrollSpeedPixelsPerSecond(), getScrollOffset(), getSimulatedSpeedKmh(),
                getPlayerCar().getX());
        trafficSystem.update(deltaTime);

        /* Update all hazard spawners */
        for (RoadHazardSpawner spawner : hazardSpawners)
            spawner.update(deltaTime, getScrollOffset());

        /* Hazard particle effects — delegated to HazardEffectSystem (SRP) */
        hazardEffects.update();

        setRulesBroken(getRuleManager().getRulesBroken());

        /* Player vertical movement — delegated to PlayerController (SRP) */
        playerController.update(deltaTime);

        /* Police chase — delegated to ChaseDirector (SRP) */
        chaseDirector.update(deltaTime, getPlayerCar(),
                getSimulatedSpeedKmh(), getMaxSpeed(),
                getDashboard(), policeLightSystem);
    }

    @Override
    protected boolean isGameOver() {
        if (getPlayerCar().isFlashing())
            return false;
        if (getRulesBroken() >= GameConstants.MAX_WANTED_STARS)
            return true;
        return chaseDirector.hasCaughtPlayer();
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
        chaseDirector = null;
        playerController = null;
        hazardEffects = null;
        policeFactory = null;
        rainEffect = null;
        if (policeLightSystem != null) {
            policeLightSystem.dispose();
            policeLightSystem = null;
        }
        Gdx.app.log("Level2Scene", "Level 2 data disposed");
    }
}
