package io.github.raesleg.demo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Level2Scene — Raining expressway (police chase enabled).
 * <p>
 * Extends {@link BaseGameScene} with higher speeds, wet-road conditions,
 * and a police-chase mechanic that activates when the WANTED level
 * reaches the threshold ({@value #WANTED_THRESHOLD} rule breaks).
 *
 * <pre>
 * +---------------------------------------------------------------+
 * | SCORE: 0       [S] ----------C---------- [F]   WANTED: [ ]    |
 * |                                                                |
 * |                     (rain expressway)                          |
 * |                                                                |
 * |                                                SPEED: 0 KM/H  |
 * +---------------------------------------------------------------+
 * </pre>
 */
public class Level2Scene extends BaseGameScene {

    /* ── Level parameters ── */
    private static final float LEVEL_LENGTH = 80000f;
    private static final float MAX_SPEED = 250f;
    private static final float ACCELERATION = 50f;
    private static final float BRAKE_RATE = 70f;
    private static final int WANTED_THRESHOLD = 3;

    private boolean policeChaseActive;

    public Level2Scene() {
        super();
        this.policeChaseActive = false;
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
        // Level 2: Raining expressway — police enabled
        //
        // TODO: Spawn AI traffic cars with higher lane speeds.
        //
        // TODO: Place MotionZone strips with MotionTuning.LOW_TRACTION
        // to simulate wet road patches.
        //
        // TODO: Pre-create a police MovableEntity (initially offscreen)
        // with AIControlled pursuit behaviour. Add to EntityManager
        // but keep inactive until policeChaseActive flips.
        //
        // TODO: Register rain-specific sounds (e.g. "rain_loop") via
        // getSound().addSound(...).

        Gdx.app.log("Level2Scene", "Level 2 initialised — rain expressway, police enabled");
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * Level-specific per-frame logic
     * ══════════════════════════════════════════════════════════════
     */

    @Override
    protected void updateGame(float deltaTime) {
        /* Police chase activates when WANTED reaches threshold */
        if (!policeChaseActive && getRulesBroken() >= WANTED_THRESHOLD) {
            policeChaseActive = true;
            Gdx.app.log("Level2Scene", "Police chase activated!");
            // TODO: Activate police car entity — set its AIControlled
            // source to pursue the player car.
        }

        if (policeChaseActive) {
            // TODO: Update police pursuit AI logic.
            // TODO: Check if police catches player (game over condition).
        }

        // TODO: Spawn traffic at timed intervals.
        // TODO: Check level completion (progress >= 1.0).
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * Level-specific rendering — rain overlay
     * ══════════════════════════════════════════════════════════════
     */

    @Override
    protected void renderLevelEffects(ShapeRenderer sr, SpriteBatch batch) {
        /* Semi-transparent blue-grey wash to simulate rain */
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(0.2f, 0.25f, 0.35f, 0.15f);
        sr.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        sr.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}
