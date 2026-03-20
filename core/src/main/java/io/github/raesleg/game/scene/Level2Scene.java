package io.github.raesleg.game.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.physics.box2d.BodyDef;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.io.CommandHistory;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.entities.PoliceCar;
import io.github.raesleg.game.factory.NPCCarSpawner;
import io.github.raesleg.game.factory.PickupableSpawner;
import io.github.raesleg.game.factory.PuddleSpawner;
import io.github.raesleg.game.factory.TreeSpawner;
import io.github.raesleg.game.rules.BreakRuleCommand;
import io.github.raesleg.game.rules.RuleManager;

/**
 * Level2Scene — Highway Chase (Temple Run style).
 * <p>
 * The police car is on the player's tail from the very start.
 * The player must weave through heavy NPC traffic while maintaining
 * high speed to outrun the police. Crashing into NPC cars increases
 * police aggression (they close in faster). The level is lost when
 * the police car catches up.
 * <p>
 * <b>No crosswalks or pedestrians</b> — this is a pure expressway
 * survival level.
 *
 * <pre>
 * +---------------------------------------------------------------+
 * | SCORE: 0       [S] ----------C---------- [F]   WANTED: [*]    |
 * |                                                                |
 * |               (highway chase — rain)                           |
 * |                                                                |
 * |                                                SPEED: 0 KM/H  |
 * +---------------------------------------------------------------+
 * </pre>
 */
public class Level2Scene extends BaseGameScene {

    /* ── Level parameters ── */
    private static final float LEVEL_LENGTH = 80000f;
    private static final float MAX_SPEED = 90f;
    private static final float ACCELERATION = 50f;
    private static final float BRAKE_RATE = 70f;
    private static final float MAX_SCROLL_PXPS = 1050f;

    /* ── NPC traffic configuration (heavier than Level 1) ── */
    private static final float NPC_SPAWN_INTERVAL = 1.4f;

    private static final float ROAD_CENTRE_X = RoadRenderer.ROAD_LEFT + RoadRenderer.ROAD_WIDTH / 2f;

    /* ── Level-specific components ── */
    private NPCCarSpawner npcSpawner;
    private PuddleSpawner puddleSpawner;
    private PickupableSpawner pickupSpawner;
    private TreeSpawner treeSpawner;
    private RuleManager ruleManager;
    private CommandHistory commandHistory;
    private PoliceCar policeCar;
    private boolean policeSpawned;
    private boolean sirenStarted;

    /** Max wanted stars before game over. */
    private static final int MAX_STARS = 5;

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
        return "bgm.mp3";
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * Level-specific initialisation
     * ══════════════════════════════════════════════════════════════
     */

