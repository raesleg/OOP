package io.github.raesleg.game.factory;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.physics.BodyType;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.engine.physics.PhysicsWorld;
import io.github.raesleg.game.GameConstants;
import io.github.raesleg.game.entities.IChaseEntity;
import io.github.raesleg.game.entities.vehicles.PoliceCar;
import io.github.raesleg.game.scene.RoadRenderer;

/**
 * PoliceCarFactory — Creates and registers a PoliceCar entity with its
 * physics body in a single factory call.
 * <p>
 * Extracted from Level2Scene.spawnPolice() to satisfy SRP: the scene
 * should not calculate PPM conversions or create Box2D bodies directly.
 * <p>
 * <b>DIP:</b> Depends on engine abstractions ({@link PhysicsWorld},
 * {@link PhysicsBody}, {@link BodyType}) — never imports Box2D directly.
 * <p>
 * <b>Design Pattern:</b> Factory Method — encapsulates complex object
 * creation behind a simple interface.
 */
public final class PoliceCarFactory {

    private final PhysicsWorld world;
    private final EntityManager entityManager;

    public PoliceCarFactory(PhysicsWorld world, EntityManager entityManager) {
        this.world = world;
        this.entityManager = entityManager;
    }

    /**
     * Creates a police car just below the visible screen, registers it
     * with the entity manager, and returns it as an {@link IChaseEntity}.
     *
     * @return the spawned chase entity
     */
    public IChaseEntity spawn() {
        float centreXPixels = RoadRenderer.ROAD_LEFT + RoadRenderer.ROAD_WIDTH / 2f;
        float centreXMetres = centreXPixels / Constants.PPM;
        float startYPixels = GameConstants.POLICE_START_Y;
        float startYMetres = startYPixels / Constants.PPM;

        PhysicsBody policeBody = world.createBody(
                BodyType.KINEMATIC,
                centreXMetres,
                startYMetres,
                (GameConstants.POLICE_WIDTH / Constants.PPM) / 2f,
                (GameConstants.POLICE_HEIGHT / Constants.PPM) / 2f,
                0f, 0f, true, null);

        PoliceCar car = new PoliceCar(policeBody, centreXPixels, startYPixels);
        entityManager.addEntity(car);
        return car;
    }
}
