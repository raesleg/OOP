package io.github.raesleg.game.scene;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.io.CommandHistory;
import io.github.raesleg.engine.physics.PhysicsBody;

import io.github.raesleg.game.entities.misc.Pedestrian;
import io.github.raesleg.game.entities.misc.StopSign;
import io.github.raesleg.game.factory.NPCCarSpawner;
import io.github.raesleg.game.factory.PickupableSpawner;
import io.github.raesleg.game.factory.TreeSpawner;
import io.github.raesleg.game.collision.PedestrianHitReaction;
import io.github.raesleg.game.collision.listeners.TrafficViolationListener;
import io.github.raesleg.game.movement.PedestrianIntent;
import io.github.raesleg.game.movement.PedestrianMovement;
import io.github.raesleg.game.rules.BreakRuleCommand;
import io.github.raesleg.game.rules.PedestrianHitCommand;
import io.github.raesleg.game.rules.RuleManager;
import io.github.raesleg.game.zone.CrosswalkZone;

public class Level1Scene extends BaseGameScene {

    private static final float LEVEL_LENGTH = 50000f;
    private static final float MAX_SPEED = 60f;
    private static final float ACCELERATION = 95f;
    private static final float BRAKE_RATE = 160f;
    private static final float MAX_SCROLL_PXPS = 850f;

    private static final float NPC_SPAWN_INTERVAL = 2.0f;

    private static final float CROSSWALK_HEIGHT = 80f;
    private static final float CROSSING_SPEED = 80f;
    private static final float ROAD_CENTRE_X = RoadRenderer.ROAD_LEFT + RoadRenderer.ROAD_WIDTH / 2f;

    private static final float[] CROSSING_POSITIONS = { 5000f, 15000f, 28000f, 40000f };

    private static final int MAX_STARS = 5;

    private NPCCarSpawner npcSpawner;
    private PickupableSpawner pickupSpawner;
    private TreeSpawner treeSpawner;
    private RuleManager ruleManager;
    private CommandHistory commandHistory;

    private final List<CrosswalkZone> crosswalkZones = new ArrayList<>();
    private final List<StopSign> stopSigns = new ArrayList<>();
    private final List<PedestrianEncounter> pedestrianEncounters = new ArrayList<>();

    private static final class PedestrianEncounter {
        private final Pedestrian pedestrian;
        private final PedestrianIntent intent;
        private final PedestrianMovement movement;
        private final PedestrianHitReaction hitReaction;
        private final CrosswalkZone zone;
        private boolean rewarded;
        private boolean crashHandled;
        private boolean failQueued;

        private PedestrianEncounter(
                Pedestrian pedestrian,
                PedestrianIntent intent,
                PedestrianMovement movement,
                PedestrianHitReaction hitReaction,
                CrosswalkZone zone) {
            this.pedestrian = pedestrian;
            this.intent = intent;
            this.movement = movement;
            this.hitReaction = hitReaction;
            this.zone = zone;
            this.rewarded = false;
            this.crashHandled = false;
            this.failQueued = false;
        }
    }

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

