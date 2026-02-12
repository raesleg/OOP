package io.github.raesleg.engine.physics;

public class PhysicsManager {

    private PhysicsWorld world;

    public PhysicsManager(PhysicsWorld world) {
        this.world = world;
    }

    public PhysicsWorld getWorld() {
        return world;
    }

    public void step(float deltaTime) {
        world.step(deltaTime);
    }
    
}
