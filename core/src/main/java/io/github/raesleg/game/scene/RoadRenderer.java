package io.github.raesleg.game.scene;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import io.github.raesleg.game.GameConstants;

/**
 * RoadRenderer — Draws the lane-dodger road background using ShapeRenderer.
 * <p>
 * Purely visual: green grass shoulders, dark-grey asphalt, white dashed
 * lane dividers that scroll downward based on a {@code scrollOffset}
 * supplied each frame by the owning scene.
 * <p>
 * Road geometry constants are package-visible so {@link GameScene} can
 * position physics walls and entities relative to the road.
 */
public class RoadRenderer {

    /* ── Road geometry — delegated to GameConstants ── */
    public static final float ROAD_LEFT = GameConstants.ROAD_LEFT;
    public static final float ROAD_RIGHT = GameConstants.ROAD_RIGHT;
    public static final float ROAD_WIDTH = GameConstants.ROAD_WIDTH;
    static final int LANE_COUNT = 3;
    static final float LANE_WIDTH = ROAD_WIDTH / LANE_COUNT; // 200

    /* ── Lane-dash styling ── */
    private static final float DASH_LENGTH = 40f;
    private static final float DASH_GAP = 30f;
    private static final float DASH_WIDTH = 4f;

    /* ── Colours ── */
    private static final Color GRASS_COLOR = new Color(0.18f, 0.45f, 0.18f, 1f);
    private static final Color ROAD_COLOR = new Color(0.22f, 0.22f, 0.25f, 1f);
    private static final Color SHOULDER_COLOR = new Color(0.35f, 0.35f, 0.30f, 1f);
    private static final Color EDGE_COLOR = Color.WHITE;
    private static final Color DASH_COLOR = Color.WHITE;

    /**
     * Draws the full road background (grass → asphalt → shoulders → lane dashes)
     * covering the entire visible camera region.
     *
     * @param sr           an un-begun ShapeRenderer (this method calls begin/end)
     * @param scrollOffset cumulative downward scroll in pixels — drives the
     *                     animated dashes so the road appears to move
     * @param visMinX      left edge of the visible area (world coords)
     * @param visMinY      bottom edge of the visible area (world coords)
     * @param visMaxX      right edge of the visible area (world coords)
     * @param visMaxY      top edge of the visible area (world coords)
     */
    public void draw(ShapeRenderer sr, float scrollOffset,
            float visMinX, float visMinY, float visMaxX, float visMaxY) {
        float visH = visMaxY - visMinY;

        sr.begin(ShapeRenderer.ShapeType.Filled);

        // ── Grass (fills entire visible area outside road) ──
        sr.setColor(GRASS_COLOR);
        sr.rect(visMinX, visMinY, ROAD_LEFT - visMinX, visH);
        sr.rect(ROAD_RIGHT, visMinY, visMaxX - ROAD_RIGHT, visH);

        // ── Asphalt ──
        sr.setColor(ROAD_COLOR);
        sr.rect(ROAD_LEFT, visMinY, ROAD_WIDTH, visH);

        // ── Road shoulders (thin edge strips) ──
        float shoulderW = 6f;
        sr.setColor(SHOULDER_COLOR);
        sr.rect(ROAD_LEFT, visMinY, shoulderW, visH);
        sr.rect(ROAD_RIGHT - shoulderW, visMinY, shoulderW, visH);

        // ── Solid edge lines ──
        float edgeW = 3f;
        sr.setColor(EDGE_COLOR);
        sr.rect(ROAD_LEFT + shoulderW, visMinY, edgeW, visH);
        sr.rect(ROAD_RIGHT - shoulderW - edgeW, visMinY, edgeW, visH);

        // ── Scrolling lane divider dashes ──
        sr.setColor(DASH_COLOR);
        float cycle = DASH_LENGTH + DASH_GAP;
        float offset = scrollOffset % cycle;

        for (int lane = 1; lane < LANE_COUNT; lane++) {
            float dividerX = ROAD_LEFT + lane * LANE_WIDTH - DASH_WIDTH / 2f;

            for (float y = visMinY - DASH_LENGTH + offset; y < visMaxY + cycle; y += cycle) {
                sr.rect(dividerX, y, DASH_WIDTH, DASH_LENGTH);
            }
        }

        sr.end();
    }
}
