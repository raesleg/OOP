package io.github.raesleg.game.factory;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.physics.BodyType;
import io.github.raesleg.engine.physics.PhysicsWorld;
import io.github.raesleg.game.GameConstants;
import io.github.raesleg.game.scene.RoadRenderer;

/**
 * BoundaryFactory — Creates the four static boundary walls that keep
 * entities on the road.
 * <p>
 * Extracted from {@code BaseGameScene.show()} to satisfy SRP: the
 * scene should not calculate PPM conversions or create Box2D bodies
 * directly.
 * <p>
 * <b>DIP:</b> Depends on engine abstractions ({@link PhysicsWorld},
 * {@link BodyType}) — never imports Box2D directly.
 */
public final class BoundaryFactory {

    private BoundaryFactory() {
    }

    /**
     * Creates two side walls and top/bottom walls around the road.
     *
     * @param world  the physics world to create bodies in
     * @param worldW world width in metres
     * @param worldH world height in metres
     */
    public static void createBoundaries(PhysicsWorld world, float worldW, float worldH) {
        float t = GameConstants.BOUNDARY_WALL_THICKNESS;
        float roadLeftM = RoadRenderer.ROAD_LEFT / Constants.PPM;
        float roadRightM = RoadRenderer.ROAD_RIGHT / Constants.PPM;

        // Left wall
        world.createBody(BodyType.STATIC,
                roadLeftM - t / 2f, worldH / 2f,
                t / 2f, worldH / 2f, 0, 0.4f, false, null);
        // Right wall
        world.createBody(BodyType.STATIC,
                roadRightM + t / 2f, worldH / 2f,
                t / 2f, worldH / 2f, 0, 0.4f, false, null);
        // Bottom wall
        world.createBody(BodyType.STATIC,
                worldW / 2f, -t / 2f,
                worldW / 2f, t / 2f, 0, 0.4f, false, null);
        // Top wall
        world.createBody(BodyType.STATIC,
                worldW / 2f, worldH + t / 2f,
                worldW / 2f, t / 2f, 0, 0.4f, false, null);
    }
}
