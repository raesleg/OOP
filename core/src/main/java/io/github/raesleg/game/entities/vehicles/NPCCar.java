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
    private final float approachSpeed;
    private boolean expired;
    private boolean inPreview;
    private float previewTimer;
    private float lifeTimer;

    public NPCCar(String filename,
            float x, float y,
            float w, float h,
            int laneIndex,
            float approachSpeed,
            ControlSource controls,
            MovementStrategy strategy,
            MovementModel movementModel,
            PhysicsBody body,
            SensorComponent sensor) {
        super(filename, x, y, w, h, controls, movementModel, body);
        setMovementStrategy(strategy);
        this.laneIndex = laneIndex;
        this.sensor = sensor;
        this.approachSpeed = approachSpeed;
        this.expired = false;
        this.inPreview = true;
        this.previewTimer = 0f;
        this.lifeTimer = 0f;
    }

    private static final float MAX_LIFETIME = 10f;

    public void updateLifeCycle(float scrollPixelsPerSecond, float deltaTime, float screenHeight) {
        if (getPhysicsBody() == null || getPhysicsBody().isDestroyed()) {
            expired = true;
            return;
        }

        lifeTimer += deltaTime;

        if (inPreview) {
            // During preview: move at scroll speed so NPC scrolls into view from top
            // From player's perspective it appears to move down the screen toward them
            float bodyX = getPhysicsBody().getPosition().x;
            float bodyY = getPhysicsBody().getPosition().y;
            getPhysicsBody().setPosition(bodyX, bodyY + (scrollPixelsPerSecond / Constants.PPM) * deltaTime);
            syncSpriteFromBody();
            
            // End preview once NPC reaches middle of screen
            float screenY = getY();
            if (screenY >= screenHeight * 0.25f && screenY <= screenHeight * 0.75f) {
                inPreview = false; // Switch to slower active movement once at middle of screen
            }
            return;
        }

        // After preview: slide down at scroll + approach speed
        float totalSpeed = scrollPixelsPerSecond + approachSpeed;
        float bodyX = getPhysicsBody().getPosition().x;
        float bodyY = getPhysicsBody().getPosition().y;
        getPhysicsBody().setPosition(bodyX, bodyY + (totalSpeed / Constants.PPM) * deltaTime);

        syncSpriteFromBody();

        if (getY() < -getH() * 2f || lifeTimer > MAX_LIFETIME) {
            expired = true;
        }
    }

    private void syncSpriteFromBody() {
        setX(getPhysicsBody().getPosition().x * Constants.PPM - getW() / 2f);
        setY(getPhysicsBody().getPosition().y * Constants.PPM - getH() / 2f);
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