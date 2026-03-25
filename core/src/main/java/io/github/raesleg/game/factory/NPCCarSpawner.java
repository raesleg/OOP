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

import io.github.raesleg.game.GameConstants;
import io.github.raesleg.game.entities.vehicles.NPCCar;
import io.github.raesleg.game.entities.vehicles.PlayerCar;
import io.github.raesleg.game.movement.NpcDrivingStrategy;
import io.github.raesleg.game.movement.AIPerceptionService;
import io.github.raesleg.game.movement.CarMovementModel;
import io.github.raesleg.game.movement.SensorComponent;
import io.github.raesleg.game.movement.VehicleProfile;
import io.github.raesleg.game.scene.RoadRenderer;

public class NPCCarSpawner implements ILaneOccupancy {

    private final EntityManager entityManager;
    private final PhysicsWorld world;
    private final float screenHeight;

    /* Spawning configuration */
    private float spawnTimer;
    private float spawnInterval; // Seconds between spawns
    private float spawnYOffset; // How far ahead to spawn (pixels)

    /* Active NPCs - tracked for counting/debugging only */
    private final List<NPCCar> activeNPCs;

    /* Player position for collision avoidance at spawn */
    private float playerY = -1f;
    private float playerX = -1f; // Player X position to exclude their lane

    /*
     * Exclusion zones — Y ranges where no NPC may spawn (e.g. crosswalk positions)
     */
    private final List<float[]> exclusionZones;

    /** Optional reference to hazard spawner for lane-overlap prevention. */
    private ILaneOccupancy hazardOccupancy;

    /** When false, no new NPC cars will spawn (e.g. active crosswalk on screen). */
    private boolean spawningEnabled = true;

    /** Pixels of NPC visible at top of screen during preview peek. */
    private static final float PREVIEW_PEEK = 35f;

    /** Extra downward speed (px/s) — 10mph slower than player max speed. */
    private static final float APPROACH_SPEED = -25f; // Negative = slower, player catches and overtakes

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
        this.spawnYOffset = -150f; // Spawn above screen, will scroll into view
        this.spawnTimer = 0f;
        this.activeNPCs = new ArrayList<>();
        this.exclusionZones = (exclusionZones != null) ? exclusionZones : new ArrayList<>();
    }

    /** Sets the hazard occupancy reference for lane-overlap prevention. */
    public void setHazardOccupancy(ILaneOccupancy hazardOccupancy) {
        this.hazardOccupancy = hazardOccupancy;
    }

    /**
     * Enables or disables NPC spawning (e.g. suppress during active crosswalks).
     */
    public void setSpawningEnabled(boolean enabled) {
        this.spawningEnabled = enabled;
    }
    /** Sets the player Y position each frame for spawn collision avoidance. */
    public void setPlayerY(float playerY) {
        this.playerY = playerY;
    }

    /** Sets the player X position each frame for lane exclusion. */
    public void setPlayerX(float playerX) {
        this.playerX = playerX;
    }
    /**
     * scrollPixelsPerSecond = road/world downward speed in pixels/sec
     */
    public void update(float deltaTime, float scrollPixelsPerSecond) {
        // Update spawn timer
        spawnTimer += deltaTime;

        // Spawn new car if interval passed and spawning is enabled
        if (spawnTimer >= spawnInterval && spawningEnabled) {
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

    /** Chance (0–1) that a second NPC spawns alongside the first. */
    private static final float DOUBLE_SPAWN_CHANCE = 0.30f;

    /**
     * Spawns one or two NPC cars in random lanes, always leaving at least
     * one lane free for the player.
     */
    private void spawnRandomCar() {
        float relativeY = spawnYOffset;

        // Skip if spawn Y falls inside an exclusion zone (e.g. crosswalk)
        for (float[] zone : exclusionZones) {
            if (relativeY >= zone[0] - GameConstants.NPC_HEIGHT && relativeY <= zone[1] + GameConstants.NPC_HEIGHT) {
                return;
            }
        }

        // Determine which lanes are already occupied near the spawn Y
        Set<Integer> occupied = getOccupiedLanesNear(relativeY, GameConstants.NPC_HEIGHT * 2.5f);

        // Also check hazard lanes to prevent visual overlap
        if (hazardOccupancy != null) {
            occupied.addAll(hazardOccupancy.getOccupiedLanesNear(relativeY, 400f));
        }

        // Exclude the lane the player is currently in — prevent collision at spawn
        int playerLane = getPlayerLane();
        if (playerLane >= 0 && playerLane < 3) {
            occupied.add(playerLane);
        }

        if (occupied.size() >= 2) {
            return; // already 2 lanes blocked — leave at least 1 free
        }

        // Collect free lanes
        List<Integer> freeLanes = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (!occupied.contains(i))
                freeLanes.add(i);
        }

        if (freeLanes.isEmpty()) {
            return;
        }

        // Always spawn one NPC
        int firstIdx = (int) (Math.random() * freeLanes.size());
        int firstLane = freeLanes.remove(firstIdx);
        spawnSingleNPC(firstLane, relativeY);

        // Possibly spawn a second NPC — only if ≥ 1 free lane remains
        if (freeLanes.size() >= 2 && Math.random() < DOUBLE_SPAWN_CHANCE) {
            int secondIdx = (int) (Math.random() * freeLanes.size());
            int secondLane = freeLanes.get(secondIdx);
            spawnSingleNPC(secondLane, relativeY);
        }
    }

    /** Creates a single NPC car in the given lane at the given Y offset. */
    private void spawnSingleNPC(int laneIndex, float relativeY) {
        float laneX = RoadRenderer.ROAD_LEFT + (laneIndex + 0.5f) * RoadRenderer.ROAD_WIDTH / 3f;

        PhysicsBody body = world.createBody(
                BodyType.KINEMATIC,
                (laneX) / Constants.PPM,
                (relativeY + GameConstants.NPC_HEIGHT / 2f) / Constants.PPM,
                (GameConstants.NPC_WIDTH / Constants.PPM) / 2f * 0.3f,
                (GameConstants.NPC_HEIGHT / Constants.PPM) / 2f * 0.3f,
                50f,
                0f,
                false,
                null);

        AIPerceptionService perceptionService = new AIPerceptionService(entityManager);

        SensorComponent sensor = new SensorComponent(
                220f, // forward range
                60f, // side range
                70f, // stop distance
                120f // follow distance
        );

        NPCCar npc = new NPCCar(
                "car2.png",
                laneX - GameConstants.NPC_WIDTH / 2f,
                relativeY,
                GameConstants.NPC_WIDTH,
                GameConstants.NPC_HEIGHT,
                laneIndex,
                APPROACH_SPEED,
                new AIControlled(),
                new NpcDrivingStrategy(perceptionService, sensor),
                new CarMovementModel(VehicleProfile.npcTraffic()),
                body,
                sensor);
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
     */
    @Override
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


    /**
     * Determines which lane (0-2) the player is currently in based on X position.
     * Returns -1 if unavailable.
     */
    private int getPlayerLane() {
        if (playerX < 0) return -1; // Not available
        
        float laneWidth = RoadRenderer.ROAD_WIDTH / 3f;
        float relativeX = playerX - RoadRenderer.ROAD_LEFT;
        
        if (relativeX < 0 || relativeX > RoadRenderer.ROAD_WIDTH) {
            return -1; // Player outside road
        }
        
        int lane = (int) (relativeX / laneWidth);
        return Math.max(0, Math.min(2, lane)); // Clamp to 0-2
    }
}