package io.github.raesleg.game.entities.vehicles;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.IExpirable;
import io.github.raesleg.engine.io.ControlSource;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.movement.MovementModel;
import io.github.raesleg.engine.movement.MovementStrategy;
import io.github.raesleg.engine.physics.PhysicsBody;

import io.github.raesleg.game.entities.IPerceivable;
import io.github.raesleg.game.entities.PerceptionCategory;
import io.github.raesleg.game.movement.SensorComponent;

public class NPCCar extends MovableEntity implements IExpirable, IPerceivable {

    private final int laneIndex;
    private final SensorComponent sensor;
    private boolean expired;

    public NPCCar(String filename,
            float x, float y,
            float w, float h,
            int laneIndex,
            ControlSource controls,
            MovementStrategy strategy,
            MovementModel movementModel,
            PhysicsBody body,
            SensorComponent sensor) {
        super(filename, x, y, w, h, controls, movementModel, body);
        setMovementStrategy(strategy);
        this.laneIndex = laneIndex;
        this.sensor = sensor;
        this.expired = false;
    }

    public void updateLifeCycle(float scrollPixelsPerSecond, float deltaTime, float screenHeight) {
        if (getPhysicsBody() == null) {
            return;
        }

        // Move NPC body down the screen at the same rate as the world scroll
        float bodyX = getPhysicsBody().getPosition().x;
        float bodyY = getPhysicsBody().getPosition().y;
        float scrollMetersPerSecond = scrollPixelsPerSecond / Constants.PPM;
        getPhysicsBody().setPosition(bodyX, bodyY - scrollMetersPerSecond * deltaTime);

        // Sync sprite position from physics body
        float newY = getPhysicsBody().getPosition().y * Constants.PPM - getH() / 2f;
        setX(getPhysicsBody().getPosition().x * Constants.PPM - getW() / 2f);
        setY(newY);

        // Expire when scrolled off the bottom of the screen
        if (newY < -getH() * 2f) {
            expired = true;
        }
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    @Override
    public PerceptionCategory getPerceptionCategory() {
        return PerceptionCategory.VEHICLE;
    }

    public void markExpired() {
        expired = true;
    }

    public int getLaneIndex() {
        return laneIndex;
    }

    public SensorComponent getSensor() {
        return sensor;
    }

    @Override
    public void dispose() {
        if (getPhysicsBody() != null) {
            getPhysicsBody().destroy();
        }
    }
}