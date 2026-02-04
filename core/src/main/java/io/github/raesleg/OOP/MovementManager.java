package io.github.raesleg.OOP;

public class MovementManager {

    private final PhysicsWorld physicsWorld;

    public MovementManager(PhysicsWorld physicsWorld) {
        this.physicsWorld = physicsWorld;
    }

    public void update(EntityManager entityM, float dt) {
        // Let entities apply forces / velocities
        entityM.forEach(IMovable.class, m -> m.move(dt));
        // Step physics simulation
        physicsWorld.step(dt);
    }
}
