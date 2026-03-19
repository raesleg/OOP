package io.github.raesleg.game.entities.vehicles;

import io.github.raesleg.engine.io.ControlSource;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.movement.PlayerMovement;
import io.github.raesleg.game.movement.PlayerMovementModel;

public class TestPlayerCar extends MovableEntity {

    public TestPlayerCar(
            String filename,
            float x, float y,
            float width, float height,
            ControlSource controls,
            PhysicsBody body) {

        super(
                filename,
                x, y,
                width, height,
                controls,
                new PlayerMovementModel(),
                body
        );

        setMovementStrategy(new PlayerMovement());
    }
}