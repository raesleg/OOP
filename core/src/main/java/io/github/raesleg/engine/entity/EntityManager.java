package io.github.raesleg.engine.entity;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class EntityManager {

    private List<Entity> entityList = new ArrayList<>();
    private List<Entity> pendingEntities = new ArrayList<>();

    public void update(float deltaTime) {
        if (!pendingEntities.isEmpty()) {
            entityList.addAll(pendingEntities);
            pendingEntities.clear();
        }

        List<Entity> snap = getSnapshot();
        for (Entity e : snap) {
            e.update(deltaTime);
        }

        // clear dead entities
        Iterator<Entity> it = entityList.iterator();
        while (it.hasNext()) {
            Entity e = it.next();
            if (e instanceof IExpirable exp && exp.isExpired()) {
                it.remove();
            }
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

    public List<Entity> getSnapshot() {  //new method for safe copying
        return new ArrayList<>(entityList);
    }

    public <T> void forEach(Class<T> type, Consumer<T> action) {
        for (Entity e : getSnapshot()) { //getSnapshot
            if (type.isInstance(e)) {
                action.accept(type.cast(e));
            }
        }
    }

    public void render(SpriteBatch batch) {
        for (Entity e : entityList) {
            e.draw(batch);
        }
    }

    public void dispose() {
        for (Entity e : entityList) {
            e.dispose();
        }
        for (Entity e : pendingEntities) {
            e.dispose();
        }
        entityList.clear();
        pendingEntities.clear();
    }
}
