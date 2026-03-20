package io.github.raesleg.game.collision;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.collision.ICollisionListener;
import io.github.raesleg.engine.entity.Entity;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.entity.IFlashable;
import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.movement.MovementModel;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.entities.Pedestrian;
import io.github.raesleg.game.entities.Pickupable;
import io.github.raesleg.game.entities.vehicles.NPCCar;
import io.github.raesleg.game.entities.vehicles.npc.world.effects.ExplosionParticle;
import io.github.raesleg.game.scene.RoadRenderer;
import io.github.raesleg.game.zone.CrosswalkZone;
import io.github.raesleg.game.zone.MotionZone;

/**
 * GameCollisionHandler — Game-specific collision logic.
 * <p>
 * Handles four collision types:
 * <ol>
 * <li><b>MovableEntity vs MotionZone</b> — Apply/remove friction zones
 * (EXISTING)</li>
 * <li><b>Player vs Road Boundary</b> — Flash effect + bounce back (NEW)</li>
 * <li><b>Player vs NPC Car</b> — Crash effect + knockback (NEW)</li>
 * <li><b>User vs AI Entity</b> — Explosion particles (EXISTING)</li>
 * <li><b>Player vs CrosswalkZone</b> — Tracks whether player is inside a
 * crossing (NEW)</li>
 * <li><b>Player vs Pedestrian</b> — Rule-break penalty + flash effect
 * (NEW)</li>
 * </ol>
 */
public class GameCollisionHandler implements ICollisionListener {

    private final EntityManager entityManager;
    private final float explosionForceThreshold;
    private final SoundDevice soundManager;

    /* Collision thresholds for road boundaries and NPC crashes */
    private static final float BOUNDARY_BOUNCE_FORCE = 15f;
    private static final float CRASH_KNOCKBACK_MULTIPLIER = 8f;

    /** Minimum time (ms) between traffic crash penalty events (debounce). */
    private static final long CRASH_COOLDOWN_MS = 1500;
    private long lastCrashTimeMs = 0;

    /**
     * Callback interface for traffic violation events (Observer pattern).
     * The owning scene implements this so the collision handler can
     * notify it without depending on any specific scene type (DIP).
     */
    public interface TrafficViolationListener {
        /** Called when the player enters a crosswalk while a pedestrian is crossing. */
        void onCrosswalkViolation();

        /** Called when the player collides with an NPC traffic vehicle. */
        void onTrafficCrash();

        /** Called when the player directly hits a pedestrian. */
        void onPedestrianHit(Pedestrian pedestrian, Vector2 knockbackDirection, float knockbackForce);

        /** Called when the player picks up a collectible. */
        default void onPickup() {
        }
    }

    private TrafficViolationListener violationListener;

    /** Helper record to normalise the order of entity pairs (DRY). */
    private record ZoneCollision(MovableEntity movable, MotionZone zone) {
    }

    /**
     * Extracts a MovableEntity/MotionZone pair regardless of A/B ordering.
     * Returns null if the pair is not a zone collision.
     */
    private ZoneCollision extractZoneCollision(Entity a, Entity b) {
        if (a instanceof MovableEntity m && b instanceof MotionZone z)
            return new ZoneCollision(m, z);
        if (b instanceof MovableEntity m && a instanceof MotionZone z)
            return new ZoneCollision(m, z);
        return null;
    }

    public GameCollisionHandler(EntityManager entityManager, SoundDevice soundManager) {
        this(entityManager, soundManager, 0.1f); // default threshold
    }

    public GameCollisionHandler(EntityManager entityManager, SoundDevice soundManager, float explosionForceThreshold) {
        this.entityManager = entityManager;
        this.soundManager = soundManager;
        this.explosionForceThreshold = explosionForceThreshold;
    }

    /** Sets the listener notified on any traffic violation. */
    public void setTrafficViolationListener(TrafficViolationListener listener) {
        this.violationListener = listener;
    }

