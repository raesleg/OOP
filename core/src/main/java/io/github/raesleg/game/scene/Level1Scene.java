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
import io.github.raesleg.game.collision.GameCollisionHandler;
import io.github.raesleg.game.entities.Pedestrian;
import io.github.raesleg.game.entities.StopSign;
import io.github.raesleg.game.factory.NPCCarSpawner;
import io.github.raesleg.game.factory.PickupableSpawner;
import io.github.raesleg.game.factory.PuddleSpawner;
import io.github.raesleg.game.factory.TreeSpawner;
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
    private PuddleSpawner puddleSpawner;
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
        private final CrosswalkZone zone;
        private final float relativeY;
        private boolean rewarded;

        private PedestrianEncounter(
                Pedestrian pedestrian,
                PedestrianIntent intent,
                PedestrianMovement movement,
                CrosswalkZone zone,
                float relativeY) {
            this.pedestrian = pedestrian;
            this.intent = intent;
            this.movement = movement;
            this.zone = zone;
            this.relativeY = relativeY;
            this.rewarded = false;
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

        puddleSpawner = new PuddleSpawner(
                getEntityManager(),
                getWorld(),
                VIRTUAL_HEIGHT,
                4.0f,
                npcSpawner,
                crosswalkExclusions);

        npcSpawner.setPuddleSpawner(puddleSpawner);

        pickupSpawner = new PickupableSpawner(
                getEntityManager(),
                getWorld(),
                VIRTUAL_HEIGHT,
                5.0f,
                npcSpawner);

        treeSpawner = new TreeSpawner(
                getEntityManager(),
                VIRTUAL_HEIGHT,
                3.0f);

        ruleManager = new RuleManager();
        commandHistory = new CommandHistory();

        getCollisionHandler().setTrafficViolationListener(
                new GameCollisionHandler.TrafficViolationListener() {
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
                        commandHistory.executeAndRecord(new PedestrianHitCommand(ruleManager));
                        setInstantFail(true, "Hit a pedestrian and caused an accident");
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
            getSound().addSound("boundary_hit", "hit_sound.wav");
        } catch (Exception e) {
            Gdx.app.log("Level1Scene", "Could not load boundary_hit sound: " + e.getMessage());
        }

        try {
            getSound().addSound("crash", "crash_sound.wav");
        } catch (Exception e) {
            Gdx.app.log("Level1Scene", "Could not load crash sound: " + e.getMessage());
        }

        Gdx.app.log("Level1Scene", "=== INIT LEVEL DATA COMPLETE ===");
    }

    private CrosswalkZone createCrosswalkZone(float worldY) {
        float zoneHalfW = (RoadRenderer.ROAD_WIDTH / Constants.PPM) / 2f;
        float zoneHalfH = (CROSSWALK_HEIGHT / Constants.PPM) / 2f;

        PhysicsBody zoneBody = getWorld().createBody(
                BodyDef.BodyType.KinematicBody,
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

        PhysicsBody pedBody = getWorld().createBody(
                BodyDef.BodyType.KinematicBody,
                (pedStartX + pedW / 2f) / Constants.PPM,
                (worldY + pedH / 2f) / Constants.PPM,
                pedHalfW,
                pedHalfH,
                0f,
                0f,
                true,
                null);

        Pedestrian pedestrian = new Pedestrian(pedStartX, worldY, pedW, pedH, pedBody);
        PedestrianIntent intent = new PedestrianIntent(direction);
        PedestrianMovement movement = new PedestrianMovement(CROSSING_SPEED, pedW);

        return new PedestrianEncounter(
                pedestrian,
                intent,
                movement,
                zone,
                worldY);
    }

    @Override
    protected void updateGame(float deltaTime) {
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
            updatePedestrianEncounter(encounter, scroll, deltaTime);

            if (encounter.pedestrian.isExpired()) {
                encounter.zone.setCrossingActive(false);
                encounterIter.remove();
            }
        }

        Iterator<StopSign> signIter = stopSigns.iterator();
        while (signIter.hasNext()) {
            StopSign sign = signIter.next();
            if (sign.isExpired()) {
                signIter.remove();
                continue;
            }
            sign.updatePosition(scroll);
        }

        setRulesBroken(ruleManager.getRulesBroken());
        if (ruleManager.isInstantFail()) {
            setInstantFail(true, "Hit a pedestrian and caused an accident");
        }
    }

    private void updatePedestrianEncounter(PedestrianEncounter encounter, float scroll, float deltaTime) {
        Pedestrian ped = encounter.pedestrian;
        PedestrianIntent intent = encounter.intent;
        PedestrianMovement movement = encounter.movement;
        CrosswalkZone zone = encounter.zone;

        if (ped.isExpired()) {
            return;
        }

        float screenY = encounter.relativeY + scroll;
        ped.setY(screenY);

        boolean visibleForActivation = screenY > -100f && screenY < 800f;
        if (visibleForActivation && !movement.isActive() && !movement.isFinished()) {
            movement.activate();
            zone.setCrossingActive(true);
        }

        movement.update(ped, intent, deltaTime);

        if (!movement.isFinished() && movement.hasReachedFinish(ped)) {
            movement.markFinishedSuccessfully();
            zone.setCrossingActive(false);

            if (!encounter.rewarded) {
                addScore(200);
                getSound().playSound("reward", 1.0f);
                encounter.rewarded = true;
            }

            ped.markExpired();
            return;
        }

        if (screenY < -ped.getH() * 2f) {
            movement.markFinishedUnsuccessfully();
            zone.setCrossingActive(false);
            ped.markExpired();
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

        crosswalkZones.clear();
        stopSigns.clear();
        pedestrianEncounters.clear();

        if (commandHistory != null) {
            commandHistory.clear();
        }

        Gdx.app.log("Level1Scene", "Level 1 data disposed");
    }
}