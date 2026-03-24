package io.github.raesleg.game.scene;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.event.EventBus;
import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.physics.PhysicsWorld;
import io.github.raesleg.engine.system.IGameSystem;
import io.github.raesleg.game.GameConstants;
import io.github.raesleg.game.collision.PedestrianHitReaction;
import io.github.raesleg.game.entities.misc.Pedestrian;
import io.github.raesleg.game.entities.misc.StopSign;
import io.github.raesleg.game.event.InstantFailEvent;
import io.github.raesleg.game.event.ScoreChangedEvent;
import io.github.raesleg.engine.io.CommandHistory;
import io.github.raesleg.game.factory.CrosswalkFactory;
import io.github.raesleg.game.movement.PedestrianMovement;
import io.github.raesleg.game.rules.BreakRuleCommand;
import io.github.raesleg.game.rules.RuleManager;
import io.github.raesleg.game.zone.CrosswalkZone;

/**
 * CrosswalkEncounterSystem — Manages all crosswalk zones, pedestrian
 * encounters, stop signs, and crossing violation detection for Level 1.
 * <p>
 * Extracted from Level1Scene to satisfy SRP: crosswalk encounter lifecycle
 * is one cohesive responsibility, independent of spawning, speed control,
 * or audio management.
 * <p>
 * <b>Design patterns:</b>
 * <ul>
 * <li>Observer — publishes score/fail events to the EventBus</li>
 * <li>Command — records violations via BreakRuleCommand</li>
 * </ul>
 */
public final class CrosswalkEncounterSystem implements IGameSystem {

    private static final float VIRTUAL_HEIGHT = 720f;

    private final EntityManager entityManager;
    private final PhysicsWorld world;
    private final SoundDevice sound;
    private final EventBus eventBus;
    private final RuleManager ruleManager;
    private final CommandHistory commandHistory;

    private final List<CrosswalkZone> crosswalkZones = new ArrayList<>();
    private final List<StopSign> stopSigns = new ArrayList<>();
    private final List<PedestrianEncounter> encounters = new ArrayList<>();

    /** Supplier for current scroll offset and speed — set by the scene. */
    private float scrollOffset;
    private float simulatedSpeedKmh;

    /** Callback for instant fail (set by the owning scene). */
    private boolean instantFailTriggered;
    private String instantFailReason;

    /** Tracks whether any crosswalk encounter is currently active on screen. */
    private boolean crosswalkActiveOnScreen;

    /**
     * Internal data class for a single pedestrian crossing encounter.
     */
    static final class PedestrianEncounter {
        final Pedestrian pedestrian;
        final PedestrianMovement movement;
        final PedestrianHitReaction hitReaction;
        final CrosswalkZone zone;
        boolean rewarded;
        boolean crashHandled;
        boolean failQueued;
        boolean stopViolationFired;

        PedestrianEncounter(
                Pedestrian pedestrian, 
                PedestrianMovement movement, PedestrianHitReaction hitReaction,
                CrosswalkZone zone) {
            this.pedestrian = pedestrian;
            this.movement = movement;
            this.hitReaction = hitReaction;
            this.zone = zone;
        }
    }

    public CrosswalkEncounterSystem(EntityManager entityManager, PhysicsWorld world,
            SoundDevice sound, EventBus eventBus,
            RuleManager ruleManager, CommandHistory commandHistory) {
        this.entityManager = entityManager;
        this.world = world;
        this.sound = sound;
        this.eventBus = eventBus;
        this.ruleManager = ruleManager;
        this.commandHistory = commandHistory;
    }

    /**
     * Creates all crosswalk zones, pedestrians, and stop signs for the given
     * positions.
     * Call once during level init.
     */
    public void initCrosswalks(List<Float> crossingPositions) {
        float roadCentreX = RoadRenderer.ROAD_LEFT + RoadRenderer.ROAD_WIDTH / 2f;

        for (int i = 0; i < crossingPositions.size(); i++) {
            float worldY = crossingPositions.get(i);

            CrosswalkZone zone = CrosswalkFactory.createZone(world, worldY, roadCentreX);
            crosswalkZones.add(zone);
            entityManager.addEntity(zone);

            CrosswalkFactory.EncounterComponents ec = CrosswalkFactory.createEncounter(world, i, worldY);
            PedestrianEncounter encounter = new PedestrianEncounter(
                    ec.getPedestrian(), ec.getMovement(),
                    ec.getHitReaction(), zone);
            encounters.add(encounter);
            entityManager.addEntity(encounter.pedestrian);

            StopSign sign = new StopSign(RoadRenderer.ROAD_LEFT - 130f, worldY - 30f);
            stopSigns.add(sign);
            entityManager.addEntity(sign);
        }
    }

    /** Called by the scene each frame before update(). */
    public void setFrameState(float scrollOffset, float simulatedSpeedKmh) {
        this.scrollOffset = scrollOffset;
        this.simulatedSpeedKmh = simulatedSpeedKmh;
    }

    /** Returns true if any pedestrian crossing is actively visible on screen. */
    public boolean isCrosswalkActiveOnScreen() {
        return crosswalkActiveOnScreen;
    }

    public boolean isInstantFailTriggered() {
        return instantFailTriggered;
    }

    public String getInstantFailReason() {
        return instantFailReason;
    }

