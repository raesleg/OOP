package io.github.raesleg.game.scene;

import com.badlogic.gdx.Gdx;

/**
 * Level1Scene — Sunny road (normal traffic, no police).
 * <p>
 * Extends {@link BaseGameScene} and provides Level 1 configuration.
 * Teammates inject traffic spawning, obstacle placement, and
 * additional game rules via the {@link #initLevelData()} and
 * {@link #updateGame(float)} hooks.
 *
 * <pre>
 * +---------------------------------------------------------------+
 * | SCORE: 0       [S] ----------C---------- [F]   WANTED: [ ]    |
 * |                                                                |
 * |                        (sunny road)                            |
 * |                                                                |
 * |                                                SPEED: 0 KM/H  |
 * +---------------------------------------------------------------+
 * </pre>
 */
public class Level1Scene extends BaseGameScene {

    /* ── Level parameters ── */
    private static final float LEVEL_LENGTH = 50000f;
    private static final float MAX_SPEED = 60f;
    private static final float ACCELERATION = 95f;
    private static final float BRAKE_RATE = 160f;
    private static final float MAX_SCROLL_PXPS = 850f;

    @Override
    protected float getMaxScrollPixelsPerSecond() {
        return MAX_SCROLL_PXPS;
    }

    @Override
    protected float getLevelLength() {
        return LEVEL_LENGTH;
    }

    @Override
    protected float getMaxSpeed() {
        return MAX_SPEED;
    }

    @Override
    protected float getAcceleration() {
        return ACCELERATION;
    }

    @Override
    protected float getBrakeRate() {
        return BRAKE_RATE;
    }

    @Override
    protected String getBgmPath() {
        return "bgm.ogg";
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * Level-specific initialisation
     * ══════════════════════════════════════════════════════════════
     */

    @Override
    protected void initLevelData() {
        // Level 1: Sunny road — normal traffic, no police
        //
        // TODO: Spawn AI traffic cars (MovableEntity + AIControlled + FrictionMovement)
        // and add them to getEntityManager().
        //
        // TODO: Place MotionZone strips across lanes for surface variation
        // (e.g. MotionTuning.HIGH_FRICTION potholes).
        //
        // TODO: Register level-specific sounds via getSound().addSound(...).

        Gdx.app.log("Level1Scene", "Level 1 initialised — sunny road, no police");
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * Level-specific per-frame logic
     * ══════════════════════════════════════════════════════════════
     */

    @Override
    protected void updateGame(float deltaTime) {
        // TODO: Spawn traffic at timed intervals.
        // TODO: Check level completion (progress >= 1.0) and transition
        // to a results / next-level scene via getSceneManager().set(...).
    }

    /*
     * Level 1 has no special visual effects — the default sunny road
     * rendered by BaseGameScene is sufficient. renderLevelEffects() and
     * disposeLevelData() use their inherited no-op defaults.
     */
}
