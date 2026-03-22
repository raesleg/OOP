package io.github.raesleg.game.collision;

import com.badlogic.gdx.math.Vector2;

import io.github.raesleg.engine.collision.ICollisionListener;
import io.github.raesleg.engine.entity.Entity;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.collision.handlers.BoundaryCollisionHandler;
import io.github.raesleg.game.collision.handlers.CrosswalkCollisionHandler;
import io.github.raesleg.game.collision.handlers.NPCCarCollisionHandler;
import io.github.raesleg.game.collision.handlers.NPCPedestrianCollisionHandler;
import io.github.raesleg.game.collision.handlers.PedestrianCollisionHandler;
import io.github.raesleg.game.collision.handlers.PickupCollisionHandler;
import io.github.raesleg.game.collision.handlers.ZoneCollisionHandler;
import io.github.raesleg.game.collision.handlers.ExplosionCollisionHandler;
import io.github.raesleg.game.collision.listeners.PickupListener;
import io.github.raesleg.game.collision.listeners.TrafficViolationListener;

/**
 * GameCollisionHandler — Coordinator for game-specific collision logic.
 * 
 * This class delegates collision handling to specialised handler classes
 * Each handler is responsible for one type of collision (Single Responsibility
 * Principle).
 * 
 * Design Patterns:
 * Facade: Provides a simple interface to the complex collision subsystem
 * Strategy: Different handlers implement different collision strategies
 * Observer: TrafficViolationListener notifies the scene of events
 * Dependency Injection: Handlers injected via constructor
 * 
 *
 * SOLID Compliance:
 * Single Responsibility: Only coordinates between handlers
 * Open/Closed: Add new collision types by adding handlers, instead of modifying
 * class
 * Liskov Substitution: All handlers can be swapped with implementations
 * Interface Segregation: Each handler has a focused interface
 * Dependency Inversion: Depends on abstractions (handler interfaces)
 * 
 */
public class GameCollisionHandler implements ICollisionListener {

    /* Specialized collision handlers (Strategy Pattern) */
    private final ZoneCollisionHandler zoneHandler;
    private final BoundaryCollisionHandler boundaryHandler;
    private final CrosswalkCollisionHandler crosswalkHandler;
    private final PedestrianCollisionHandler pedestrianHandler;
    private final PickupCollisionHandler pickupHandler;
    private final NPCCarCollisionHandler npcCarHandler;
    private final NPCPedestrianCollisionHandler npcPedestrianHandler;
    private final ExplosionCollisionHandler explosionHandler;

    /* Observer for traffic violations */

    /**
     * Creates the collision handler with all specialized handlers.
     * 
     * @param entityManager EntityManager for accessing all entities
     * @param soundManager  SoundDevice for playing collision sounds
     */
    public GameCollisionHandler(EntityManager entityManager, SoundDevice soundManager) {
        this(entityManager, soundManager, 0.1f);
    }

    /**
     * Creates the collision handler with custom explosion threshold.
     * 
     * @param entityManager           EntityManager for accessing all entities
     * @param soundManager            SoundDevice for playing collision sounds
     * @param explosionForceThreshold Minimum force to trigger explosions
     */
    public GameCollisionHandler(EntityManager entityManager, SoundDevice soundManager,
            float explosionForceThreshold) {
        // Instantiate all specialized handlers (Dependency Injection)
        this.zoneHandler = new ZoneCollisionHandler();
        this.boundaryHandler = new BoundaryCollisionHandler(soundManager);
        this.crosswalkHandler = new CrosswalkCollisionHandler();
        this.pedestrianHandler = new PedestrianCollisionHandler(soundManager);
        this.pickupHandler = new PickupCollisionHandler();
        this.npcCarHandler = new NPCCarCollisionHandler(soundManager);
        this.npcPedestrianHandler = new NPCPedestrianCollisionHandler(soundManager);
        this.explosionHandler = new ExplosionCollisionHandler(entityManager, soundManager, explosionForceThreshold);
    }

