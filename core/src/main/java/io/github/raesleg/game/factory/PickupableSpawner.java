package io.github.raesleg.game.factory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.badlogic.gdx.physics.box2d.BodyDef;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.engine.physics.PhysicsWorld;
import io.github.raesleg.game.entities.Pickupable;
import io.github.raesleg.game.scene.RoadRenderer;

/**
 * PickupableSpawner — Periodically spawns collectible Pickupable squares.
 * Avoids lanes occupied by NPC cars.
 */
public class PickupableSpawner {

    private final EntityManager entityManager;
    private final PhysicsWorld world;
    private final float screenHeight;
    private final NPCCarSpawner npcCarSpawner;

    private float spawnTimer;
    private final float spawnInterval;
    private final List<Pickupable> activePickups = new ArrayList<>();

    private static final float PICKUP_SIZE = 100f; //changed for battery.png

    public PickupableSpawner(EntityManager entityManager, PhysicsWorld world,
            float screenHeight, float spawnInterval,
            NPCCarSpawner npcCarSpawner) {
        this.entityManager = entityManager;
        this.world = world;
        this.screenHeight = screenHeight;
        this.spawnInterval = spawnInterval;
        this.spawnTimer = 0f;
        this.npcCarSpawner = npcCarSpawner;
    }

    public void update(float deltaTime, float scrollOffset) {
        spawnTimer += deltaTime;
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f;
            spawnPickup(scrollOffset);
        }
        Iterator<Pickupable> it = activePickups.iterator();
        while (it.hasNext()) {
            Pickupable p = it.next();
            if (p.isExpired()) {
                it.remove();
                continue;
            }
            p.updatePosition(scrollOffset);
        }
    }

    private void spawnPickup(float scrollOffset) {
        float relativeY = -scrollOffset + screenHeight + 400f;

        // Pick a lane not currently occupied by NPC
        Set<Integer> blocked = (npcCarSpawner != null)
                ? npcCarSpawner.getOccupiedLanesNear(relativeY, 300f)
                : Set.of();

        List<Integer> freeLanes = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (!blocked.contains(i))
                freeLanes.add(i);
        }
        if (freeLanes.isEmpty())
            return;

        int laneIndex = freeLanes.get((int) (Math.random() * freeLanes.size()));
        float laneX = RoadRenderer.ROAD_LEFT
                + (laneIndex + 0.5f) * RoadRenderer.ROAD_WIDTH / 3f;

        PhysicsBody body = world.createBody(
                BodyDef.BodyType.KinematicBody,
                laneX / Constants.PPM,
                relativeY / Constants.PPM,
                (PICKUP_SIZE / Constants.PPM) / 2f,
                (PICKUP_SIZE / Constants.PPM) / 2f,
                0f, 0f, true, null);

        Pickupable pickup = new Pickupable(body, laneX, relativeY,
                PICKUP_SIZE, PICKUP_SIZE);
        entityManager.addEntity(pickup);
        activePickups.add(pickup);
    }

    public void clearAll() {
        for (Pickupable p : activePickups)
            p.markExpired();
        activePickups.clear();
    }
}