    @Override
    protected void initLevelData() {
        // NPC traffic spawner (denser traffic than Level 1)
        npcSpawner = new NPCCarSpawner(
                getEntityManager(), getWorld(),
                VIRTUAL_HEIGHT, NPC_SPAWN_INTERVAL);

        // Puddle spawner — coordinated with NPC lanes, no crosswalk exclusions
        puddleSpawner = new PuddleSpawner(
                getEntityManager(), getWorld(),
                VIRTUAL_HEIGHT, 3.5f,
                npcSpawner, null);

        // Pickupable spawner — collectible yellow squares
        pickupSpawner = new PickupableSpawner(
                getEntityManager(), getWorld(),
                VIRTUAL_HEIGHT, 5.0f,
                npcSpawner);

        // Tree spawner
        treeSpawner = new TreeSpawner(
                getEntityManager(), VIRTUAL_HEIGHT, 2.5f);

        // Command pattern components (traffic crashes still penalise)
        ruleManager = new RuleManager();
        commandHistory = new CommandHistory();

        // Wire traffic violation listener — traffic crashes increase wanted stars
        getCollisionHandler().setTrafficViolationListener(
                new io.github.raesleg.game.collision.GameCollisionHandler.TrafficViolationListener() {
                    @Override
                    public void onCrosswalkViolation() {
                        // No crosswalks in Level 2
                    }

                    @Override
                    public void onTrafficCrash() {
                        commandHistory.executeAndRecord(
                                new BreakRuleCommand(ruleManager, "TRAFFIC_CRASH", 1));
                        incrementCrashCount();
                        addScore(-100);
                        getSound().playSound("negative", 1.0f);
                    }

                    @Override
                    public void onPedestrianHit() {
                        // No pedestrians in Level 2
                    }

                    @Override
                    public void onPickup() {
                        addScore(50);
                        getSound().playSound("reward", 1.0f);
                    }
                });

        // Police is NOT spawned immediately — spawns on first rule break
        policeSpawned = false;

        // Collision sounds
        try {
            getSound().addSound("boundary_hit", "hit_sound.wav");
        } catch (Exception e) {
            Gdx.app.log("Level2Scene", "Could not load boundary_hit sound: " + e.getMessage());
        }
        try {
            getSound().addSound("policesiren", "policesiren.mp3");
        } catch (Exception e) {
            Gdx.app.log("Level2Scene", "Could not load policesiren sound: " + e.getMessage());
        }

        // Enable police distance mode on dashboard
        getDashboard().setPoliceDistanceMode(true);

        Gdx.app.log("Level2Scene", "Level 2 initialised — highway chase");
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * Level-specific per-frame logic
     * ══════════════════════════════════════════════════════════════
     */

    @Override
    protected void updateGame(float deltaTime) {
        if (npcSpawner != null)
            npcSpawner.update(deltaTime, getScrollOffset());
        if (puddleSpawner != null)
            puddleSpawner.update(deltaTime, getScrollOffset());
        if (pickupSpawner != null)
            pickupSpawner.update(deltaTime, getScrollOffset());
        if (treeSpawner != null)
            treeSpawner.update(deltaTime, getScrollOffset());

        setRulesBroken(ruleManager.getRulesBroken());

        // Spawn police on first rule break (not immediately)
        if (!policeSpawned && ruleManager.getRulesBroken() >= 1) {
            spawnPolice();
            policeSpawned = true;
        }

        if (policeCar != null) {
            policeCar.updateChase(
                    deltaTime,
                    getPlayerCar().getX(),
                    getPlayerCar().getY(),
                    getSimulatedSpeedKmh(),
                    getMaxSpeed(),
                    ruleManager.getPoliceAggression());

            // Police siren — start on first frame, volume scales with distance
            if (!sirenStarted) {
                getSound().loopSound("policesiren");
                sirenStarted = true;
            }
            float distance = getPlayerCar().getY() - policeCar.getScreenY();
            float maxDist = 600f;
            float sirenVol = 1.0f - Math.min(1f, Math.max(0f, distance / maxDist));
            sirenVol = Math.max(0.05f, sirenVol);
            getSound().setSoundVolume("policesiren", sirenVol);

            // Update dashboard distance meter
            float normDist = Math.min(1f, Math.max(0f, distance / maxDist));
            getDashboard().onPoliceDistanceUpdated(normDist);
        }
    }

    /** Spawns the police car below the screen at level start. */
    private void spawnPolice() {
        float centreX = ROAD_CENTRE_X / Constants.PPM;
        float startY = -200f;
        PhysicsBody policeBody = getWorld().createBody(
                BodyDef.BodyType.KinematicBody,
                centreX,
                startY / Constants.PPM,
                (80f / Constants.PPM) / 2f,
                (140f / Constants.PPM) / 2f,
                0f, 0f, true, null);
        policeCar = new PoliceCar(policeBody);
        getEntityManager().addEntity(policeCar);
        Gdx.app.log("Level2Scene", "Police spawned — chase begins!");
    }

    @Override
    protected boolean isGameOver() {
        if (getPlayerCar().isFlashing())
            return false;
        if (getRulesBroken() >= MAX_STARS)
            return true;
        return policeCar != null && policeCar.hasCaughtPlayer();
    }

    @Override
    protected String getGameOverReason() {
        if (getRulesBroken() >= MAX_STARS)
            return "Too many violations \u2014 5 wanted stars";
        return "Police caught you";
    }

    @Override
    protected String getLevelName() {
        return "Level 2 \u2014 Highway Chase";
    }

    @Override
    protected BaseGameScene createRetryScene() {
        return new Level2Scene();
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * Level-specific rendering — rain overlay
     * ══════════════════════════════════════════════════════════════
     */

    @Override
    protected void renderLevelEffects(ShapeRenderer sr, SpriteBatch batch) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.begin(ShapeRenderer.ShapeType.Filled);

        // Rain overlay — semi-transparent blue-grey wash
        sr.setColor(0.2f, 0.25f, 0.35f, 0.15f);
        sr.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        sr.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Draw puddles before entity pass (correct z-order: under player car)
        if (puddleSpawner != null) {
            batch.begin();
            puddleSpawner.render(batch);
            batch.end();
        }
    }

    @Override
    protected void disposeLevelData() {
        if (npcSpawner != null) {
            npcSpawner.clearAll();
            npcSpawner = null;
        }
        if (puddleSpawner != null) {
            puddleSpawner.clearAll();
            puddleSpawner = null;
        }
        if (pickupSpawner != null) {
            pickupSpawner.clearAll();
            pickupSpawner = null;
        }
        if (treeSpawner != null) {
            treeSpawner.clearAll();
            treeSpawner = null;
        }
        policeCar = null;
        if (commandHistory != null)
            commandHistory.clear();
        Gdx.app.log("Level2Scene", "Level 2 data disposed");
    }
}
