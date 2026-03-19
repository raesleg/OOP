package io.github.raesleg.game.scene;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
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

    /* ── Road geometry (pixels, 1280×720 virtual coords) ── */
    public static final float ROAD_LEFT = 340f;
    public static final float ROAD_RIGHT = 940f;
    public static final float ROAD_WIDTH = ROAD_RIGHT - ROAD_LEFT; // 600
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

    private final float screenWidth;
    private final float screenHeight;

    public RoadRenderer(float screenWidth, float screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    /**
     * Draws the full road background (grass → asphalt → shoulders → lane dashes).
     *
     * @param sr           an un-begun ShapeRenderer (this method calls begin/end)
     * @param scrollOffset cumulative downward scroll in pixels — drives the
     *                     animated dashes so the road appears to move
     */
    public void draw(ShapeRenderer sr, float scrollOffset) {
        sr.begin(ShapeRenderer.ShapeType.Filled);

        // ── Grass (left & right of road) ──
        sr.setColor(GRASS_COLOR);
        sr.rect(0, 0, ROAD_LEFT, screenHeight);
        sr.rect(ROAD_RIGHT, 0, screenWidth - ROAD_RIGHT, screenHeight);

        // ── Asphalt ──
        sr.setColor(ROAD_COLOR);
        sr.rect(ROAD_LEFT, 0, ROAD_WIDTH, screenHeight);

        // ── Road shoulders (thin edge strips) ──
        float shoulderW = 6f;
        sr.setColor(SHOULDER_COLOR);
        sr.rect(ROAD_LEFT, 0, shoulderW, screenHeight);
        sr.rect(ROAD_RIGHT - shoulderW, 0, shoulderW, screenHeight);

        // ── Solid edge lines ──
        float edgeW = 3f;
        sr.setColor(EDGE_COLOR);
        sr.rect(ROAD_LEFT + shoulderW, 0, edgeW, screenHeight);
        sr.rect(ROAD_RIGHT - shoulderW - edgeW, 0, edgeW, screenHeight);

        // ── Scrolling lane divider dashes ──
        sr.setColor(DASH_COLOR);
        float cycle = DASH_LENGTH + DASH_GAP;
        float offset = scrollOffset % cycle;

        for (int lane = 1; lane < LANE_COUNT; lane++) {
            float dividerX = ROAD_LEFT + lane * LANE_WIDTH - DASH_WIDTH / 2f;

            for (float y = -DASH_LENGTH + offset; y < screenHeight + cycle; y += cycle) {
                sr.rect(dividerX, y, DASH_WIDTH, DASH_LENGTH);
            }
        }

        sr.end();
    }
}
