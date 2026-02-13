package io.github.raesleg.engine.movement;

import io.github.raesleg.engine.entity.Entity;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.physics.IPhysics;

public class MovementManager {

    private IPhysics physics;
    private EntityManager entityM;

    public MovementManager(IPhysics physics, EntityManager entityM) {
        this.physics = physics;
        this.entityM = entityM;
    }

    public void update(float deltaTime) {
        // snapshot once so iteration is safe even if entities are added during move()
        for (Entity e : entityM.getSnapshot()) {
            if (e instanceof IMovable m) {
                m.move(deltaTime);
            }
        }
        physics.step(deltaTime);
    }
}
