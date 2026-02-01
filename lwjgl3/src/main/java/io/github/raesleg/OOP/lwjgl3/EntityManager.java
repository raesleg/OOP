package io.github.raesleg.OOP.lwjgl3;

import java.util.ArrayList;
import java.util.List;

public class EntityManager {

    /* Private Variables */

    private List<Entity> entityList = new ArrayList<Entity>();
    
    // idk if this is necessary but its in diagram
    private List<Entity> pendingEntities = new ArrayList<Entity>();

    private MovementManager movementM;
    private CollisionManager collisionM;

    /* Public functions */
    public void EntityManager() {};

    // w params?
    //public void EntityManager(Entity entities..);

    public void initialise() {};

    public void update(double deltaTime) {};

    public void addEntity(Entity entity) {};

    public void removeEntity(Entity entity) {};

    public int getEntityCount() {
        return entityList.size();
    };

    public Entity getEntity(int index) {
        return null;  // temp
    }

    public List<IMovable> getMovableEntities() {
        return null;  // temp
    };

}
