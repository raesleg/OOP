package io.github.raesleg.game.scene;

import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.physics.PhysicsWorld;
import io.github.raesleg.engine.system.IGameSystem;
import io.github.raesleg.game.factory.NPCCarSpawner;
import io.github.raesleg.game.factory.PickupableSpawner;
import io.github.raesleg.game.factory.TreeSpawner;

import java.util.List;

/**
 * TrafficSpawningSystem — Coordinates NPC car, pickup, and tree spawning.
 * Extracted from Level1Scene/Level2Scene to satisfy SRP: spawn lifecycle
 * management is one responsibility, independent of crosswalk encounters,
 * scoring, or audio.
 * Supports external spawn suppression (e.g. active crosswalks on screen).
 */
public final class TrafficSpawningSystem implements IGameSystem {

    private final NPCCarSpawner npcSpawner;
    private final PickupableSpawner pickupSpawner;
    private final TreeSpawner treeSpawner;

    // Frame-scoped values supplied by the owning scene
    private float npcScrollSpeed;
    private float scrollOffset;
    private float playerX; // Player X position for lane exclusion
    private boolean spawningEnabled = true;

    public TrafficSpawningSystem(EntityManager entityManager, PhysicsWorld world,
            float screenHeight, float npcSpawnInterval,
            float pickupSpawnInterval, float treeSpawnInterval,
            List<float[]> crosswalkExclusions) {
        this.npcSpawner = new NPCCarSpawner(entityManager, world, screenHeight,
                npcSpawnInterval, crosswalkExclusions);
        this.pickupSpawner = new PickupableSpawner(entityManager, world, screenHeight,
                pickupSpawnInterval, npcSpawner, crosswalkExclusions);
        this.treeSpawner = new TreeSpawner(entityManager, screenHeight, treeSpawnInterval);
    }

    // Alternative constructor without crosswalk exclusions (Level 2)
    public TrafficSpawningSystem(EntityManager entityManager, PhysicsWorld world,
            float screenHeight, float npcSpawnInterval,
            float pickupSpawnInterval, float treeSpawnInterval) {
        this(entityManager, world, screenHeight, npcSpawnInterval,
                pickupSpawnInterval, treeSpawnInterval, List.of());
    }

    // Called by the scene each frame before update()
    public void setFrameState(float npcScrollSpeed, float scrollOffset, float simulatedSpeed, float playerX) {
        this.npcScrollSpeed = npcScrollSpeed;
        this.scrollOffset = scrollOffset;
        this.playerX = playerX;
    }

    // Enables or disables NPC car spawning (e.g. active crosswalk suppression)
    public void setSpawningEnabled(boolean enabled) {
        this.spawningEnabled = enabled;
    }

    // Dynamically adjust NPC spawn interval for difficulty escalation
    public void setNpcSpawnInterval(float interval) {
        npcSpawner.setSpawnInterval(interval);
    }

    @Override
    public void update(float deltaTime) {
        npcSpawner.setSpawningEnabled(spawningEnabled);
        npcSpawner.setPlayerX(playerX);  // Pass player X for lane exclusion
        npcSpawner.update(deltaTime, npcScrollSpeed);
        pickupSpawner.update(deltaTime, scrollOffset);
        treeSpawner.update(deltaTime, scrollOffset);
    }

    @Override
    public void dispose() {
        npcSpawner.clearAll();
        pickupSpawner.clearAll();
        treeSpawner.clearAll();
    }

    public NPCCarSpawner getNpcSpawner() {
        return npcSpawner;
    }

    public PickupableSpawner getPickupSpawner() {
        return pickupSpawner;
    }
}