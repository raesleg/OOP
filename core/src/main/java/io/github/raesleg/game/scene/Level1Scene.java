package io.github.raesleg.game.scene;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.physics.box2d.BodyDef;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.io.CommandHistory;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.entities.Pedestrian;
import io.github.raesleg.game.entities.StopSign;
import io.github.raesleg.game.factory.NPCCarSpawner;
import io.github.raesleg.game.factory.PickupableSpawner;
import io.github.raesleg.game.factory.PuddleSpawner;
import io.github.raesleg.game.factory.TreeSpawner;
import io.github.raesleg.game.rules.BreakRuleCommand;
import io.github.raesleg.game.rules.PedestrianHitCommand;
import io.github.raesleg.game.rules.RuleManager;
import io.github.raesleg.game.zone.CrosswalkZone;

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

    /* ── NPC traffic configuration ── */
    private static final float NPC_SPAWN_INTERVAL = 2.0f;

    /* ── Pedestrian crossing configuration ── */
    private static final float CROSSWALK_HEIGHT = 80f;
    private static final float CROSSING_SPEED = 80f;
    private static final float ROAD_CENTRE_X = RoadRenderer.ROAD_LEFT + RoadRenderer.ROAD_WIDTH / 2f;

    /** World-Y positions where crosswalks are placed (scrolled into view). */
    private static final float[] CROSSING_POSITIONS = { 5000f, 15000f, 28000f, 40000f };

    /* ── Level-specific components ── */
    private NPCCarSpawner npcSpawner;
    private PuddleSpawner puddleSpawner;
    private PickupableSpawner pickupSpawner;
    private TreeSpawner treeSpawner;
    private RuleManager ruleManager;
    private CommandHistory commandHistory;
    private final List<CrosswalkZone> crosswalkZones = new ArrayList<>();
    private final List<Pedestrian> pedestrians = new ArrayList<>();
    private final List<StopSign> stopSigns = new ArrayList<>();

    private Pedestrian hitPedestrian;

    /** Max wanted stars before game over. */
    private static final int MAX_STARS = 5;

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

    @Override
    protected void initLevelData() {
        Gdx.app.log("Level1Scene", "=== INIT LEVEL DATA START ===");

        // Build exclusion zones from crosswalk positions
        List<float[]> crosswalkExclusions = new ArrayList<>();
        for (float pos : CROSSING_POSITIONS) {
            crosswalkExclusions.add(new float[] { pos - CROSSWALK_HEIGHT, pos + CROSSWALK_HEIGHT });
        }

        // NPC traffic spawner with crosswalk exclusion
        npcSpawner = new NPCCarSpawner(
                getEntityManager(),
                getWorld(),
                VIRTUAL_HEIGHT,
                NPC_SPAWN_INTERVAL,
                crosswalkExclusions);

        // Puddle spawner — puddles never on crosswalks, coordinated with NPC lanes
        puddleSpawner = new PuddleSpawner(
                getEntityManager(), getWorld(),
                VIRTUAL_HEIGHT, 4.0f,
                npcSpawner,
                crosswalkExclusions);

        // Cross-link: NPC spawner also avoids puddle lanes
        npcSpawner.setPuddleSpawner(puddleSpawner);

        // Pickupable spawner — collectible yellow squares
        pickupSpawner = new PickupableSpawner(
                getEntityManager(), getWorld(),
                VIRTUAL_HEIGHT, 5.0f,
                npcSpawner);

        // Tree spawner — decorative trees on road shoulders
        treeSpawner = new TreeSpawner(
                getEntityManager(), VIRTUAL_HEIGHT, 3.0f);

        // Command pattern components
        ruleManager = new RuleManager();
        commandHistory = new CommandHistory();

        // Wire traffic violation listener (Observer pattern via DIP)
        getCollisionHandler().setTrafficViolationListener(
                new io.github.raesleg.game.collision.GameCollisionHandler.TrafficViolationListener() {
                    @Override
                    public void onCrosswalkViolation() {
                        commandHistory.executeAndRecord(
                                new BreakRuleCommand(ruleManager, "CROSSWALK_VIOLATION", 2));
                        addScore(-100);
                        getSound().playSound("negative", 1.0f);
                    }

                    @Override
                    public void onTrafficCrash() {
                        commandHistory.executeAndRecord(
                                new BreakRuleCommand(ruleManager, "TRAFFIC_CRASH", 1));
                        incrementCrashCount();
                        addScore(-100);
                    }

                    @Override
                    public void onPedestrianHit() {
                        // Wait for flying animation before fail
                        commandHistory.executeAndRecord(
                                new PedestrianHitCommand(ruleManager));
                        //setInstantFail(true, "Hit a pedestrian and caused an accident");
                    }

                    @Override
                    public void onPickup() {
                        addScore(50);
                        getSound().playSound("reward", 1.0f);
                    }
                });

        // Spawn crosswalk zones, pedestrians, and stop signs
        for (int i = 0; i < CROSSING_POSITIONS.length; i++) {
            float worldY = CROSSING_POSITIONS[i];

            // Crosswalk sensor zone — body created here, passed to entity (SRP)
            float zoneHalfW = (RoadRenderer.ROAD_WIDTH / Constants.PPM) / 2f;
            float zoneHalfH = (CROSSWALK_HEIGHT / Constants.PPM) / 2f;
            PhysicsBody zoneBody = getWorld().createBody(
                    BodyDef.BodyType.KinematicBody,
                    ROAD_CENTRE_X / Constants.PPM,
                    worldY / Constants.PPM,
                    zoneHalfW, zoneHalfH,
                    0f, 0f, true, null);
            CrosswalkZone zone = new CrosswalkZone(
                    ROAD_CENTRE_X, worldY,
                    RoadRenderer.ROAD_WIDTH, CROSSWALK_HEIGHT, zoneBody);
            crosswalkZones.add(zone);
            getEntityManager().addEntity(zone);

            // Pedestrian crossing the road
            float direction = (i % 2 == 0) ? 1f : -1f;
            float pedHalfW = 40f / Constants.PPM / 2f;
            float pedHalfH = 40f / Constants.PPM / 2f;
            PhysicsBody pedBody = getWorld().createBody(
                BodyDef.BodyType.DynamicBody, // Changed to dynamic for physics response
                ROAD_CENTRE_X / Constants.PPM,
                worldY / Constants.PPM,
                pedHalfW, pedHalfH,
                1f,     // density (gives it mass)
                0f,     // friction
                false,  // NOT a sensor - creates real collision
                null
            );
            // Set high damping (test)
            pedBody.setLinearDamping(999f);

            Pedestrian ped = new Pedestrian(worldY, direction, CROSSING_SPEED, pedBody);
            pedestrians.add(ped);
            getEntityManager().addEntity(ped);

            // Pair the zone with its pedestrian so violations only fire
            // while the pedestrian is actively crossing (Observer pattern)
            zone.setPedestrian(ped);

            // Stop sign on the left shoulder
            StopSign sign = new StopSign(RoadRenderer.ROAD_LEFT - 130f, worldY - 30f);
            stopSigns.add(sign);
            getEntityManager().addEntity(sign);
        }

        // Collision sounds
        try {
            getSound().addSound("boundary_hit", "collide_sound.wav");
        } catch (Exception e) {
            Gdx.app.log("Level1Scene", "Could not load boundary_hit sound: " + e.getMessage());
        }
        try {
            getSound().addSound("crash", "collide_sound.wav");
        } catch (Exception e) {
            Gdx.app.log("Level1Scene", "Could not load crash sound: " + e.getMessage());
        }

        Gdx.app.log("Level1Scene", "=== INIT LEVEL DATA COMPLETE ===");
    }


    @Override
    protected void updateGame(float deltaTime) {
        // NPC traffic
        if (npcSpawner != null) {
            npcSpawner.update(deltaTime, getScrollOffset());
        }
        if (puddleSpawner != null) {
            puddleSpawner.update(deltaTime, getScrollOffset());
        }
        if (pickupSpawner != null) {
            pickupSpawner.update(deltaTime, getScrollOffset());
        }
        if (treeSpawner != null) {
            treeSpawner.update(deltaTime, getScrollOffset());
        }

        float scroll = getScrollOffset();

        // Update crosswalk zones — check expired first to avoid operating on disposed
        // entities
        Iterator<CrosswalkZone> zoneIter = crosswalkZones.iterator();
        while (zoneIter.hasNext()) {
            CrosswalkZone zone = zoneIter.next();
            if (zone.isExpired()) {
                zoneIter.remove();
                continue;
            }
            zone.updatePosition(scroll);
        }

        // Update pedestrians — check expired first to avoid operating on disposed
        // entities
        Iterator<Pedestrian> pedIter = pedestrians.iterator();
        while (pedIter.hasNext()) {
            Pedestrian ped = pedIter.next();
            if (ped.isExpired()) {
                // Reward the player when a pedestrian crosses safely
                if (ped.hasCrossedSuccessfully()) {
                    addScore(200);
                    getSound().playSound("reward", 1.0f);
                }
                // Check if this was a hit pedestrian finishing their flight
                else if (ped.isFlying() && ped == hitPedestrian) {
                    // Flying animation complete, trigger game over NOW
                    setInstantFail(true, "Hit a pedestrian and caused an accident");
                    hitPedestrian = null;
                }
                pedIter.remove();
                continue;
            }
            
            // Track which pedestrian is flying ( for delayed game over )
            if (ped.isFlying() && hitPedestrian == null) {
                hitPedestrian = ped;
            }

            ped.updatePosition(scroll, deltaTime);
        }

        // Update stop signs — check expired first
        Iterator<StopSign> signIter = stopSigns.iterator();
        while (signIter.hasNext()) {
            StopSign sign = signIter.next();
            if (sign.isExpired()) {
                signIter.remove();
                continue;
            }
            sign.updatePosition(scroll);
        }

        // Sync WANTED meter
        setRulesBroken(ruleManager.getRulesBroken());
        // if (ruleManager.isInstantFail()) {
        //     setInstantFail(true, "Hit a pedestrian and caused an accident");
        // }
    }

    @Override
    protected void renderLevelEffects(ShapeRenderer sr, SpriteBatch batch) {
        // Draw crosswalk stripe zones (Shape entities aren't rendered by EntityManager)
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (CrosswalkZone zone : crosswalkZones) {
            zone.draw(sr);
        }
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
    protected boolean isGameOver() {
        return getRulesBroken() >= MAX_STARS;
    }

    @Override
    protected String getGameOverReason() {
        return "Too many violations \u2014 5 wanted stars";
    }

    @Override
    protected String getLevelName() {
        return "Level 1 \u2014 Sunny Road";
    }

    @Override
    protected BaseGameScene createRetryScene() {
        return new Level1Scene();
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

        // Clear local references only — EntityManager owns entity disposal
        crosswalkZones.clear();
        pedestrians.clear();
        stopSigns.clear();

        if (commandHistory != null)
            commandHistory.clear();

        Gdx.app.log("Level1Scene", "Level 1 data disposed");
    }
}