    @Override
    protected void initLevelData() {
        Gdx.app.log("Level1Scene", "=== INIT LEVEL DATA START ===");

        List<float[]> crosswalkExclusions = new ArrayList<>();
        for (float pos : CROSSING_POSITIONS) {
            crosswalkExclusions.add(new float[] { pos - CROSSWALK_HEIGHT, pos + CROSSWALK_HEIGHT });
        }

        npcSpawner = new NPCCarSpawner(
                getEntityManager(),
                getWorld(),
                VIRTUAL_HEIGHT,
                NPC_SPAWN_INTERVAL,
                crosswalkExclusions);

        pickupSpawner = new PickupableSpawner(
                getEntityManager(),
                getWorld(),
                VIRTUAL_HEIGHT,
                6.0f,
                npcSpawner);

        treeSpawner = new TreeSpawner(
                getEntityManager(),
                VIRTUAL_HEIGHT,
                2.5f);

        ruleManager = new RuleManager();
        commandHistory = new CommandHistory();

        getCollisionHandler().setTrafficViolationListener(
                new TrafficViolationListener() {
                    @Override
                    public void onCrosswalkViolation() {
                        commandHistory.executeAndRecord(
                                new BreakRuleCommand(ruleManager, "CROSSWALK", 1));
                        addScore(-50);
                    }

                    @Override
                    public void onTrafficCrash() {
                        commandHistory.executeAndRecord(
                                new BreakRuleCommand(ruleManager, "TRAFFIC_CRASH", 1));
                        incrementCrashCount();
                        addScore(-100);
                    }

                    @Override
                    public void onPedestrianHit(Pedestrian pedestrian, Vector2 knockbackDirection,
                            float knockbackForce) {
                        commandHistory.executeAndRecord(new PedestrianHitCommand(ruleManager));
                        getSound().playSound("pedestrain_hit", 1.0f);
                        triggerPedestrianHit(pedestrian, knockbackDirection, knockbackForce);
                    }

                    @Override
                    public void onPickup() {
                        addScore(50);
                        getSound().playSound("reward", 1.0f);
                    }
                });

        for (int i = 0; i < CROSSING_POSITIONS.length; i++) {
            float worldY = CROSSING_POSITIONS[i];

            CrosswalkZone zone = createCrosswalkZone(worldY);
            crosswalkZones.add(zone);
            getEntityManager().addEntity(zone);

            PedestrianEncounter encounter = createPedestrianEncounter(i, worldY, zone);
            pedestrianEncounters.add(encounter);
            getEntityManager().addEntity(encounter.pedestrian);

            StopSign sign = new StopSign(RoadRenderer.ROAD_LEFT - 130f, worldY - 30f);
            stopSigns.add(sign);
            getEntityManager().addEntity(sign);
        }

        try {
            getSound().addSound("boundary_hit", "crash_sound.wav");
        } catch (Exception e) {
            Gdx.app.log("Level1Scene", "Could not load boundary_hit sound: " + e.getMessage());
        }

        try {
            getSound().addSound("crash", "crash_sound.wav");
        } catch (Exception e) {
            Gdx.app.log("Level1Scene", "Could not load crash sound: " + e.getMessage());
        }

        try {
            getSound().addSound("pedestrain_hit", "pedestrain_hit.wav");
        } catch (Exception e) {
            Gdx.app.log("Level1Scene", "Could not load pedestrain_hit sound: " + e.getMessage());
        }

        try {
            getSound().addSound("scream", "scream.mp3");
        } catch (Exception e) {
            Gdx.app.log("Level1Scene", "Could not load scream sound: " + e.getMessage());
        }


        Gdx.app.log("Level1Scene", "=== INIT LEVEL DATA COMPLETE ===");
    }

    private CrosswalkZone createCrosswalkZone(float worldY) {
        float zoneHalfW = (RoadRenderer.ROAD_WIDTH / Constants.PPM) / 2f;
        float zoneHalfH = (CROSSWALK_HEIGHT / Constants.PPM) / 2f;

        PhysicsBody zoneBody = getWorld().createBody(
                BodyDef.BodyType.DynamicBody,
                ROAD_CENTRE_X / Constants.PPM,
                worldY / Constants.PPM,
                zoneHalfW,
                zoneHalfH,
                0f,
                0f,
                true,
                null);

        return new CrosswalkZone(
                ROAD_CENTRE_X,
                worldY,
                RoadRenderer.ROAD_WIDTH,
                CROSSWALK_HEIGHT,
                zoneBody);
    }

    private PedestrianEncounter createPedestrianEncounter(int index, float worldY, CrosswalkZone zone) {
        float direction = (index % 2 == 0) ? 1f : -1f;

        float pedW = 80f;
        float pedH = 80f;

        float pedStartX = (direction > 0f)
                ? RoadRenderer.ROAD_LEFT - pedW
                : RoadRenderer.ROAD_RIGHT + pedW;

        float pedHalfW = (pedW / Constants.PPM) / 2f;
        float pedHalfH = (pedH / Constants.PPM) / 2f;

        // Hitbox is smaller than sprite — just the person's body, not the full tile
        float hitboxHalfW = pedHalfW * 0.4f;
        float hitboxHalfH = pedHalfH * 0.4f;

        PhysicsBody pedBody = getWorld().createBody(
            BodyDef.BodyType.DynamicBody,
            (pedStartX + pedW / 2f) / Constants.PPM,
            (worldY + pedH / 2f) / Constants.PPM,
            hitboxHalfW,
            hitboxHalfH,
            0f,
            0f,
            false,
            null);

        Pedestrian pedestrian = new Pedestrian(pedStartX, worldY, pedW, pedH, pedBody);
        PedestrianIntent intent = new PedestrianIntent(direction);
        PedestrianMovement movement = new PedestrianMovement(CROSSING_SPEED, pedW);
        PedestrianHitReaction hitReaction = new PedestrianHitReaction();

        return new PedestrianEncounter(
                pedestrian,
                intent,
                movement,
                hitReaction,
                zone);
    }

