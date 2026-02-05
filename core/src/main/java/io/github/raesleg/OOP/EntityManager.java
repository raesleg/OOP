package io.github.raesleg.OOP;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class EntityManager {

    private final List<Entity> entityList = new ArrayList<>();
    private final List<Entity> pendingEntities = new ArrayList<>();

    public EntityManager() {
    }

    public void initialise() {
    }

    // Use float to match LibGDX + your MovementManager
    public void update(float deltaTime) {
        // Merge pending entities safely (ownership stays inside EntityManager)
        if (!pendingEntities.isEmpty()) {
            entityList.addAll(pendingEntities);
            pendingEntities.clear();
        }

        /* code for collision */

        // update all entities
        for (Entity e : entityList) {
            e.update(deltaTime);
        }

        // remove dead particles after updating
        int removed = 0;
        Iterator<Entity> iterator = entityList.iterator();
        while(iterator.hasNext()) {
            Entity e = iterator.next();
            if (e instanceof ExplosionParticle && ((ExplosionParticle) e).isDead()) {
                iterator.remove();
                removed++;
            }
        }

        if (removed > 0) {
            System.out.println("Removed " + removed + " dead particles"); // logging
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

    public <T> void forEach(Class<T> type, Consumer<T> action) {
        for (Entity e : entityList) {
            if (type.isInstance(e)) {
                action.accept(type.cast(e));
            }
        }
    }

    /**
     * Render all entities.
     * Encapsulation: Entities draw themselves; GameScene just calls this.
     */
    public void render(SpriteBatch batch) {
        for (Entity e : entityList) {
            e.draw(batch);
        }
    }
}
