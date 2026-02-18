package io.github.raesleg.engine.movement;

import io.github.raesleg.engine.entity.Entity;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.physics.PhysicsWorld;

public class MovementManager {

    private PhysicsWorld world;
    private EntityManager entityManager;

    public MovementManager(PhysicsWorld world, EntityManager entityManager) {
        this.world = world;
        this.entityManager = entityManager;
    }

    public void update(float deltaTime) {
        // snapshot once so iteration is safe even if entities are added during move()
        for (Entity e : entityManager.getSnapshot()) {
            if (e instanceof IMovable m) {
                m.move(deltaTime);
            }
        }
        world.step(deltaTime);
    }
}
