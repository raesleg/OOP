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
import io.github.raesleg.game.movement.SurfaceEffect;
import io.github.raesleg.game.state.ChaseDirector;
import io.github.raesleg.game.zone.RoadHazard;

// Level2Scene is an ednless chase with escalating difficulty — no distance-based win condition, but multiple lose conditions (too many violations or caught by police)
// Aggression escalation: as time passes, police get more aggressive (close in faster) and NPC traffic gets denser (spawn interval decreases) — creates natural difficulty curve without artificial "phases"
public class Level2Scene extends BaseGameScene {

    /* ── Level-specific components ── */
    private TrafficSpawningSystem trafficSystem;
    private PoliceCarFactory policeFactory;
    private ChaseDirector chaseDirector;
    private HazardEffectSystem hazardEffects;

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
         * Traffic spawning system (NPC cars, pickups) — no crosswalk exclusions
         */
        trafficSystem = new TrafficSpawningSystem(
                getEntityManager(), getWorld(), GameConstants.L2_SPAWN_SCREEN_HEIGHT,
                GameConstants.L2_NPC_SPAWN_SEC, GameConstants.L2_PICKUP_SPAWN_SEC);

        /* Road hazards (puddles, mud) — NPC spawner provides lane occupancy */
        hazardSpawners.add(new RoadHazardSpawner(
                getEntityManager(), getWorld(), GameConstants.L2_SPAWN_SCREEN_HEIGHT,
                GameConstants.L2_PUDDLE_INTERVAL,
                trafficSystem.getNpcSpawner(), null,
                SurfaceEffect.LOW_FRICTION, "puddle.png"));

        hazardSpawners.add(new RoadHazardSpawner(
                getEntityManager(), getWorld(), GameConstants.L2_SPAWN_SCREEN_HEIGHT,
                GameConstants.L2_MUD_INTERVAL,
                trafficSystem.getNpcSpawner(), null,
                SurfaceEffect.HIGH_FRICTION, "mud.png"));

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

        /* Chase director (SRP extraction — police spawn/AI/siren/distance) */
        chaseDirector = new ChaseDirector(policeFactory, getRuleManager(), getSound());

        /* Hazard particle effect system (SRP extraction) */
        hazardEffects = new HazardEffectSystem(getEntityManager(), getPlayerCar());

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

        try {
            getSound().addSound("puddle", "puddlesound.wav");
        } catch (Exception e) {
        Gdx.app.log("Level2Scene", "Could not load puddle sound: " + e.getMessage());
        }

        try {
            getSound().addSound("mud", "mudsound.wav");
        } catch (Exception e) {
        Gdx.app.log("Level2Scene", "Could not load mud sound: " + e.getMessage());
        }

        /* Dashboard — enable police distance mode */
        getDashboard().setPoliceDistanceMode(true);

        /* Start rain ambience */
        getSound().loopSound("rain");
        getSound().setSoundVolume("rain", 1.0f);

        /* Level 2 specific rules (dependency injection for popup) */
        List<String> level2Rules = new ArrayList<>();
        level2Rules.add("Avoid hitting other vehicles");
        level2Rules.add("Crashes increase police aggression");
        level2Rules.add("Watch for puddles and mud that reduce traction");
        level2Rules.add("Don't get caught by the police");
        level2Rules.add("Keep your wanted level down and maintain distance");

        getSceneManager().push(new RulesPopupScene("Level 2", level2Rules));

        Gdx.app.log("Level2Scene", "Level 2 initialised — highway chase");
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * Level-specific per-frame logic
     * ══════════════════════════════════════════════════════════════
     */

    @Override
    protected void updateGame(float deltaTime) {
        /* Difficulty escalation: increase NPC spawn frequency as time passes */
        float elapsedTime = getGameTime();
        float escalationProgress = Math.min(1.0f, elapsedTime / GameConstants.L2_DIFFICULTY_ESCALATION_TIME);
        float currentSpawnInterval = GameConstants.L2_NPC_SPAWN_MAX_INTERVAL
                - (GameConstants.L2_NPC_SPAWN_MAX_INTERVAL - GameConstants.L2_NPC_SPAWN_MIN_INTERVAL) * escalationProgress;
        trafficSystem.setNpcSpawnInterval(currentSpawnInterval);

        /* Delegate NPC/pickup/tree spawning to composed system */
        trafficSystem.setFrameState(
                getNpcScrollSpeedPixelsPerSecond(), getScrollOffset(), getSimulatedSpeedKmh(), getPlayerCar().getX());
        trafficSystem.update(deltaTime);

        /* Update all hazard spawners */
        for (RoadHazardSpawner spawner : hazardSpawners)
            spawner.update(deltaTime, getScrollOffset());

        /* Hazard particle effects — delegated to HazardEffectSystem (SRP) */
        hazardEffects.update();

        setRulesBroken(getRuleManager().getRulesBroken());

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

    @Override
    protected boolean hasProgressBasedWin() {
        return false; // Level 2 is ENDLESS — no distance-based completion
    }

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