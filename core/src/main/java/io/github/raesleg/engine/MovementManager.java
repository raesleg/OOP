package io.github.raesleg.engine;

public class MovementManager {

    private PhysicsWorld physicsWorld;

    public MovementManager(PhysicsWorld physicsWorld) {
        this.physicsWorld = physicsWorld;
    }

    public void update(EntityManager entityM, float dt) {
        // snapshot once so iteration is safe even if entities are added during move()
        for (Entity e : entityM.getSnapshot()) {
            if (e instanceof IMovable m) {
                m.move(dt);
            }
        }
        physicsWorld.step(dt);
    }
}
