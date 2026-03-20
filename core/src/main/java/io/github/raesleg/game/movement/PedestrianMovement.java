package io.github.raesleg.game.movement;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.entities.Pedestrian;
import io.github.raesleg.game.scene.RoadRenderer;

/**
 * Scene-owned pedestrian movement state and execution.
 * Not part of the entity itself.
 */
public class PedestrianMovement {

    private final float walkingSpeed;
    private final float leftFinishX;
    private final float rightFinishX;

    private boolean active;
    private boolean finished;
    private boolean crossedSuccessfully;

    public PedestrianMovement(float walkingSpeed, float spriteWidth) {
        this.walkingSpeed = walkingSpeed;
        this.leftFinishX = RoadRenderer.ROAD_LEFT - spriteWidth * 2f;
        this.rightFinishX = RoadRenderer.ROAD_RIGHT + spriteWidth * 2f;
        this.active = false;
        this.finished = false;
        this.crossedSuccessfully = false;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void update(Pedestrian pedestrian, PedestrianIntent intent, float deltaTime) {
        if (pedestrian == null || intent == null || !active || finished) {
            return;
        }

        float nextX = pedestrian.getX() + intent.getDirection() * walkingSpeed * deltaTime;
        pedestrian.setX(nextX);

        PhysicsBody body = pedestrian.getPhysicsBody();
        if (body != null) {
            float bodyX = (nextX + pedestrian.getW() / 2f) / Constants.PPM;
            float bodyY = (pedestrian.getY() + pedestrian.getH() / 2f) / Constants.PPM;
            body.setPosition(bodyX, bodyY);
        }
    }

    public boolean hasReachedFinish(Pedestrian pedestrian) {
        if (pedestrian == null) {
            return false;
        }

        return pedestrian.getX() < leftFinishX || pedestrian.getX() > rightFinishX;
    }

    public void markFinishedSuccessfully() {
        this.finished = true;
        this.crossedSuccessfully = true;
        this.active = false;
    }

    public void markFinishedUnsuccessfully() {
        this.finished = true;
        this.crossedSuccessfully = false;
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean hasCrossedSuccessfully() {
        return crossedSuccessfully;
    }
}