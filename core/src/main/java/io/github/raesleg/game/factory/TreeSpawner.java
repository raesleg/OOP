package io.github.raesleg.game.factory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.game.entities.misc.Tree;
import io.github.raesleg.game.scene.RoadRenderer;

/**
 * TreeSpawner — Periodically spawns decorative trees on both
 * road shoulders (left of ROAD_LEFT, right of ROAD_RIGHT).
 */
public class TreeSpawner {

    private final EntityManager entityManager;
    private final float screenHeight;

    private float spawnTimer;
    private final float spawnInterval;
    private final List<Tree> activeTrees = new ArrayList<>();

    private static final float TREE_W = 60f;
    private static final float TREE_H = 80f;
    private static final float MIN_Y_SPACING = 250f;
    private static final String[] TEXTURES = { "tree1.png", "tree2.png" };

    public TreeSpawner(EntityManager entityManager, float screenHeight,
            float spawnInterval) {
        this.entityManager = entityManager;
        this.screenHeight = screenHeight;
        this.spawnInterval = spawnInterval;
        this.spawnTimer = 0f;
    }

    public void update(float deltaTime, float scrollOffset) {
        spawnTimer += deltaTime;
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f;
            spawnTree(scrollOffset);
        }
        Iterator<Tree> it = activeTrees.iterator();
        while (it.hasNext()) {
            Tree t = it.next();
            if (t.isExpired()) {
                it.remove();
                continue;
            }
            t.updatePosition(scrollOffset);
        }
    }

    private void spawnTree(float scrollOffset) {
        float relativeY = -scrollOffset + screenHeight + 300f;

        // Enforce minimum Y spacing between trees to avoid clustering
        for (Tree t : activeTrees) {
            if (Math.abs(t.getY() - relativeY) < MIN_Y_SPACING) {
                return;
            }
        }

        String tex = TEXTURES[(int) (Math.random() * TEXTURES.length)];

        // Left shoulder tree
        float leftX = (float) (Math.random() * (RoadRenderer.ROAD_LEFT - TREE_W - 20f)) + 10f;
        Tree leftTree = new Tree(tex, leftX, relativeY, TREE_W, TREE_H);
        entityManager.addEntity(leftTree);
        activeTrees.add(leftTree);

        // Right shoulder tree (sometimes)
        if (Math.random() > 0.4) {
            String tex2 = TEXTURES[(int) (Math.random() * TEXTURES.length)];
            float rightX = RoadRenderer.ROAD_RIGHT + 20f
                    + (float) (Math.random() * (1280f - RoadRenderer.ROAD_RIGHT - TREE_W - 30f));
            Tree rightTree = new Tree(tex2, rightX, relativeY, TREE_W, TREE_H);
            entityManager.addEntity(rightTree);
            activeTrees.add(rightTree);
        }
    }

    public void clearAll() {
        for (Tree t : activeTrees)
            t.markExpired();
        activeTrees.clear();
    }
}
