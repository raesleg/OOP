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
import io.github.raesleg.game.movement.NpcDrivingStrategy;
import io.github.raesleg.game.movement.AIPerceptionService;
import io.github.raesleg.game.movement.CarMovementModel;
import io.github.raesleg.game.movement.SensorComponent;
import io.github.raesleg.game.movement.VehicleProfile;
import io.github.raesleg.game.scene.RoadRenderer;

/** Factory responsible for spawning NPC traffic with 
 * lane awareness and collision avoidance (ILaneOccupancy) */

public class NPCCarSpawner implements ILaneOccupancy {

    // Core Dependencies
    private final EntityManager entityManager;
    private final PhysicsWorld world; 
    private final float screenHeight; 

    // Spawn timing and positioning
    private float spawnTimer; 
    private float spawnInterval; 
    private float spawnYOffset;

    // NPC lifecycle tracking
    private final List<NPCCar> activeNPCs;

    private float playerX = -1f; // Player X coordinate; used to exclude their lane at spawn

    // Level design constraints: Y ranges where NPCs cannot spawn (hazards, crosswalks)
    private final List<float[]> exclusionZones;


    private ILaneOccupancy hazardOccupancy; // Prevent visual overlap
    private boolean spawningEnabled = true; // Disabled during crosswalks/critical events


    private static final float APPROACH_SPEED = -25f; // Speed delta after preview (negative = slower)

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

    // Sets the hazard occupancy reference for lane-overlap prevention
    public void setHazardOccupancy(ILaneOccupancy hazardOccupancy) {
        this.hazardOccupancy = hazardOccupancy;
    }

    public void setSpawningEnabled(boolean enabled) {
        this.spawningEnabled = enabled;
    }

    // Set player X position each frame for lane exclusion at spawn time
    public void setPlayerX(float playerX) {
        this.playerX = playerX;
    }

    // Update spawn timer and NPC lifecycle; trigger spawning when interval elapses
    public void update(float deltaTime, float scrollPixelsPerSecond) {
        // Update spawn timer
        spawnTimer += deltaTime;

        // Spawn new car if interval passed and spawning is enabled
        if (spawnTimer >= spawnInterval && spawningEnabled) {
            spawnTimer = 0f;
            spawnRandomCar();
        }

        // Update lifecycle for all active NPCs; cleanup expired ones from tracking list
        activeNPCs.removeIf(npc -> {
            if (npc.isExpired()) {
                return true;
            }

            npc.updateLifeCycle(scrollPixelsPerSecond, deltaTime, screenHeight);
            return npc.isExpired();
        });
    }

    // Small chance of double spawns for traffic density variety
    private static final float DOUBLE_SPAWN_CHANCE = 0.30f;

    // Spawn 1-2 NPCs in available lanes while excluding player's lane and respecting exclusion zones
    private void spawnRandomCar() {
        float relativeY = spawnYOffset;

        // Skip if spawn Y falls inside an exclusion zone
        for (float[] zone : exclusionZones) {
            if (relativeY >= zone[0] - GameConstants.NPC_HEIGHT && relativeY <= zone[1] + GameConstants.NPC_HEIGHT) {
                return; // Don't spawn during level hazards
            }
        }

        // Determine which lanes are already occupied near the spawn Y
        Set<Integer> occupied = getOccupiedLanesNear(relativeY, GameConstants.NPC_HEIGHT * 2.5f);

        if (hazardOccupancy != null) {
            occupied.addAll(hazardOccupancy.getOccupiedLanesNear(relativeY, 400f));
        }

        // Exclude player's lane to prevent spawn collisions
        int playerLane = getPlayerLane();
        if (playerLane >= 0 && playerLane < 3) {
            occupied.add(playerLane); // Reserve player's lane
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

    // Instantiate a complete NPC with physics body, AI perception, and movement strategy
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

        // Create AI perception system for obstacle detection
        AIPerceptionService perceptionService = new AIPerceptionService(entityManager);

        // Configure sensor ranges for avoidance and pathfinding
        SensorComponent sensor = new SensorComponent(
                220f,
                60f,
                70f,
                120f 
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


    public int getActiveCount() {
        return activeNPCs.size();
    }

    // Mark all NPCs for removal on level transition
    public void clearAll() {
        for (NPCCar npc : activeNPCs) {
            npc.markExpired();
        }
        activeNPCs.clear();
    }

    // Return lanes with NPCs near given Y (for avoiding spawn collisions and hazard overlaps)
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


    // Calculate player's current lane (0-2) from X position, or -1 if not available
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