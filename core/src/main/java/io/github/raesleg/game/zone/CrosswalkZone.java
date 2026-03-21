package io.github.raesleg.game.zone;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.IExpirable;
import io.github.raesleg.engine.entity.Shape;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.state.CrosswalkZoneState;

public class CrosswalkZone extends Shape implements IExpirable {

    private final PhysicsBody body;
    private final float relativeY;
    private final CrosswalkZoneState state;

    public CrosswalkZone(float centreXPx, float relativeY,
                         float widthPx, float heightPx, PhysicsBody body) {
        super(centreXPx - widthPx / 2f, 0, widthPx, heightPx,
                new Color(1f, 1f, 1f, 0.25f));

        this.relativeY = relativeY;
        this.body = body;
        this.state = new CrosswalkZoneState();

        if (body != null) {
            body.setUserData(this);
        }
    }

    public void updatePosition(float scrollOffset) {
        float screenY = relativeY + scrollOffset;
        setY(screenY);

        syncPhysicsBody(screenY);
        updateExpiry(screenY);
    }

    private void syncPhysicsBody(float screenY) {
        if (body == null) {
            return;
        }

        float centreXM = (getX() + getW() / 2f) / Constants.PPM;
        float centreYM = (screenY + getH() / 2f) / Constants.PPM;
        body.setPosition(centreXM, centreYM);
    }

    private void updateExpiry(float screenY) {
        if (screenY < -getH() * 3f) {
            state.markExpired();
        }
    }

    public void setPlayerInside(boolean inside) {
        state.setPlayerInside(inside);
    }

    public boolean tryFireViolation() {
        return state.tryFireViolation();
    }

    public boolean isPlayerInside() {
        return state.isPlayerInside();
    }

    public void setCrossingActive(boolean crossingActive) {
        state.setCrossingActive(crossingActive);
    }

    public boolean isPedestrianCrossing() {
        return state.isPedestrianCrossing();
    }

    @Override
    public boolean isExpired() {
        return state.isExpired();
    }

    public void markExpired() {
        state.markExpired();
    }

    @Override
    public void draw(ShapeRenderer sr) {
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

    public PhysicsBody getPhysicsBody() {
        return body;
    }

    public float getRelativeY() {
        return relativeY;
    }

    public CrosswalkZoneState getState() {
        return state;
    }
}