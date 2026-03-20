package io.github.raesleg.game.factory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.BodyDef;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.engine.physics.PhysicsWorld;
import io.github.raesleg.game.entities.vehicles.NPCCar;
import io.github.raesleg.game.scene.RoadRenderer;

/**
 * NPCCarSpawner — Manages spawning and lifecycle of NPC traffic cars.
 * <p>
 * This class encapsulates the logic for:
 * <ul>
 * <li>Spawning NPC cars at timed intervals</li>
 * <li>Positioning cars in appropriate lanes</li>
 * <li>Updating car positions based on scroll offset</li>
 * <li>Marking off-screen cars as expired (auto-removal via IExpirable)</li>
 * </ul>
 * <p>
 * <b>Design Pattern:</b> This is a <b>Manager/Service class</b> that handles
 * a specific responsibility (NPC lifecycle), demonstrating the Single
 * Responsibility Principle.
 * <p>
 * <b>Note:</b> NPCCar implements IExpirable, so EntityManager automatically
 * removes them when they go off-screen. This avoids needing a removeEntity()
 * method.
 * <p>
 * <b>Usage:</b> Each level creates an NPCCarSpawner in initLevelData() and
 * calls update() in updateGame().
 */
public class NPCCarSpawner {

    private final EntityManager entityManager;
    private final PhysicsWorld world;
    private final float screenHeight;

    /* Spawning configuration */
    private float spawnTimer;
    private float spawnInterval; // Seconds between spawns
    private float spawnYOffset; // How far ahead to spawn (pixels)

    /* Active NPCs - tracked for counting/debugging only */
    private final List<NPCCar> activeNPCs;

    /*
     * Exclusion zones — Y ranges where no NPC may spawn (e.g. crosswalk positions)
     */
    private final List<float[]> exclusionZones;

    /** Optional reference to puddle spawner for lane-overlap prevention. */
    private PuddleSpawner puddleSpawner;

    /* NPC car dimensions (pixels) */
    private static final float NPC_WIDTH = 70f;
    private static final float NPC_HEIGHT = 120f;

    /**
     * Creates an NPC car spawner.
     *
     * @param entityManager EntityManager to add/remove cars from
     * @param world         PhysicsWorld for creating collision bodies
     * @param screenHeight  Screen height in pixels
     * @param spawnInterval Time between spawns (seconds)
     */
    public NPCCarSpawner(EntityManager entityManager, PhysicsWorld world,
            float screenHeight, float spawnInterval) {
        this(entityManager, world, screenHeight, spawnInterval, null);
    }

    public NPCCarSpawner(EntityManager entityManager, PhysicsWorld world,
            float screenHeight, float spawnInterval,
            List<float[]> exclusionZones) {
        this.entityManager = entityManager;
        this.world = world;
        this.screenHeight = screenHeight;
        this.spawnInterval = spawnInterval;
        this.spawnYOffset = screenHeight + 200f; // Spawn just above screen
        this.spawnTimer = 0f;
        this.activeNPCs = new ArrayList<>();
        this.exclusionZones = (exclusionZones != null) ? exclusionZones : new ArrayList<>();
        this.puddleSpawner = null;
    }

    /** Sets the puddle spawner reference for lane-overlap prevention. */
    public void setPuddleSpawner(PuddleSpawner puddleSpawner) {
        this.puddleSpawner = puddleSpawner;
    }

    /**
     * Updates the spawner — spawns new cars and updates positions.
     * Call this every frame from your level's updateGame().
     *
     * Note: Off-screen NPCs are automatically removed by EntityManager
     * via the IExpirable interface, so no manual cleanup needed here.
     * 
     * @param deltaTime    Time since last frame (seconds)
     * @param scrollOffset Current road scroll offset (pixels)
     */
    public void update(float deltaTime, float scrollOffset) {
        // Update spawn timer
        spawnTimer += deltaTime;

        // Spawn new car if interval passed
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f;
            spawnRandomCar(scrollOffset);
        }

