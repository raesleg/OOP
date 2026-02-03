package io.github.raesleg.OOP;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


public class EntityManager {

    /* Private Variables */

    private List<Entity> entityList = new ArrayList<Entity>();
    
    // idk if this is necessary but its in diagram
    private List<Entity> pendingEntities = new ArrayList<Entity>();

    /* Public functions */
    public EntityManager() {};

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

    public <T> void forEach(Class<T> type, Consumer<T> action) {
        for (Entity e : entityList) {
            if (type.isInstance(e)) {
                action.accept(type.cast(e));
            }
        }
    }
}
