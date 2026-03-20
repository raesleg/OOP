package io.github.raesleg.game.factory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.physics.PhysicsWorld;
import io.github.raesleg.game.entities.vehicles.NPCCar;
import io.github.raesleg.game.scene.RoadRenderer;
import io.github.raesleg.game.zone.Puddle;

/**
 * PuddleSpawner — Periodically spawns Puddle zones on the road.
 * Respects exclusion zones (e.g. crosswalk positions) and avoids
 * spawning in lanes already occupied by NPC cars.
 */
public class PuddleSpawner {

    private final EntityManager entityManager;
    private final PhysicsWorld world;
    private final float screenHeight;
    private final NPCCarSpawner npcCarSpawner;

    private float spawnTimer;
    private final float spawnInterval;
    private final List<Puddle> activePuddles = new ArrayList<>();
    private final List<float[]> exclusionZones;

    /** Tracks (laneIndex, relativeY) for each active puddle. */
    private final List<int[]> puddleLanes = new ArrayList<>();

    private static final float PUDDLE_W = 100f;
    private static final float PUDDLE_H = 40f;

    /**
     * @param npcCarSpawner  reference for lane-overlap checking (may be null)
     * @param exclusionZones list of [minY, maxY] world-Y ranges where no puddle may
     *                       spawn
     */
    public PuddleSpawner(EntityManager entityManager, PhysicsWorld world,
            float screenHeight, float spawnInterval,
            NPCCarSpawner npcCarSpawner,
            List<float[]> exclusionZones) {
        this.entityManager = entityManager;
        this.world = world;
        this.screenHeight = screenHeight;
        this.spawnInterval = spawnInterval;
        this.spawnTimer = 0f;
        this.npcCarSpawner = npcCarSpawner;
        this.exclusionZones = (exclusionZones != null) ? exclusionZones : new ArrayList<>();
    }

    public void update(float deltaTime, float scrollOffset) {
        spawnTimer += deltaTime;
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f;
            spawnRandomPuddle(scrollOffset);
        }
        for (int i = activePuddles.size() - 1; i >= 0; i--) {
            Puddle p = activePuddles.get(i);
            if (p.isExpired()) {
                activePuddles.remove(i);
                puddleLanes.remove(i);
                continue;
            }
            p.updatePosition(scrollOffset);
        }
    }

    private void spawnRandomPuddle(float scrollOffset) {
        float relativeY = -scrollOffset + screenHeight + 300f;

        // Check exclusion zones
        for (float[] zone : exclusionZones) {
            if (relativeY >= zone[0] - PUDDLE_H && relativeY <= zone[1] + PUDDLE_H) {
                return;
            }
        }

        // Determine which lanes are blocked by NPC cars (wide check to prevent overlap)
        Set<Integer> blockedLanes = (npcCarSpawner != null)
                ? npcCarSpawner.getOccupiedLanesNear(relativeY, 400f)
                : Set.of();

        // Pick a free lane — skip if all lanes are blocked
        List<Integer> freeLanes = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (!blockedLanes.contains(i))
                freeLanes.add(i);
        }

        // Never fill all 3 lanes (NPC + puddle combined) — keep at least 1 free
        if (freeLanes.isEmpty() || blockedLanes.size() >= 2) {
            return;
        }

        int laneIndex = freeLanes.get((int) (Math.random() * freeLanes.size()));
        float laneX = RoadRenderer.ROAD_LEFT
                + (laneIndex + 0.5f) * RoadRenderer.ROAD_WIDTH / 3f;

        Puddle puddle = new Puddle(world, laneX, relativeY, PUDDLE_W, PUDDLE_H);
        entityManager.addEntity(puddle);
        activePuddles.add(puddle);
        puddleLanes.add(new int[] { laneIndex, (int) relativeY });
    }

    /**
     * Returns the set of lane indices (0-2) that have an active puddle
     * whose relativeY is within {@code range} pixels of {@code nearY}.
     * Used by NPCCarSpawner to avoid overlapping with puddles.
     */
    public Set<Integer> getOccupiedLanesNear(float nearY, float range) {
        Set<Integer> lanes = new HashSet<>();
        for (int i = 0; i < puddleLanes.size(); i++) {
            if (i < activePuddles.size() && !activePuddles.get(i).isExpired()) {
                int[] info = puddleLanes.get(i);
                if (Math.abs(info[1] - nearY) < range) {
                    lanes.add(info[0]);
                }
            }
        }
        return lanes;
    }

    /** Draws all active puddles (called before entity pass for correct z-order). */
    public void render(com.badlogic.gdx.graphics.g2d.SpriteBatch batch) {
        for (Puddle p : activePuddles) {
            if (!p.isExpired()) {
                p.drawPuddle(batch);
            }
        }
    }

    public void clearAll() {
        for (Puddle p : activePuddles)
            p.markExpired();
        activePuddles.clear();
        puddleLanes.clear();
    }
}
