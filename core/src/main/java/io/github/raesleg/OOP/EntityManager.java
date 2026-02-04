package io.github.raesleg.OOP;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EntityManager {

    private final List<Entity> entityList = new ArrayList<>();
    private final List<Entity> pendingEntities = new ArrayList<>();

    public EntityManager() {}

    public void initialise() {}

    // Use float to match LibGDX + your MovementManager
    public void update(float deltaTime) {
        // Merge pending entities safely (ownership stays inside EntityManager)
        if (!pendingEntities.isEmpty()) {
            entityList.addAll(pendingEntities);
            pendingEntities.clear();
        }
    }

    public void addEntity(Entity entity) {
        if (entity != null) {
            pendingEntities.add(entity);
        }
    }

    public void removeEntity(Entity entity) {
        entityList.remove(entity);
        pendingEntities.remove(entity);
    }

    public int getEntityCount() {
        return entityList.size();
    }

    public Entity getEntity(int index) {
        return entityList.get(index);
    }

    // Safe iteration (no list exposure) — matches your professor’s feedback
    public <T> void forEach(Class<T> type, Consumer<T> action) {
        for (Entity e : entityList) {
            if (type.isInstance(e)) {
                action.accept(type.cast(e));
            }
        }
    }
}
