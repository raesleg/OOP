package io.github.raesleg.game.entities.misc;

import com.badlogic.gdx.utils.Array;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.entity.TextureObject;
import io.github.raesleg.game.scene.RoadRenderer;

import static io.github.raesleg.engine.scene.Scene.VIRTUAL_HEIGHT;
import static io.github.raesleg.engine.scene.Scene.VIRTUAL_WIDTH;


public class Trees {

    private final Array<Tree> trees;
    private final int count;

    public Trees(int count, EntityManager entityManager) {
        this.count = count;
        this.trees = new Array<>();

        spawnTrees(entityManager);
    }

    private void spawnTrees(EntityManager entityManager) {
        for (int i = 0; i < count; i++) {
            float width = 50f;
            float height = 100f;

            float x;
            if (Math.random() < 0.5f) {
                // left side
                x = (float) Math.random() *
                    (RoadRenderer.ROAD_LEFT - width);
            } else {
                // right side
                x = RoadRenderer.ROAD_RIGHT +
                    (float) Math.random() *
                        (VIRTUAL_WIDTH - RoadRenderer.ROAD_RIGHT - width);
            }

            float y = (float) Math.random() * VIRTUAL_HEIGHT;

            Tree tree = new Tree(x, y, width, height);
            trees.add(tree);
            entityManager.addEntity(tree);
        }
    }

    public void update(float speed, float deltaTime) {
        for (Tree tree : trees) {

            float newY = tree.getY() - speed * 14.0f * deltaTime;

            // Wrap to top
            if (newY + tree.getH() < 0) {
                newY = VIRTUAL_HEIGHT;

                float treeWidth = tree.getW();
                float x;

                if (Math.random() < 0.5f) {
                    // left side
                    x = (float) Math.random() *
                        (RoadRenderer.ROAD_LEFT - treeWidth);
                } else {
                    // right side
                    x = RoadRenderer.ROAD_RIGHT +
                        (float) Math.random() *
                            (VIRTUAL_WIDTH - RoadRenderer.ROAD_RIGHT - treeWidth);
                }

                tree.setX(x);
            }

            tree.setY(newY);
        }
    }

    /* ───────────────────────────── */

    private static class Tree extends TextureObject {

        public Tree(float x, float y, float w, float h) {
            super("tree2.png", x, y, w, h);
        }

        @Override
        public void update(float deltaTime) {
            // Movement handled externally
        }
    }
}
