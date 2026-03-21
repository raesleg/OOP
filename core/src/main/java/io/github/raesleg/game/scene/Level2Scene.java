package io.github.raesleg.game.scene;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.io.CommandHistory;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.collision.listeners.TrafficViolationListener;
import io.github.raesleg.game.entities.misc.Pedestrian;
import io.github.raesleg.game.entities.vehicles.PoliceCar;
import io.github.raesleg.game.factory.NPCCarSpawner;
import io.github.raesleg.game.factory.PickupableSpawner;
import io.github.raesleg.game.factory.RoadHazardSpawner;
import io.github.raesleg.game.factory.TreeSpawner;
import io.github.raesleg.game.movement.SurfaceEffect;
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
    private PickupableSpawner pickupSpawner;
    private TreeSpawner treeSpawner;
    private RuleManager ruleManager;
    private CommandHistory commandHistory;
    private PoliceCar policeCar;
    private boolean policeSpawned;
    private boolean sirenStarted;

    // All road hazards (puddles, oil spills) in one list —
    // each entry is a RoadHazardSpawner configured with a different SurfaceEffect.
    // No separate puddleSpawner/oilSpawner fields needed.
    private final List<RoadHazardSpawner> hazardSpawners = new ArrayList<>();

    /** Max wanted stars before game over. */
    private static final int MAX_STARS = 5;

    /* ── Rain rendering state ── */
    private static final int DROP_COUNT = 150;
    private final float[] dropX   = new float[DROP_COUNT];
    private final float[] dropY   = new float[DROP_COUNT];
    private final float[] dropLen = new float[DROP_COUNT];
    private final float[] dropSpd = new float[DROP_COUNT];
    private boolean dropsReady = false;
    private float rainTime = 0f; // for vignette pulse

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

        // Register all hazard types — just add more entries here for new hazards
        hazardSpawners.add(new RoadHazardSpawner(
                getEntityManager(), getWorld(), VIRTUAL_HEIGHT, 3.5f,
                npcSpawner, null,
                SurfaceEffect.PUDDLE, "puddle.png"));
 
        hazardSpawners.add(new RoadHazardSpawner(
                getEntityManager(), getWorld(), VIRTUAL_HEIGHT, 6.0f,
                npcSpawner, null,
                SurfaceEffect.MUD, "mud.png"));
 
        npcSpawner.setHazardSpawner(hazardSpawners.get(1));

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
                new TrafficViolationListener() {
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
                    }
                    
                    @Override
                    public void onPedestrianHit(Pedestrian pedestrian, Vector2 knockbackDirection, float knockbackForce) {
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

        // Rain sound
        try {
            getSound().addSound("rain", "rainsound.wav");
        } catch (Exception e) {
            Gdx.app.log("Level2Scene", "Could not load rain sound: " + e.getMessage());
        }

        // Enable police distance mode on dashboard
        getDashboard().setPoliceDistanceMode(true);

        // Enable rain when level starts
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
        if (npcSpawner != null)
            npcSpawner.update(deltaTime, getNpcScrollSpeedPixelsPerSecond());

        // Update all hazard spawners in one loop — no if/else per type
        for (RoadHazardSpawner spawner : hazardSpawners)
            spawner.update(deltaTime, getScrollOffset());

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

    /** Spawns the police car just below the visible screen. */
    private void spawnPolice() {
        float centreX = ROAD_CENTRE_X / Constants.PPM;
        float startY = -50f;
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
     * Level2-specific rendering — rain overlay
     * ══════════════════════════════════════════════════════════════
     */

    @Override
    protected void renderLevelEffects(ShapeRenderer sr, SpriteBatch batch) {
 
        // Init rain drops once
        if (!dropsReady) {
            for (int i = 0; i < DROP_COUNT; i++) {
                dropX[i]   = MathUtils.random(0f, VIRTUAL_WIDTH);
                dropY[i]   = MathUtils.random(0f, VIRTUAL_HEIGHT);
                dropLen[i] = MathUtils.random(10f, 28f);
                dropSpd[i] = MathUtils.random(500f, 900f);
            }
            dropsReady = true;
        }
 
        float dt = Gdx.graphics.getDeltaTime();
        rainTime += dt;
 
        for (int i = 0; i < DROP_COUNT; i++) {
            dropY[i] -= dropSpd[i] * dt;
            dropX[i] -= dropSpd[i] * 0.12f * dt;
            if (dropY[i] < -dropLen[i]) {
                dropY[i] = VIRTUAL_HEIGHT + dropLen[i];
                dropX[i] = MathUtils.random(0f, VIRTUAL_WIDTH);
            }
        }
 
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
 
        sr.begin(ShapeRenderer.ShapeType.Filled);
 
        // 1. Subtle blue-grey atmosphere — NOT so dark it creates black bars
        sr.setColor(0.10f, 0.13f, 0.22f, 0.22f);
        sr.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
 
        // 2. "Wet lens" blur fake — multiple tiny offset passes smear the image
        //    Use very low alpha so they layer without creating solid blocks
        for (int pass = 0; pass < 5; pass++) {
            float ox = MathUtils.sin(pass * 1.3f) * 2.5f;
            float oy = MathUtils.cos(pass * 1.1f) * 2.5f;
            sr.setColor(0.12f, 0.18f, 0.28f, 0.045f);
            sr.rect(ox, oy, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        }
 
        // 3. Soft vignette — only darken the very edges, fade quickly inward
        //    Thin strips so no harsh black column effect
        float vigAlpha = 0.40f + MathUtils.sin(rainTime * 1.6f) * 0.04f;
 
        // Side strips — only 45px wide, soft alpha
        sr.setColor(0.02f, 0.03f, 0.06f, vigAlpha * 0.7f);
        sr.rect(0, 0, 45f, VIRTUAL_HEIGHT);
        sr.rect(VIRTUAL_WIDTH - 45f, 0, 45f, VIRTUAL_HEIGHT);
 
        // Top/bottom strips
        sr.setColor(0.02f, 0.03f, 0.06f, vigAlpha * 0.8f);
        sr.rect(0, VIRTUAL_HEIGHT - 55f, VIRTUAL_WIDTH, 55f);
        sr.rect(0, 0, VIRTUAL_WIDTH, 45f);
 
        sr.end();
 
        // 4. Rain streaks
        sr.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < DROP_COUNT; i++) {
            float alpha = MathUtils.random(0.28f, 0.60f);
            sr.setColor(0.72f, 0.84f, 1.0f, alpha);
            float endX = dropX[i] + dropLen[i] * 0.18f;
            float endY = dropY[i] + dropLen[i];
            sr.line(dropX[i], dropY[i], endX, endY);
            if (i % 5 == 0) {
                sr.setColor(0.88f, 0.94f, 1.0f, alpha * 0.45f);
                sr.line(dropX[i] + 1f, dropY[i], endX + 1f, endY);
            }
        }
        sr.end();
 
        Gdx.gl.glDisable(GL20.GL_BLEND);
 
        // Render all hazard types in one loop
        batch.begin();
        for (RoadHazardSpawner spawner : hazardSpawners)
            spawner.render(batch);
        batch.end();
    }

    @Override
    protected void disposeLevelData() {
        getSound().stopSound("rain");
        if (npcSpawner != null) {
            npcSpawner.clearAll();
            npcSpawner = null;
        }

        for (RoadHazardSpawner s : hazardSpawners) s.clearAll();
        hazardSpawners.clear();

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
