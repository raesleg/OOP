package io.github.raesleg.engine.movement;

import io.github.raesleg.engine.entity.Entity;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.physics.PhysicsWorld;

public class MovementManager {

    private PhysicsWorld physicsWorld;
    private EntityManager entityM;

    public MovementManager(PhysicsWorld physicsWorld, EntityManager entityM) {
        this.physicsWorld = physicsWorld;
        this.entityM = entityM;
    }

    public void update(float deltaTime) {
        // snapshot once so iteration is safe even if entities are added during move()
        for (Entity e : entityM.getSnapshot()) {
            if (e instanceof IMovable m) {
                m.move(deltaTime);
            }
        }
        physicsWorld.step(deltaTime);
    }
}
