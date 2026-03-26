package io.github.raesleg.game.movement;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.entities.misc.Pedestrian;
import io.github.raesleg.game.scene.RoadRenderer;

// Movement state and logic for pedestrians crossing the road
// Controlled by the scene, not the entity
// Scene can create different movement instances (e.g. for jaywalking) without modifying the Pedestrian class
public class PedestrianMovement {

    public enum CrossingState {
        WAITING,
        CROSSING,
        SUCCESS,
        FAILED
    }

    private final float walkingSpeed;
    private final float direction; // -1 or +1, set at intent creation
    private final float crossedFromLeft;
    private final float crossedFromRight;

    private CrossingState state;

    public PedestrianMovement(float walkingSpeed, float spriteWidth, float direction) {
        this.walkingSpeed = walkingSpeed;
        this.direction = direction < 0f ? -1f : 1f;
        this.crossedFromLeft = RoadRenderer.ROAD_LEFT - spriteWidth * 2f;
        this.crossedFromRight = RoadRenderer.ROAD_RIGHT + spriteWidth * 2f;
        this.state = CrossingState.WAITING;
    }

    public void activate() {
        if (state == CrossingState.WAITING) {
            state = CrossingState.CROSSING;
        }
    }

    public void deactivate() {
        if (state == CrossingState.CROSSING) {
            state = CrossingState.WAITING;
        }
    }

    public void update(Pedestrian pedestrian, float deltaTime) {
        if (pedestrian == null || state != CrossingState.CROSSING) {
            return;
        }

        float nextX = pedestrian.getX() + direction * walkingSpeed * deltaTime;
        pedestrian.setX(nextX);

        PhysicsBody body = pedestrian.getPhysicsBody();
        if (body != null) {
            float bodyX = (nextX + pedestrian.getW() / 2f) / Constants.PPM;
            float bodyY = (pedestrian.getY() + pedestrian.getH() / 2f) / Constants.PPM;
            body.setPosition(bodyX, bodyY);
            body.setVelocity(0f, 0f);
        }
    }

    public boolean hasReachedFinish(Pedestrian pedestrian) {
        if (pedestrian == null) {
            return false;
        }

        return pedestrian.getX() < crossedFromLeft || pedestrian.getX() > crossedFromRight;
    }

    public void markFinishedSuccessfully() {
        state = CrossingState.SUCCESS;
    }

    public void markFinishedUnsuccessfully() {
        state = CrossingState.FAILED;
    }

    public boolean isActive() {
        return state == CrossingState.CROSSING;
    }

    public boolean isFinished() {
        return state == CrossingState.SUCCESS || state == CrossingState.FAILED;
    }

    public boolean hasCrossedSuccessfully() {
        return state == CrossingState.SUCCESS;
    }

    public CrossingState getState() {
        return state;
    }

    public float getDirection() {
        return direction;
    }
}