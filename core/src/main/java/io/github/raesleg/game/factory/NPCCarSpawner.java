package io.github.raesleg.game.factory;

import java.util.ArrayList;
import java.util.List;

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
 * removes them when they go off-screen. This avoids needing a removeEntity() method.
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
    private float spawnYOffset;  // How far ahead to spawn (pixels)
    
    /* Active NPCs - tracked for counting/debugging only */
    private final List<NPCCar> activeNPCs;
    
    /* NPC car dimensions (pixels) */
    private static final float NPC_WIDTH = 70f;
    private static final float NPC_HEIGHT = 120f;
    
    /**
     * Creates an NPC car spawner.
     * 
     * @param entityManager EntityManager to add/remove cars from
     * @param world PhysicsWorld for creating collision bodies
     * @param screenHeight Screen height in pixels
     * @param spawnInterval Time between spawns (seconds)
     */
    public NPCCarSpawner(EntityManager entityManager, PhysicsWorld world, 
                         float screenHeight, float spawnInterval) {
        this.entityManager = entityManager;
        this.world = world;
        this.screenHeight = screenHeight;
        this.spawnInterval = spawnInterval;
        this.spawnYOffset = screenHeight + 200f; // Spawn just above screen
        this.spawnTimer = 0f;
        this.activeNPCs = new ArrayList<>();
    }
    
    /**
     * Updates the spawner — spawns new cars and updates positions.
     * Call this every frame from your level's updateGame().
     * 
     * Note: Off-screen NPCs are automatically removed by EntityManager
     * via the IExpirable interface, so no manual cleanup needed here.
     * 
     * @param deltaTime Time since last frame (seconds)
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
     * Spawns a new NPC car in a random lane.
     * 
     * @param scrollOffset Current scroll offset (used for relative positioning)
     */
    private void spawnRandomCar(float scrollOffset) {
        // Random lane (0, 1, or 2)
        int laneIndex = (int) (Math.random() * 3);
        
        // Calculate spawn Y position (relative to current scroll)
        // This is the car's "world position" that stays constant
        float relativeY = -scrollOffset + spawnYOffset;
        
        // Create physics body (KINEMATIC for collision detection with physical response)
        // Kinematic bodies can be moved manually but don't respond to forces
        // They DO create collision responses with dynamic bodies (player car)
        float laneX = RoadRenderer.ROAD_LEFT + (laneIndex + 0.5f) * RoadRenderer.ROAD_WIDTH / 3f;
        float bodyX = laneX / Constants.PPM;
        float bodyY = (relativeY + NPC_HEIGHT / 2f) / Constants.PPM;
        
        PhysicsBody body = world.createBody(
            BodyDef.BodyType.KinematicBody,  // Changed from StaticBody - allows collision response
            bodyX,
            bodyY,
            (NPC_WIDTH / Constants.PPM) / 2f,
            (NPC_HEIGHT / Constants.PPM) / 2f,
            1f,     // density (matters for collision response)
            0f,     // friction
            false,  // Changed from true - NOT a sensor, creates real collisions
            null    // userData will be set by NPCCar constructor
        );
        
        // Create NPC car entity
        NPCCar npc = new NPCCar(
            "car.png",  // Using same texture as player car for now - can change later
            laneIndex,
            relativeY,
            NPC_WIDTH,
            NPC_HEIGHT,
            body
        );
        
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
            npc.markExpired(); // Will be auto-removed by EntityManager
        }
        activeNPCs.clear();
    }
}