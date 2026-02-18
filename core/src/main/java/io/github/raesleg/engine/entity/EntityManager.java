package io.github.raesleg.engine.entity;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class EntityManager {

    private List<Entity> entityList = new ArrayList<>();
    private List<Entity> pendingEntities = new ArrayList<>();

    /* ── Cached snapshot (avoids allocating a new ArrayList every call) ── */
    private List<Entity> cachedSnapshot;
    private boolean snapshotDirty = true;

    public void update(float deltaTime) {
        if (!pendingEntities.isEmpty()) {
            entityList.addAll(pendingEntities);
            pendingEntities.clear();
            snapshotDirty = true;
        }

        List<Entity> snap = getSnapshot();
        for (Entity e : snap) {
            e.update(deltaTime);
        }

        // clear dead entities — dispose them first to free GPU resources
        Iterator<Entity> it = entityList.iterator();
        while (it.hasNext()) {
            Entity e = it.next();
            if (e instanceof IExpirable exp && exp.isExpired()) {
                e.dispose();
                it.remove();
                snapshotDirty = true;
            }
        }
    }

    public void addEntity(Entity entity) {
        if (entity != null) {
            pendingEntities.add(entity);
            // snapshot stays valid until pendingEntities are flushed in update()
        }
    }

    /**
     * Returns a read-only view of the current entity list.
     * The backing copy is only rebuilt when the list has actually changed
     * (dirty-flag caching), eliminating thousands of short-lived ArrayList
     * allocations per second and the resulting GC pressure.
     */
    public List<Entity> getSnapshot() {
        if (snapshotDirty || cachedSnapshot == null) {
            cachedSnapshot = Collections.unmodifiableList(new ArrayList<>(entityList));
            snapshotDirty = false;
        }
        return cachedSnapshot;
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
        cachedSnapshot = null;
        snapshotDirty = true;
    }
}
