package io.github.raesleg.game.scene;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import io.github.raesleg.game.GameConstants;
import io.github.raesleg.game.collision.listeners.Level1TrafficListener;

// Level 1: Sunny Road with crosswalks, pedestrians, and stop signs
// Focuses on basic driving and introduces crosswalk mechanics
public class Level1Scene extends BaseGameScene {

    /* ── Composed systems (SRP) ── */
    private TrafficSpawningSystem trafficSystem;
    private CrosswalkEncounterSystem crosswalkSystem;
    private CrosswalkRenderer crosswalkRenderer;

    @Override
    protected float getMaxScrollPixelsPerSecond() {
        return GameConstants.L1_MAX_SCROLL;
    }

    @Override
    protected float getLevelLength() {
        return GameConstants.L1_LEVEL_LENGTH;
    }

    @Override
    protected float getMaxSpeed() {
        return GameConstants.L1_MAX_SPEED;
    }

    @Override
    protected float getAcceleration() {
        return GameConstants.L1_ACCELERATION;
    }

    @Override
    protected float getBrakeRate() {
        return GameConstants.L1_BRAKE_RATE;
    }

    @Override
    protected String getBgmPath() {
        return GameConstants.BGM_DEFAULT;
    }

    @Override
    protected void initLevelData() {
        Gdx.app.log("Level1Scene", "=== INIT LEVEL DATA START ===");

        /*
         * Register crash explosion condition — Level 1 explodes after 3 crashes.
         * Delegates evaluation to MatchDirector (SRP).
         */
        addEndCondition(() -> getMatchDirector().evaluateCrashExplosion(
                getPlayerCar().getX() + getPlayerCar().getW() / 2f,
                getPlayerCar().getY() + getPlayerCar().getH() / 2f));

        /* Build crosswalk exclusion zones for spawner */
        List<float[]> crosswalkExclusions = new ArrayList<>();
        for (float pos : GameConstants.L1_CROSSING_POSITIONS) {
            crosswalkExclusions.add(new float[] {
                    pos - GameConstants.L1_CROSSWALK_HEIGHT,
                    pos + GameConstants.L1_CROSSWALK_HEIGHT
            });
        }

        /* Traffic spawning system (NPC cars, pickups) */
        trafficSystem = new TrafficSpawningSystem(
                getEntityManager(), getWorld(), GameConstants.SPAWN_SCREEN_HEIGHT,
                GameConstants.L1_NPC_SPAWN_SEC, 6.0f, crosswalkExclusions);

        /* Crosswalk encounter system (zones, pedestrians, stop signs) */
        crosswalkSystem = new CrosswalkEncounterSystem(
                getEntityManager(), getWorld(), getSound(), getEventBus(),
                getRuleManager(), getCommandHistory());
        crosswalkSystem.initCrosswalks(GameConstants.L1_CROSSING_POSITIONS);

        /* Crosswalk renderer (SRP — rendering extracted from scene) */
        crosswalkRenderer = new CrosswalkRenderer();

        /* Wire collision listeners — extracted standalone classes (SRP + DIP) */
        getCollisionHandler().setTrafficViolationListener(
                new Level1TrafficListener(
                        getRuleManager(), getCommandHistory(), getSound(),
                        crosswalkSystem, this::addScore, this::incrementCrashCount));
        getCollisionHandler().setPickupListener(this::handlePickup);

        /* Level-specific sounds */
        try {
            getSound().addSound("boundary_hit", GameConstants.SFX_BOUNDARY_HIT);
        } catch (Exception e) {
            Gdx.app.log("Level1Scene", "Sound load fail: " + e.getMessage());
        }
        try {
            getSound().addSound("crash", GameConstants.SFX_CRASH);
        } catch (Exception e) {
            Gdx.app.log("Level1Scene", "Sound load fail: " + e.getMessage());
        }
        try {
            getSound().addSound("pedestrain_hit", GameConstants.SFX_PEDESTRIAN_HIT);
        } catch (Exception e) {
            Gdx.app.log("Level1Scene", "Sound load fail: " + e.getMessage());
        }
        try {
            getSound().addSound("scream", GameConstants.SFX_SCREAM);
        } catch (Exception e) {
            Gdx.app.log("Level1Scene", "Sound load fail: " + e.getMessage());
        }

        /* Level 1 specific rules (dependency injection for popup) */
        List<String> level1Rules = new ArrayList<>();
        level1Rules.add("Slow down near crossings");
        level1Rules.add("Stop if pedestrians are present, and wait for them to cross");
        level1Rules.add("Do not hit pedestrians");
        level1Rules.add("Do not crash into other vehicles");
        level1Rules.add("Keep an eye on fuel levels");

        getSceneManager().push(new RulesPopupScene("Level 1", level1Rules));

        Gdx.app.log("Level1Scene", "=== INIT LEVEL DATA COMPLETE ===");
    }

    @Override
    protected void updateGame(float deltaTime) {
        /* Supply frame state to systems */
        crosswalkSystem.setFrameState(getScrollOffset(), getSimulatedSpeedKmh());
        crosswalkSystem.update(deltaTime);

        /* Suppress NPC spawning while a crosswalk encounter is active on screen */
        trafficSystem.setSpawningEnabled(!crosswalkSystem.isCrosswalkActiveOnScreen());
        trafficSystem.setFrameState(
                getNpcScrollSpeedPixelsPerSecond(), getScrollOffset(), getSimulatedSpeedKmh(), getPlayerCar().getX());
        trafficSystem.update(deltaTime);

        /* Sync rule count to base scene */
        setRulesBroken(getRuleManager().getRulesBroken());

        /* Check if crosswalk system triggered an instant fail */
        if (crosswalkSystem.isInstantFailTriggered()) {
            setInstantFail(true, crosswalkSystem.getInstantFailReason());
        }
    }

    @Override
    protected void renderLevelEffects(ShapeRenderer sr, SpriteBatch batch) {
        crosswalkRenderer.render(sr, crosswalkSystem.getCrosswalkZones());
    }

    @Override
    protected boolean isGameOver() {
        return getRulesBroken() >= GameConstants.MAX_WANTED_STARS;
    }

    @Override
    protected String getGameOverReason() {
        return "Too many violations — " + GameConstants.MAX_WANTED_STARS + " wanted stars";
    }

    @Override
    protected String getLevelName() {
        return "Level 1 — Sunny Road";
    }

    @Override
    protected List<String> getViolationLog() {
        return (getRuleManager() != null) ? getRuleManager().getViolationLog() : List.of();
    }

    @Override
    protected BaseGameScene createRetryScene() {
        return new Level1Scene();
    }

    @Override
    protected void disposeLevelData() {
        if (trafficSystem != null) {
            trafficSystem.dispose();
            trafficSystem = null;
        }
        if (crosswalkSystem != null) {
            crosswalkSystem.dispose();
            crosswalkSystem = null;
        }
        Gdx.app.log("Level1Scene", "Level 1 data disposed");
    }
}