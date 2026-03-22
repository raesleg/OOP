package io.github.raesleg.game.factory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.physics.PhysicsWorld;
import io.github.raesleg.game.movement.SurfaceEffect;
import io.github.raesleg.game.scene.RoadRenderer;
import io.github.raesleg.game.zone.RoadHazard;

/**
 * HazardSpawner — Spawns RoadHazard zones (puddles, oil spills, etc.)
 *
 * Hazard type is injected via constructor — no subclass or separate
 * spawner needed for oil spills (Open/Closed Principle).
 *
 * Usage:
 *   new HazardSpawner(..., SurfaceEffect.PUDDLE,    "puddle.png")
 *   new HazardSpawner(..., SurfaceEffect.MUD, "mud.png")
 */
public class RoadHazardSpawner {

    private final EntityManager entityManager;
    private final PhysicsWorld world;
    private final float screenHeight;
    private final NPCCarSpawner npcCarSpawner;
    private final List<float[]> exclusionZones;
    private final SurfaceEffect surfaceEffect;
    private final String texturePath;

    private float spawnTimer;
    private final float spawnInterval;

    private final List<RoadHazard> activeHazards = new ArrayList<>();
    private final List<int[]> hazardLanes = new ArrayList<>();

    private static final float HAZARD_W = 100f;
    private static final float HAZARD_H = 40f;


    /** Backwards-compatible constructor — spawns puddles. */
    public RoadHazardSpawner(EntityManager entityManager, PhysicsWorld world,
            float screenHeight, float spawnInterval,
            NPCCarSpawner npcCarSpawner, List<float[]> exclusionZones) {
        this(entityManager, world, screenHeight, spawnInterval,
                npcCarSpawner, exclusionZones,
                SurfaceEffect.PUDDLE, "puddle.png");
    }

    /** Full constructor — specify hazard type via SurfaceEffect + texture. */
    public RoadHazardSpawner(EntityManager entityManager, PhysicsWorld world,
            float screenHeight, float spawnInterval,
            NPCCarSpawner npcCarSpawner, List<float[]> exclusionZones,
            SurfaceEffect surfaceEffect, String texturePath) {
        this.entityManager = entityManager;
        this.world = world;
        this.screenHeight = screenHeight;
        this.spawnInterval = spawnInterval;
        this.spawnTimer = 0f;
        this.npcCarSpawner = npcCarSpawner;
        this.exclusionZones = (exclusionZones != null) ? exclusionZones : new ArrayList<>();
        this.surfaceEffect = surfaceEffect;
        this.texturePath = texturePath;
    }

    public void update(float deltaTime, float scrollOffset) {
        spawnTimer += deltaTime;
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f;
            spawnRandomHazard(scrollOffset);
        }
        for (int i = activeHazards.size() - 1; i >= 0; i--) {
            RoadHazard h = activeHazards.get(i);
            if (h.isExpired()) {
                activeHazards.remove(i);
                hazardLanes.remove(i);
                continue;
            }
            h.updatePosition(scrollOffset);
        }
    }

    private void spawnRandomHazard(float scrollOffset) {
        float relativeY = -scrollOffset + screenHeight + 300f;

        for (float[] zone : exclusionZones) {
            if (relativeY >= zone[0] - HAZARD_H && relativeY <= zone[1] + HAZARD_H)
                return;
        }

        Set<Integer> blockedLanes = (npcCarSpawner != null)
                ? npcCarSpawner.getOccupiedLanesNear(relativeY, 400f)
                : Set.of();


        List<Integer> freeLanes = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (!blockedLanes.contains(i)) freeLanes.add(i);
        }

        if (freeLanes.isEmpty() || blockedLanes.size() >= 2) return;

        int laneIndex = freeLanes.get((int) (Math.random() * freeLanes.size()));
        float laneX = RoadRenderer.ROAD_LEFT
                + (laneIndex + 0.5f) * RoadRenderer.ROAD_WIDTH / 3f;

        RoadHazard hazard = new RoadHazard(
                world, laneX, relativeY, HAZARD_W, HAZARD_H,
                surfaceEffect, texturePath);

        entityManager.addEntity(hazard);
        activeHazards.add(hazard);
        hazardLanes.add(new int[]{ laneIndex, (int) relativeY });
    }

    public Set<Integer> getOccupiedLanesNear(float nearY, float range) {
        Set<Integer> lanes = new HashSet<>();
        for (int i = 0; i < hazardLanes.size(); i++) {
            if (i < activeHazards.size() && !activeHazards.get(i).isExpired()) {
                int[] info = hazardLanes.get(i);
                if (Math.abs(info[1] - nearY) < range)
                    lanes.add(info[0]);
            }
        }
        return lanes;
    }

    public void render(com.badlogic.gdx.graphics.g2d.SpriteBatch batch) {
        for (RoadHazard h : activeHazards) {
            if (!h.isExpired()) h.drawHazard(batch);
        }
    }

    public void clearAll() {
        for (RoadHazard h : activeHazards) h.markExpired();
        activeHazards.clear();
        hazardLanes.clear();
    }
}