package io.github.raesleg.game.factory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import io.github.raesleg.engine.physics.BodyType;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.movement.AIControlled;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.engine.physics.PhysicsWorld;

import io.github.raesleg.game.entities.vehicles.NPCCar;
import io.github.raesleg.game.movement.NpcDrivingStrategy;
import io.github.raesleg.game.movement.AIPerceptionService;
import io.github.raesleg.game.movement.CarMovementModel;
import io.github.raesleg.game.movement.SensorComponent;
import io.github.raesleg.game.movement.VehicleProfile;
import io.github.raesleg.game.scene.RoadRenderer;

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

    /** Optional reference to hazard spawner for lane-overlap prevention. */
    private RoadHazardSpawner hazardSpawner;

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
        this.spawnYOffset = screenHeight + 40f; // Spawn just above screen
        this.spawnTimer = 0f;
        this.activeNPCs = new ArrayList<>();
        this.exclusionZones = (exclusionZones != null) ? exclusionZones : new ArrayList<>();
    }

    /** Sets the hazard spawner reference for lane-overlap prevention. */
    public void setHazardSpawner(RoadHazardSpawner hazardSpawner) {
        this.hazardSpawner = hazardSpawner;
    }

    /**
     * scrollPixelsPerSecond = road/world downward speed in pixels/sec
     */
    public void update(float deltaTime, float scrollPixelsPerSecond) {
        // Update spawn timer
        spawnTimer += deltaTime;

        // Spawn new car if interval passed
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f;
            spawnRandomCar();
        }

        // Update all active NPC positions based on scroll
        // Also clean up our tracking list (NPCs are auto-removed by EntityManager)
        activeNPCs.removeIf(npc -> {
            if (npc.isExpired()) {
                return true;
            }

            npc.updateLifeCycle(scrollPixelsPerSecond, deltaTime, screenHeight);
            return npc.isExpired();
        });
    }

    /**
     * Spawns a new NPC car in a random lane, ensuring at most 2 lanes are
     * occupied near the spawn Y to always leave a gap for the player.
     */
    private void spawnRandomCar() {
        float relativeY = spawnYOffset;

        // Skip if spawn Y falls inside an exclusion zone (e.g. crosswalk)
        for (float[] zone : exclusionZones) {
            if (relativeY >= zone[0] - NPC_HEIGHT && relativeY <= zone[1] + NPC_HEIGHT) {
                return;
            }
        }

        // Determine which lanes are already occupied near the spawn Y
        Set<Integer> occupied = getOccupiedLanesNear(relativeY, NPC_HEIGHT * 2.5f);

        // Also check hazard lanes to prevent visual overlap
        if (hazardSpawner != null) {
            occupied.addAll(hazardSpawner.getOccupiedLanesNear(relativeY, 400f));
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

        if (freeLanes.isEmpty()) {
            return;
        }

        int laneIndex = freeLanes.get((int) (Math.random() * freeLanes.size()));

        float laneX = RoadRenderer.ROAD_LEFT + (laneIndex + 0.5f) * RoadRenderer.ROAD_WIDTH / 3f;
        // float bodyX = laneX / Constants.PPM;
        // float bodyY = (relativeY + NPC_HEIGHT / 2f) / Constants.PPM;

        PhysicsBody body = world.createBody(
                BodyType.DYNAMIC,
                (laneX) / Constants.PPM,
                (relativeY + NPC_HEIGHT / 2f) / Constants.PPM,
                (NPC_WIDTH / Constants.PPM) / 2f * 0.3f,
                (NPC_HEIGHT / Constants.PPM) / 2f * 0.3f,
                50f,
                0f,
                false,
                null);
        body.setLinearDamping(8f);

        AIPerceptionService perceptionService = new AIPerceptionService(entityManager);

        SensorComponent sensor = new SensorComponent(
                220f, // forward range
                60f, // side range
                70f, // stop distance
                120f // follow distance
        );

        NPCCar npc = new NPCCar(
                "car2.png",
                laneX - NPC_WIDTH / 2f,
                relativeY,
                NPC_WIDTH,
                NPC_HEIGHT,
                laneIndex,
                new AIControlled(),
                new NpcDrivingStrategy(perceptionService, sensor),
                new CarMovementModel(VehicleProfile.npcTraffic()),
                body,
                sensor);
        // Add to manager and active list
        entityManager.addEntity(npc);
        activeNPCs.add(npc);

        Gdx.app.log("NPCCarSpawner", "NPC spawned: lane=" + laneIndex
                + ", x=" + npc.getX() + ", y=" + npc.getY()
                + ", active=" + activeNPCs.size());
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
     * Used by HazardSpawner to avoid overlapping with NPC cars.
     */
    public Set<Integer> getOccupiedLanesNear(float nearY, float range) {
        Set<Integer> lanes = new HashSet<>();
        for (NPCCar npc : activeNPCs) {
            if (npc.isExpired() || npc.getPhysicsBody() == null) {
                continue;
            }

            if (Math.abs(npc.getY() - nearY) < range) {
                lanes.add(npc.getLaneIndex());
            }
        }
        return lanes;
    }
}