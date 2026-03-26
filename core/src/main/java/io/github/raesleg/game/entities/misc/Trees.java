package io.github.raesleg.game.entities.misc;

import com.badlogic.gdx.utils.Array;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.entity.TextureObject;
import io.github.raesleg.game.GameConstants;
import io.github.raesleg.game.scene.RoadRenderer;

import static io.github.raesleg.engine.scene.Scene.VIRTUAL_HEIGHT;
import static io.github.raesleg.engine.scene.Scene.VIRTUAL_WIDTH;

public class Trees {

    /**
     * Extra vertical padding so trees spawn/despawn outside the zoomed camera view.
     * Derived from CAMERA_ZOOM and CAMERA_LOOK_AHEAD to avoid pop-in.
     */
    private static final float SPAWN_PADDING = VIRTUAL_HEIGHT
            * (Math.max(GameConstants.CAMERA_ZOOM, GameConstants.L2_CAMERA_ZOOM) - 1f)
            + GameConstants.CAMERA_LOOK_AHEAD + 50f;

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

            float y = -SPAWN_PADDING + (float) Math.random() * (VIRTUAL_HEIGHT + 2 * SPAWN_PADDING);

            Tree tree = new Tree(x, y, width, height);
            trees.add(tree);
            entityManager.addEntity(tree);
        }
    }

    public void update(float speed, float deltaTime) {
        for (Tree tree : trees) {

            float newY = tree.getY() - speed * 14.0f * deltaTime;

            // Wrap to top when tree scrolls below visible area
            if (newY + tree.getH() < -SPAWN_PADDING) {
                newY = VIRTUAL_HEIGHT + SPAWN_PADDING;

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

            super(Math.random() < 0.5 ? "tree1.png" : "tree2.png", x, y, w, h);
        }

        @Override
        public void update(float deltaTime) {
            // Movement handled externally
        }
    }
}