    /** Trigger a pedestrian hit reaction from an external collision handler. */
    public void triggerPedestrianHit(Pedestrian hitPedestrian, Vector2 knockbackDirection, float knockbackForce) {
        if (hitPedestrian == null)
            return;

        for (PedestrianEncounter encounter : encounters) {
            if (encounter.pedestrian == hitPedestrian) {
                // Only trigger the hit reaction if it's not already playing
                if (!encounter.hitReaction.isActive() && !encounter.hitReaction.isFinished()) {
                    encounter.movement.markFinishedUnsuccessfully();
                    encounter.zone.setCrossingActive(false);
                    encounter.hitReaction.trigger(hitPedestrian, knockbackDirection, knockbackForce);
                }
                
                // Set fail flag so the update loop publishes the event after animation finishes
                // This ensures that even repeated hits during the animation will trigger a fail
                if (!encounter.failQueued) {
                    encounter.failQueued = true;
                    instantFailReason = "Hit a pedestrian and caused an accident";
                }
                break;
            }
        }
    }

    public List<CrosswalkZone> getCrosswalkZones() {
        return crosswalkZones;
    }

    @Override
    public void update(float deltaTime) {
        instantFailTriggered = false;
        crosswalkActiveOnScreen = false;

        // Update stop signs
        Iterator<StopSign> signIter = stopSigns.iterator();
        while (signIter.hasNext()) {
            StopSign sign = signIter.next();
            sign.updatePosition(scrollOffset);
            if (sign.isExpired())
                signIter.remove();
        }

        // Update crosswalk zones
        Iterator<CrosswalkZone> zoneIter = crosswalkZones.iterator();
        while (zoneIter.hasNext()) {
            CrosswalkZone zone = zoneIter.next();
            if (zone.isExpired()) {
                zoneIter.remove();
                continue;
            }
            zone.updatePosition(scrollOffset);
        }

        // Update pedestrian encounters
        Iterator<PedestrianEncounter> encounterIter = encounters.iterator();
        while (encounterIter.hasNext()) {
            PedestrianEncounter enc = encounterIter.next();
            Pedestrian ped = enc.pedestrian;
            CrosswalkZone zone = enc.zone;

            if (ped.isExpired() || zone.isExpired()) {
                if (enc.failQueued && !enc.crashHandled) {
                    enc.crashHandled = true;
                    zone.markExpired();
                    ped.markExpired();
                    encounterIter.remove();
                    instantFailTriggered = true;
                    instantFailReason = "Hit a pedestrian and caused an accident";
                    eventBus.publish(new InstantFailEvent(instantFailReason));
                    return;
                }
                encounterIter.remove();
                continue;
            }

            float screenY = ped.getRelativeY() + scrollOffset;
            boolean visible = screenY > -200f && screenY < VIRTUAL_HEIGHT + 400f;

            // Track active-on-screen state for NPC spawn suppression
            if (enc.movement.isActive() && !enc.movement.isFinished() && !enc.failQueued && visible) {
                crosswalkActiveOnScreen = true;
            }

            // Activation check
            if (visible && !enc.movement.isActive() && !enc.movement.isFinished()
                    && !enc.hitReaction.isActive() && !enc.failQueued) {
                enc.movement.activate();
                zone.setCrossingActive(true);

                if (zone.isPlayerInside() && zone.tryFireViolation()) {
                    fireCrosswalkViolation();
                }
                if (!zone.isPlayerInside() && zone.hasPlayerPassed() && zone.tryFireViolation()) {
                    fireCrosswalkViolation();
                }
            }

            // Hit reaction update
            if (enc.hitReaction.isActive()) {
                enc.hitReaction.update(ped, deltaTime);
                zone.setCrossingActive(false);
            }

            // Fail check
            if (enc.failQueued && !enc.crashHandled
                    && (enc.hitReaction.isFinished() || !enc.hitReaction.isActive())) {
                enc.crashHandled = true;
                enc.failQueued = false;
                zone.markExpired();
                ped.markExpired();
                encounterIter.remove();
                Gdx.app.log("CrosswalkSystem", "Setting instant fail after pedestrian hit");
                instantFailTriggered = true;
                instantFailReason = "Hit a pedestrian and caused an accident";
                eventBus.publish(new InstantFailEvent(instantFailReason));
                return;
            } else if (!enc.hitReaction.isActive() && !enc.failQueued) {
                ped.updateScreenPosition(scrollOffset);
                ped.resetRenderRotation();
                ped.syncBodyToSprite();

                boolean playerBlocking = zone.isPlayerInside() && enc.movement.isActive();

                if (playerBlocking) {
                    if (!enc.stopViolationFired && simulatedSpeedKmh < 3f) {
                        enc.stopViolationFired = true;
                        commandHistory.executeAndRecord(
                                new BreakRuleCommand(ruleManager, "CROSSWALK_STOP",
                                        GameConstants.CROSSWALK_VIOLATION_STARS));
                        sound.playSound("negative", 1.0f);
                    }
                } else {
                    enc.movement.update(ped, deltaTime);
                }
            }

            // Crossing completion check
            if (!enc.failQueued && !enc.movement.isFinished()
                    && !enc.hitReaction.isActive()
                    && enc.movement.hasReachedFinish(ped)) {
                enc.movement.markFinishedSuccessfully();
                zone.setCrossingActive(false);

                if (!enc.rewarded) {
                    enc.rewarded = true;
                    eventBus.publish(new ScoreChangedEvent(200));
                    sound.playSound("reward", 1.0f);
                }

                ped.markExpired();
                zone.markExpired();
            }
        }
    }

    @Override
    public void dispose() {
        crosswalkZones.clear();
        stopSigns.clear();
        encounters.clear();
    }

    private void fireCrosswalkViolation() {
        commandHistory.executeAndRecord(
                new BreakRuleCommand(ruleManager, "CROSSWALK", GameConstants.CROSSWALK_VIOLATION_STARS));
        sound.playSound("negative", 1.0f);
        eventBus.publish(new ScoreChangedEvent(GameConstants.SCORE_PENALTY));
    }
}