        // Update all active NPC positions based on scroll
        // Also clean up our tracking list (NPCs are auto-removed by EntityManager)
        activeNPCs.removeIf(npc -> {
            if (npc.isExpired()) {
                return true; // Remove from our tracking list
            }
            // Update position for NPCs still alive
            npc.updatePosition(scrollOffset, screenHeight);
            return false;
        });
    }

    /**
     * Spawns a new NPC car in a random lane, ensuring at most 2 lanes are
     * occupied near the spawn Y to always leave a gap for the player.
     */
    private void spawnRandomCar(float scrollOffset) {
        float relativeY = -scrollOffset + spawnYOffset;

        // Skip if spawn Y falls inside an exclusion zone (e.g. crosswalk)
        for (float[] zone : exclusionZones) {
            if (relativeY >= zone[0] - NPC_HEIGHT && relativeY <= zone[1] + NPC_HEIGHT) {
                return;
            }
        }

        // Determine which lanes are already occupied near the spawn Y
        Set<Integer> occupied = getOccupiedLanesNear(relativeY, NPC_HEIGHT * 2.5f);

        // Also check puddle lanes to prevent visual overlap
        if (puddleSpawner != null) {
            occupied.addAll(puddleSpawner.getOccupiedLanesNear(relativeY, 400f));
        }

        if (occupied.size() >= 2) {
            return; // already 2 lanes blocked — leave at least 1 free
        }

        // Pick a random lane that is NOT already occupied
        List<Integer> freeLanes = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (!occupied.contains(i))
                freeLanes.add(i);
        }
        int laneIndex = freeLanes.get((int) (Math.random() * freeLanes.size()));

        float laneX = RoadRenderer.ROAD_LEFT + (laneIndex + 0.5f) * RoadRenderer.ROAD_WIDTH / 3f;
        float bodyX = laneX / Constants.PPM;
        float bodyY = (relativeY + NPC_HEIGHT / 2f) / Constants.PPM;

        PhysicsBody body = world.createBody(
                BodyDef.BodyType.KinematicBody, // Changed from StaticBody - allows collision response
                bodyX,
                bodyY,
                (NPC_WIDTH / Constants.PPM) / 2f * 0.2f,
                (NPC_HEIGHT / Constants.PPM) / 2f * 0.2f,
                1f, // density (matters for collision response)
                0f, // friction
                false, // Changed from true - NOT a sensor, creates real collisions
                null // userData will be set by NPCCar constructor
        );

        // Create NPC car entity
        NPCCar npc = new NPCCar(
                "car2.png", // Using same texture as player car for now - can change later
                laneIndex,
                relativeY,
                NPC_WIDTH,
                NPC_HEIGHT,
                body);

        // Add to manager and active list
        entityManager.addEntity(npc);
        activeNPCs.add(npc);

        System.out.println("NPC car spawned in lane " + laneIndex);
    }

    /**
     * Changes the spawn rate (useful for difficulty scaling).
     *
     * @param newInterval New time between spawns (seconds)
     */
    public void setSpawnInterval(float newInterval) {
        this.spawnInterval = newInterval;
    }

    /**
     * Gets the number of currently active NPC cars.
     *
     * @return Number of NPCs in the scene
     */
    public int getActiveCount() {
        return activeNPCs.size();
    }

    /**
     * Marks all active NPC cars as expired (for level transitions).
     * EntityManager will automatically remove them on next update().
     */
    public void clearAll() {
        for (NPCCar npc : activeNPCs) {
            npc.markExpired();
        }
        activeNPCs.clear();
    }

    /**
     * Returns the set of lane indices (0-2) that have an active NPC
     * whose relativeY is within {@code range} pixels of {@code nearY}.
     * Used by PuddleSpawner to avoid overlapping with NPC cars.
     */
    public Set<Integer> getOccupiedLanesNear(float nearY, float range) {
        Set<Integer> lanes = new HashSet<>();
        for (NPCCar npc : activeNPCs) {
            if (!npc.isExpired()
                    && Math.abs(npc.getRelativeY() - nearY) < range) {
                lanes.add(npc.getLaneIndex());
            }
        }
        return lanes;
    }
}