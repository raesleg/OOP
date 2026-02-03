package io.github.raesleg.OOP;

public class MovementManager {
    public void update(EntityManager entityM, float deltaTime){
        entityM.forEach(IMovable.class, movable -> movable.move(deltaTime));
    };
}
