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

/** NPC vehicle that transitions from preview (scrolling into view) to active (approaching player) */

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

    // Maximum time NPC can exist on screen before forced removal
    private static final float MAX_LIFETIME = 10f;

    // Manage NPC lifecycle: preview phase (scroll only) then active phase (scroll + approach); expire if off-screen
    public void updateLifeCycle(float scrollPixelsPerSecond, float deltaTime, float screenHeight) {
        if (getPhysicsBody() == null || getPhysicsBody().isDestroyed()) {
            expired = true;
            return;
        }

        lifeTimer += deltaTime;

        if (inPreview) {
            // Preview phase: move at scroll speed so NPC gradually scrolls into view from bottom
            float bodyX = getPhysicsBody().getPosition().x;
            float bodyY = getPhysicsBody().getPosition().y;
            getPhysicsBody().setPosition(bodyX, bodyY + (scrollPixelsPerSecond / Constants.PPM) * deltaTime);
            syncSpriteFromBody();
            
            // Exit preview once NPC reaches middle of screen (become fully visible and active)
            float screenY = getY();
            if (screenY >= screenHeight * 0.25f && screenY <= screenHeight * 0.75f) {
                inPreview = false; // Switch to slower active movement once at middle of screen
            }
            return;
        }

        // Active phase: slide down at combined scroll + approach speed (slower than player)
        float totalSpeed = scrollPixelsPerSecond + approachSpeed;
        float bodyX = getPhysicsBody().getPosition().x;
        float bodyY = getPhysicsBody().getPosition().y;
        getPhysicsBody().setPosition(bodyX, bodyY + (totalSpeed / Constants.PPM) * deltaTime);

        syncSpriteFromBody();

        // Mark expired if off-screen or max lifetime exceeded
        if (getY() < -getH() * 2f || lifeTimer > MAX_LIFETIME) {
            expired = true;
        }
    }

    // Sync sprite position and rotation to physics body position (center-to-corner conversion)
    private void syncSpriteFromBody() {
        setX(getPhysicsBody().getPosition().x * Constants.PPM - getW() / 2f);
        setY(getPhysicsBody().getPosition().y * Constants.PPM - getH() / 2f);
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