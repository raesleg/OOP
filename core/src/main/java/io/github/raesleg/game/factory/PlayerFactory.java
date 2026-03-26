package io.github.raesleg.game.factory;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.io.ControlSource;
import io.github.raesleg.engine.movement.MovementStrategy;
import io.github.raesleg.engine.physics.BodyType;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.engine.physics.PhysicsWorld;

import io.github.raesleg.game.GameConstants;
import io.github.raesleg.game.entities.vehicles.PlayerCar;
import io.github.raesleg.game.movement.CarMovementModel;
import io.github.raesleg.game.movement.VehicleProfile;
import io.github.raesleg.game.scene.RoadRenderer;

// Creates player car entity with physics body and registers it with the entity manager.
public final class PlayerFactory {

    private PlayerFactory() {}

    /*
        Creates a player car centred on the road at the default start position,
        registers it with the entity manager, and returns the entity.

        @param world         physics world for body creation
        @param entityManager entity manager to register the car in
        @param controls      user input control source
        @return the spawned PlayerCar
     */
    public static PlayerCar create(PhysicsWorld world, EntityManager entityManager,
            ControlSource controls, MovementStrategy movementStrategy) {
        float carPixelX = RoadRenderer.ROAD_LEFT + RoadRenderer.ROAD_WIDTH / 2f;
        float carPixelY = GameConstants.PLAYER_CAR_START_Y;
        float carW = GameConstants.PLAYER_CAR_WIDTH;
        float carH = GameConstants.PLAYER_CAR_HEIGHT;

        PhysicsBody carBody = world.createBody(
                BodyType.DYNAMIC,
                carPixelX / Constants.PPM,
                (carPixelY + carH / 2f) / Constants.PPM,
                (carW / Constants.PPM) / 2f * 0.65f,
                (carH / Constants.PPM) / 2f * 0.75f,
                1f, 0.3f, false, null);
        carBody.setBullet(true);

        PlayerCar car = new PlayerCar(
                "car.png",
                carPixelX - carW / 2f, carPixelY,
                carW, carH,
                controls,
                movementStrategy,
                new CarMovementModel(VehicleProfile.playerArcade()),
                carBody);

        entityManager.addEntity(car);
        return car;
    }
}
