package io.github.raesleg.game.entities.vehicles;

import io.github.raesleg.engine.entity.IExpirable;
import io.github.raesleg.engine.io.ControlSource;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.movement.MovementModel;
import io.github.raesleg.engine.movement.MovementStrategy;
import io.github.raesleg.engine.physics.PhysicsBody;

import io.github.raesleg.game.entities.IPerceivable;
import io.github.raesleg.game.entities.PerceptionCategory;
import io.github.raesleg.game.movement.SensorComponent;

/**
 * NPC vehicle that transitions from preview (scrolling into view) to active
 * (approaching player)
 */

public class NPCCar extends MovableEntity implements IExpirable, IPerceivable {

    private final int laneIndex; // Which of 3 lanes (0-2) NPC occupies
    private final SensorComponent sensor; // AI perception system for collision avoidance
    private final float approachSpeed; // Relative velocity (px/s) after preview phase
    private boolean expired; // Marked for removal when off-screen or lifetime exceeded
    private boolean inPreview; // True: scrolling into view only; False: approaching player
    private float lifeTimer; // Total lifetime elapsed

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
        this.lifeTimer = 0f;
    }

    /**
     * Increments the internal life timer. Called by
     * {@link io.github.raesleg.game.factory.NPCLifecycleManager}.
     */
    public void tickLifeTimer(float deltaTime) {
        lifeTimer += deltaTime;
    }

    /** Returns the total elapsed lifetime in seconds. */
    public float getLifeTimer() {
        return lifeTimer;
    }

    /** Whether the NPC is still in preview (scrolling into view). */
    public boolean isInPreview() {
        return inPreview;
    }

    /** Transitions the NPC from preview phase to active phase. */
    public void exitPreview() {
        inPreview = false;
    }

    /** Returns the configured approach speed (px/s relative to scroll). */
    public float getApproachSpeed() {
        return approachSpeed;
    }

    /**
     * Overrides MovableEntity.move() so that the NPCLifecycleManager
     * retains full authority over this car's velocity. Without this
     * override the CarMovementModel would overwrite the lifecycle
     * velocity every frame before the physics step, preventing
     * the NPC from scrolling into view.
     */
    @Override
    public void move(float dt) {
        // Velocity is set by NPCLifecycleManager — skip model/strategy step
    }

    @Override
    // Check if NPC has left the level or exceeded max lifetime
    public boolean isExpired() {
        return expired;
    }

    @Override
    // All NPCs are perceived as vehicles by the AI/collision system
    public PerceptionCategory getPerceptionCategory() {
        return PerceptionCategory.VEHICLE;
    }

    // Manual expiration trigger (e.g. on level end)
    public void markExpired() {
        expired = true;
    }

    // Which road lane NPC currently occupies for spawn/collision logic
    public int getLaneIndex() {
        return laneIndex;
    }

    // AI perception system for detecting obstacles, pedestrians, other vehicles
    public SensorComponent getSensor() {
        return sensor;
    }

    @Override
    // Release physics body resources when NPC is destroyed
    public void dispose() {
        if (getPhysicsBody() != null) {
            getPhysicsBody().destroy();
        }
    }
}