    private void triggerPedestrianHit(Pedestrian hitPedestrian, Vector2 knockbackDirection, float knockbackForce) {
        if (hitPedestrian == null) {
            return;
        }

        for (PedestrianEncounter encounter : pedestrianEncounters) {
            if (encounter.pedestrian == hitPedestrian) {
                if (!encounter.hitReaction.isActive() && !encounter.hitReaction.isFinished()) {
                    encounter.movement.markFinishedUnsuccessfully();
                    encounter.zone.setCrossingActive(false);
                    encounter.failQueued = true;
                    encounter.hitReaction.trigger(hitPedestrian, knockbackDirection, knockbackForce);
                }
                break;
            }
        }
    }

    @Override
    protected void updateGame(float deltaTime) {
        if (npcSpawner != null) {
            npcSpawner.update(deltaTime, getNpcScrollSpeedPixelsPerSecond());
        }
        if (pickupSpawner != null) {
            pickupSpawner.update(deltaTime, getScrollOffset());
        }
        if (treeSpawner != null) {
            treeSpawner.update(deltaTime, getScrollOffset());
        }

        setRulesBroken(ruleManager.getRulesBroken());

        float scroll = getScrollOffset();

        Iterator<CrosswalkZone> zoneIter = crosswalkZones.iterator();
        while (zoneIter.hasNext()) {
            CrosswalkZone zone = zoneIter.next();
            if (zone.isExpired()) {
                zoneIter.remove();
                continue;
            }
            zone.updatePosition(scroll);
        }

        Iterator<PedestrianEncounter> encounterIter = pedestrianEncounters.iterator();
        while (encounterIter.hasNext()) {
            PedestrianEncounter encounter = encounterIter.next();
            Pedestrian pedestrian = encounter.pedestrian;
            CrosswalkZone zone = encounter.zone;


            if (pedestrian.isExpired() || zone.isExpired()) {
                encounterIter.remove();
                continue;
            }

            float screenY = pedestrian.getRelativeY() + scroll;
            boolean visible = screenY > -100f && screenY < VIRTUAL_HEIGHT + 100f;

            if (visible
                    && !encounter.movement.isActive()
                    && !encounter.movement.isFinished()
                    && !encounter.hitReaction.isActive()
                    && !encounter.failQueued) {
                encounter.movement.activate();
                zone.setCrossingActive(true);
            }

            if (encounter.hitReaction.isActive()) {
                encounter.hitReaction.update(pedestrian, deltaTime);
                zone.setCrossingActive(false);
            }

            // Check for fail: either animation just finished, or failQueued with no active reaction
            if (encounter.failQueued && !encounter.crashHandled
                    && (encounter.hitReaction.isFinished() || !encounter.hitReaction.isActive())) {
                encounter.crashHandled = true;
                encounter.failQueued = false;
                zone.markExpired();
                pedestrian.markExpired();
                encounterIter.remove();
                Gdx.app.log("Level1Scene", "Setting instant fail after pedestrian hit");
                setInstantFail(true, "Hit a pedestrian and caused an accident");
                return;
            } else if (!encounter.hitReaction.isActive() && !encounter.failQueued) {
                pedestrian.updateScreenPosition(scroll);
                pedestrian.resetRenderRotation();
                pedestrian.syncBodyToSprite();
                encounter.movement.update(pedestrian, encounter.intent, deltaTime);
            }

            if (!encounter.failQueued
                    && !encounter.movement.isFinished()
                    && !encounter.hitReaction.isActive()
                    && encounter.movement.hasReachedFinish(pedestrian)) {
                encounter.movement.markFinishedSuccessfully();
                zone.setCrossingActive(false);

                if (!encounter.rewarded) {
                    encounter.rewarded = true;
                    addScore(200);
                    getSound().playSound("reward", 1.0f);
                }

                pedestrian.markExpired();
                zone.markExpired();
            }
        }
    }

    @Override
    protected void renderLevelEffects(ShapeRenderer sr, SpriteBatch batch) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (CrosswalkZone zone : crosswalkZones) {
            zone.draw(sr);
        }
        sr.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    protected boolean isGameOver() {
        return getRulesBroken() >= MAX_STARS;
    }

    @Override
    protected String getGameOverReason() {
        return "Too many violations — 5 wanted stars";
    }

    @Override
    protected String getLevelName() {
        return "Level 1 — Sunny Road";
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
        if (pickupSpawner != null) {
            pickupSpawner.clearAll();
            pickupSpawner = null;
        }
        if (treeSpawner != null) {
            treeSpawner.clearAll();
            treeSpawner = null;
        }

        crosswalkZones.clear();
        stopSigns.clear();
        pedestrianEncounters.clear();

        if (commandHistory != null) {
            commandHistory.clear();
        }

        Gdx.app.log("Level1Scene", "Level 1 data disposed");
    }
}