package io.github.raesleg.OOP.lwjgl3;

public abstract class CollisionManager {

    public void collisionResolver(EntityManager entityM){};

    public void collisionDetector(Entity entity1, Entity entity2) {};

    public void collisionHandler(Entity entity1, Entity entity2) {};

}
