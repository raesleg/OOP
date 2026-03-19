package io.github.raesleg.game.zone;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.IExpirable;
import io.github.raesleg.engine.entity.Shape;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.entities.Pedestrian;

/**
 * CrosswalkZone — A sensor zone spanning the full road width that detects
 * whether the player drives through a pedestrian crossing without braking.
 * <p>
 * The zone has a kinematic Box2D sensor body so the player's dynamic body
 * triggers {@code beginContact} / {@code endContact} callbacks in the
 * {@link io.github.raesleg.engine.collision.CollisionManager}. The game's
 * {@link io.github.raesleg.game.collision.GameCollisionHandler} uses
 * {@code instanceof CrosswalkZone} to detect that the player has entered
 * or exited the crossing area.
 * <p>
 * The zone scrolls vertically with the road — the owning scene calls
 * {@link #updatePosition(float)} every frame with the current scroll offset.
 * <p>
 * <b>Design Pattern:</b> Observer (collision events flow through
 * CollisionManager → ICollisionListener). Facade (physics body created
 * via PhysicsWorld API).
 * <p>
 * <b>Engine/Game Boundary:</b> Extends engine's Shape (ShapeRenderer entity).
 * Lives entirely in the game layer.
 */
public class CrosswalkZone extends Shape implements IExpirable {

    private final PhysicsBody body;
    private final float relativeY;
    private boolean expired;
    private boolean playerInside;
    private boolean violationFired;
    private Pedestrian pairedPedestrian;

    /**
     * Creates a crosswalk sensor zone spanning the full road width.
     * The physics body is created externally and passed in (SRP).
     *
     * @param centreXPx centre X position in pixels (middle of road)
     * @param relativeY Y position relative to scroll offset (world coords)
     * @param widthPx   zone width in pixels (typically full road width)
     * @param heightPx  zone height in pixels (crosswalk stripe depth)
     * @param body      kinematic sensor PhysicsBody for collision detection
     */
    public CrosswalkZone(float centreXPx, float relativeY,
            float widthPx, float heightPx, PhysicsBody body) {
        super(centreXPx - widthPx / 2f, 0, widthPx, heightPx,
                new Color(1f, 1f, 1f, 0.25f));

        this.relativeY = relativeY;
        this.expired = false;
        this.playerInside = false;
        this.violationFired = false;
        this.body = body;

        if (body != null) {
            body.setUserData(this);
        }
    }

    /**
     * Updates the zone's screen position and physics body based on scroll.
     *
     * @param scrollOffset current road scroll offset (pixels)
     */
    public void updatePosition(float scrollOffset) {
        float screenY = relativeY + scrollOffset;
        setY(screenY);

        // Sync kinematic sensor body
        float centreXM = (getX() + getW() / 2f) / Constants.PPM;
        float centreYM = (screenY + getH() / 2f) / Constants.PPM;
        body.setPosition(centreXM, centreYM);

        // Mark expired once scrolled well off-screen below
        if (screenY < -getH() * 3f) {
            expired = true;
        }
    }

    /** Pairs this zone with the pedestrian that crosses it. */
    public void setPedestrian(Pedestrian ped) {
        this.pairedPedestrian = ped;
    }

    /** True when the paired pedestrian is actively crossing the road. */
    public boolean isPedestrianCrossing() {
        return pairedPedestrian != null && pairedPedestrian.isCrossing();
    }

    /** Called by the collision handler when the player car enters. */
    public void setPlayerInside(boolean inside) {
        this.playerInside = inside;
        // Reset violation guard when the player leaves the zone
        if (!inside) {
            this.violationFired = false;
        }
    }

    /**
     * Returns true if a violation has NOT yet been fired for this zone entry.
     * Calling this method also sets the flag, preventing duplicate violations
     * from contact oscillation during kinematic body repositioning.
     */
    public boolean tryFireViolation() {
        if (violationFired)
            return false;
        violationFired = true;
        return true;
    }

    /** True while the player car's physics body overlaps this sensor. */
    public boolean isPlayerInside() {
        return playerInside;
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    public void markExpired() {
        expired = true;
    }

    @Override
    public void draw(ShapeRenderer sr) {
        // Draw semi-transparent white crosswalk stripes
        sr.setColor(getColor());
        float stripeW = 30f;
        float gap = 20f;
        float startX = getX();
        float y = getY();
        float h = getH();
        float endX = getX() + getW();

        for (float sx = startX; sx < endX; sx += stripeW + gap) {
            float w = Math.min(stripeW, endX - sx);
            sr.rect(sx, y, w, h);
        }
    }

    @Override
    public void dispose() {
        if (body != null) {
            body.destroy();
        }
    }
}