    /**
     * Sets the listener notified on traffic violations (Observer Pattern).
     */
    public void setTrafficViolationListener(TrafficViolationListener listener) {
        // Propagate to handlers that need it
        crosswalkHandler.setViolationListener(listener);
        pedestrianHandler.setViolationListener(listener);
        npcCarHandler.setViolationListener(listener);
    }

    /**
     * Sets the listener notified on pickup collection (ISP — separate from
     * violations).
     */
    public void setPickupListener(PickupListener listener) {
        pickupHandler.setPickupListener(listener);
    }

    @Override
    public void onCollisionBegin(Entity entityA, Entity entityB) {
        // Delegate to appropriate handlers in priority order

        // 1. Motion zones (friction, ice, etc.)
        if (zoneHandler.canHandle(entityA, entityB)) {
            zoneHandler.handleBegin(entityA, entityB);
        }

        // 2. Road boundaries (walls)
        if (boundaryHandler.canHandle(entityA, entityB)) {
            boundaryHandler.handleBegin(entityA, entityB);
            return; // Boundary collisions don't trigger other handlers
        }

        // 3. Crosswalk zones
        if (crosswalkHandler.canHandle(entityA, entityB)) {
            crosswalkHandler.handleBegin(entityA, entityB);
        }

        // 4. Pedestrians
        if (pedestrianHandler.canHandle(entityA, entityB)) {
            pedestrianHandler.handleBegin(entityA, entityB);
            return;
        }

        // 4b. NPC car hits pedestrian
        if (npcPedestrianHandler.canHandle(entityA, entityB)) {
            npcPedestrianHandler.handleBegin(entityA, entityB);
        }

        // 5. Pickupables
        if (pickupHandler.canHandle(entityA, entityB)) {
            pickupHandler.handleBegin(entityA, entityB);
        }
    }

    @Override
    public void onCollisionEnd(Entity entityA, Entity entityB) {
        // Delegate to appropriate handlers

        // Motion zones
        if (zoneHandler.canHandle(entityA, entityB)) {
            zoneHandler.handleEnd(entityA, entityB);
        }

        // Crosswalk zones
        if (crosswalkHandler.canHandle(entityA, entityB)) {
            crosswalkHandler.handleEnd(entityA, entityB);
        }
    }

    @Override
    public void onImpact(Entity entityA, Entity entityB, float impactForce, Vector2 impactPoint) {
        // Delegate to appropriate handlers in priority order

        // 1. NPC car collisions
        if (npcCarHandler.canHandle(entityA, entityB)) {
            npcCarHandler.handleImpact(entityA, entityB, impactForce, impactPoint);
            return; // NPC collisions don't trigger explosions
        }

        // 2. Explosion effects (legacy AI entities)
        if (explosionHandler.canHandle(entityA, entityB)) {
            explosionHandler.handleImpact(entityA, entityB, impactForce, impactPoint);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Helper Methods for Entity Type Detection
    // ═══════════════════════════════════════════════════════════

    /**
     * Helper: Returns user-controlled MovableEntity from pair, or null.
     * Used by multiple handlers (DRY principle).
     */
    public static MovableEntity getPlayerEntity(Entity a, Entity b) {
        if (a instanceof MovableEntity ma && !ma.isAIControlled()) {
            return ma;
        }
        if (b instanceof MovableEntity mb && !mb.isAIControlled()) {
            return mb;
        }
        return null;
    }

    /**
     * Helper: Extracts specific entity type from collision pair.
     */
    public static <T> T extractEntity(Entity a, Entity b, Class<T> type) {
        if (type.isInstance(a))
            return type.cast(a);
        if (type.isInstance(b))
            return type.cast(b);
        return null;
    }

    /**
     * Helper: Clamps velocity to prevent excessive speed.
     * Shared utility for collision handlers (DRY principle).
     */
    public static void clampVelocity(PhysicsBody body, float maxSpeed) {
        Vector2 vel = body.getVelocity();
        float currentSpeed = vel.len();

        if (currentSpeed > maxSpeed) {
            vel.nor().scl(maxSpeed);
            body.setVelocity(vel.x, vel.y);
        }
    }
}