    @Override
    public void onCollisionBegin(Entity entityA, Entity entityB) {

        // EXISTING: Handle motion zone entry
        ZoneCollision zc = extractZoneCollision(entityA, entityB);
        
        if (zc != null) {
            MovementModel model = zc.movable().getMovementModel();
            model.onEnterZone(zc.movable().getPhysicsBody(), zc.zone().getTuning());
        }

        // Check for road boundary collision
        if (isPlayerVsBoundary(entityA, entityB)) {
            MovableEntity player = getPlayerEntity(entityA, entityB);
            handleRoadBoundaryCollision(player);
            return;
        }

        // Handle player entering a CrosswalkZone
        CrosswalkZone cwZone = extractCrosswalkZone(entityA, entityB);
        MovableEntity cwPlayer = getPlayerEntity(entityA, entityB);
        if (cwZone != null && cwPlayer != null) {
            cwZone.setPlayerInside(true);
            // Only fire violation once per zone entry, and only while pedestrian is
            // crossing
            if (cwZone.isPedestrianCrossing() && cwZone.tryFireViolation()
                    && violationListener != null) {
                violationListener.onCrosswalkViolation();
            }
        }

        // Handle player hitting a Pedestrian directly
        Pedestrian ped = extractPedestrian(entityA, entityB);
        MovableEntity pedPlayer = getPlayerEntity(entityA, entityB);
        if (ped != null && pedPlayer != null && !ped.isExpired()) {
            if (pedPlayer instanceof IFlashable flashable) {
                flashable.triggerDamageFlash();
            }

            PhysicsBody playerBody = pedPlayer.getPhysicsBody();
            Vector2 playerVelocity = playerBody.getVelocity();

            Vector2 knockbackDir;
            if (playerVelocity.len2() > 0.01f) {
                knockbackDir = playerVelocity.cpy().nor();
            } else {
                PhysicsBody pedBody = ped.getPhysicsBody();
                if (pedBody != null) {
                    Vector2 pedPos = pedBody.getPosition();
                    Vector2 playerPos = playerBody.getPosition();
                    knockbackDir = pedPos.cpy().sub(playerPos).nor();
                } else {
                    knockbackDir = new Vector2(0f, 1f);
                }
            }

            float knockbackForce = Math.max(20f, playerVelocity.len() * 15f);

            if (soundManager != null) {
                soundManager.playSound("explosion", 1.0f);
            }

            if (violationListener != null) {
                violationListener.onPedestrianHit(ped, knockbackDir, knockbackForce);
            }
        }

        // Handle player picking up a Pickupable
        Pickupable pickup = extractPickupable(entityA, entityB);
        MovableEntity pickupPlayer = getPlayerEntity(entityA, entityB);
        if (pickup != null && pickupPlayer != null && !pickup.isExpired()) {
            pickup.markExpired();
            if (violationListener != null) {
                violationListener.onPickup();
            }
        }
    }

    private void handleRoadBoundaryCollision(MovableEntity player) {
        if (player == null)
            return;
    
        // Trigger flash effect if player supports it
        if (player instanceof IFlashable flashable) {
            flashable.triggerDamageFlash();
        }
    
        // Calculate bounce direction based on velocity
        PhysicsBody body = player.getPhysicsBody();
        Vector2 velocity = body.getVelocity();
        
        // Reverse the velocity direction for bounce
        Vector2 bounceDirection;
        if (velocity.len2() > 0.01f) {
            // Bounce opposite to movement direction
            bounceDirection = velocity.cpy().nor().scl(-1f);
        } else {
            // If stationary, push toward center of road
            Vector2 playerPos = body.getPosition();
            float roadCenterX = (RoadRenderer.ROAD_LEFT + RoadRenderer.ROAD_RIGHT) / 2f / Constants.PPM;
            bounceDirection = new Vector2(roadCenterX - playerPos.x, 0f).nor();
        }
    
        // Apply bounce impulse
        Vector2 bounceImpulse = bounceDirection.scl(BOUNDARY_BOUNCE_FORCE);
        body.applyImpulseAtCenter(bounceImpulse);
    
        // Play sound effect
        if (soundManager != null) {
            soundManager.playSound("boundary_hit", 0.8f);
        }
    
        Gdx.app.log("GameCollisionHandler", "Road boundary bounce applied");
    }

    @Override
    public void onCollisionEnd(Entity entityA, Entity entityB) {
        // EXISTING: Handle motion zone exit
        ZoneCollision zc = extractZoneCollision(entityA, entityB);
        if (zc != null) {
            MovementModel model = zc.movable().getMovementModel();
            model.onExitZone(zc.movable().getPhysicsBody());
        }

        // NEW: Handle player leaving a CrosswalkZone
        CrosswalkZone cwZone = extractCrosswalkZone(entityA, entityB);
        MovableEntity cwPlayer = getPlayerEntity(entityA, entityB);
        if (cwZone != null && cwPlayer != null) {
            cwZone.setPlayerInside(false);
        }
    }

    @Override
    public void onImpact(Entity entityA, Entity entityB, float impactForce, Vector2 impactPoint) {

        // NEW: Check for player vs NPC car collision
        if (isPlayerVsNPCCar(entityA, entityB)) {
            MovableEntity player = getPlayerEntity(entityA, entityB);
            NPCCar npc = getNPCCar(entityA, entityB);
            handleCarCrashCollision(player, npc, impactForce, impactPoint);
            return; // IMPORTANT: Exit here - don't trigger explosion particles
        }

        // EXISTING: Explosion logic (AI vs User) - ONLY runs if above conditions are
        // false
        // Only trigger explosion if impact is strong enough
        if (impactForce < explosionForceThreshold) {
            return;
        }

        // Determine if entity is AI-controlled / user-controlled
        MovableEntity aiEntity = null;
        MovableEntity userEntity = null;

        if (entityA instanceof MovableEntity movableA) {
            if (movableA.isAIControlled()) {
                aiEntity = movableA;
            } else {
                userEntity = movableA;
            }
        }

        if (entityB instanceof MovableEntity movableB) {
            if (movableB.isAIControlled()) {
                aiEntity = movableB;
            } else {
                userEntity = movableB;
            }
        }

        // Game Rule: User hitting AI entity causes explosion
        // This will NOT trigger for NPC cars because we returned early above
        if (aiEntity != null && userEntity != null) {
            if (soundManager != null) {
                soundManager.playSound("explosion", 1.0f);
            }

            createExplosion(impactPoint, impactForce);
            ExplosionParticle.spawnExplosion(entityManager, impactPoint, impactForce);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // NEW: Collision Type Detection
    // ═══════════════════════════════════════════════════════════

    /**
     * Checks if one entity is a user-controlled MovableEntity and the other is
     * a road boundary wall (null userData).
     */
    private boolean isPlayerVsBoundary(Entity a, Entity b) {
        MovableEntity player = getPlayerEntity(a, b);
        boolean hasNull = (a == null || b == null);
        return player != null && hasNull;
    }

    /**
     * Checks if one entity is a user-controlled MovableEntity and the other
     * is an NPC car.
     */
    private boolean isPlayerVsNPCCar(Entity a, Entity b) {
        MovableEntity player = getPlayerEntity(a, b);
        NPCCar npc = getNPCCar(a, b);
        return player != null && npc != null;
    }

    /**
     * Returns the user-controlled MovableEntity from the collision pair,
     * or null if neither is user-controlled.
     */
    private MovableEntity getPlayerEntity(Entity a, Entity b) {
        if (a instanceof MovableEntity ma && !ma.isAIControlled()) {
            return ma;
        }
        if (b instanceof MovableEntity mb && !mb.isAIControlled()) {
            return mb;
        }
        return null;
    }

    /**
     * Returns the NPC car from the collision pair, or null if neither is an NPCCar.
     */
    private NPCCar getNPCCar(Entity a, Entity b) {
        if (a instanceof NPCCar)
            return (NPCCar) a;
        if (b instanceof NPCCar)
            return (NPCCar) b;
        return null;
    }

    /**
     * Returns the CrosswalkZone from the collision pair, or null if neither is one.
     */
    private CrosswalkZone extractCrosswalkZone(Entity a, Entity b) {
        if (a instanceof CrosswalkZone cz)
            return cz;
        if (b instanceof CrosswalkZone cz)
            return cz;
        return null;
    }

    /**
     * Returns the Pedestrian from the collision pair, or null if neither is one.
     */
    private Pedestrian extractPedestrian(Entity a, Entity b) {
        if (a instanceof Pedestrian p)
            return p;
        if (b instanceof Pedestrian p)
            return p;
        return null;
    }

    /**
     * Returns the Pickupable from the collision pair, or null if neither is one.
     */
    private Pickupable extractPickupable(Entity a, Entity b) {
        if (a instanceof Pickupable p)
            return p;
        if (b instanceof Pickupable p)
            return p;
        return null;
    }

    // ═══════════════════════════════════════════════════════════
    // NEW: Collision Handlers
    // ═══════════════════════════════════════════════════════════

    // clamp player car velocity to prevent it from flying too far during collisions
    private void clampVelocity(PhysicsBody body, float maxSpeed) {
        Vector2 vel = body.getVelocity();
        float currentSpeed = vel.len();

        if (currentSpeed > maxSpeed) {
            vel.nor().scl(maxSpeed);
            body.setVelocity(vel.x, vel.y);
            Gdx.app.log("GameCollisionHandler", "Velocity clamped: " + currentSpeed + " -> " + maxSpeed);
        }
    }

    /**
     * Handles player collision with NPC car.
     * 
     * Effects:
     * - Applies knockback in opposite direction of approach
     * - Plays crash sound (if available)
     * 
     * Visual effects (flash, particles) can be added later.
     */
    private void handleCarCrashCollision(MovableEntity player, NPCCar npc, float force, Vector2 impactPoint) {
        if (player == null || npc == null)
            return;

        // Debounce: ignore rapid-fire crash events from the same collision
        long now = TimeUtils.millis();
        boolean penaltyAllowed = (now - lastCrashTimeMs >= CRASH_COOLDOWN_MS);

        Gdx.app.log("GameCollisionHandler", "Car crash detected (penalty: " + penaltyAllowed + ")");

        // Calculate knockback direction (opposite of player's velocity)
        PhysicsBody playerBody = player.getPhysicsBody();
        Vector2 playerVelocity = playerBody.getVelocity();

        // Determine knockback direction
        Vector2 knockbackDirection;
        if (playerVelocity.len2() < 0.01f) {
            // Player moving very slowly - use position-based direction (away from NPC)
            Vector2 npcPosMeters = new Vector2(
                    (npc.getX() + npc.getW() / 2f) / Constants.PPM,
                    (npc.getY() + npc.getH() / 2f) / Constants.PPM);
            knockbackDirection = playerBody.getPosition().cpy().sub(npcPosMeters).nor();
        } else {
            // Reverse player's movement direction
            knockbackDirection = playerVelocity.cpy().scl(-1).nor();
        }

        // Apply knockback force (moderate strength to keep car on screen)
        float knockbackMagnitude = Math.max(force * CRASH_KNOCKBACK_MULTIPLIER, 15f);
        Vector2 knockbackImpulse = knockbackDirection.scl(knockbackMagnitude);
        playerBody.applyImpulseAtCenter(knockbackImpulse);

        // Clamp velocity to prevent player from flying too far
        clampVelocity(playerBody, 12f); // Max 12m/s

        // Play crash sound (collide_sound.wav registered as "explosion" in
        // BaseGameScene)
        if (soundManager != null) {
            try {
                soundManager.playSound("explosion", 1.0f);
            } catch (Exception e) {
                // Sound file might not exist yet
            }
        }

        // Notify observer only if cooldown has elapsed (prevents multiple
        // penalties from a single physical collision)
        if (penaltyAllowed && violationListener != null) {
            lastCrashTimeMs = now;
            violationListener.onTrafficCrash();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // EXISTING: Explosion Logic (unchanged)
    // ═══════════════════════════════════════════════════════════

    /**
     * Apply radial explosion force to all nearby MovableEntities.
     * (EXISTING - unchanged from original implementation)
     */
    private void createExplosion(Vector2 center, float force) {
        float radius = 5f;
        float radius2 = radius * radius;

        for (Entity e : entityManager.getSnapshot()) {
            if (e instanceof MovableEntity m) {
                PhysicsBody pb = m.getPhysicsBody();

                Vector2 bodyPos = pb.getPosition();

                float dx = bodyPos.x - center.x;
                float dy = bodyPos.y - center.y;
                float dist2 = dx * dx + dy * dy;

                // only affect entities within explosion radius
                if (dist2 < radius2 && dist2 > 0.000001f) {
                    float dist = (float) Math.sqrt(dist2);

                    // direction normalized
                    float nx = dx / dist;
                    float ny = dy / dist;

                    // linear falloff: entities further away get less force
                    float falloff = 1f - (dist / radius);
                    float explosionForce = force * falloff * 5000f; // scale factor

                    Vector2 impulse = new Vector2(nx, ny).scl(explosionForce);

                    pb.applyImpulseAtCenter(impulse);
                }
            }
        }
    }
}