package io.github.raesleg.game.entities.misc;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.IExpirable;
import io.github.raesleg.engine.entity.TextureObject;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.entities.IPerceivable;
import io.github.raesleg.game.entities.PerceptionCategory;

public class Pedestrian extends TextureObject implements IExpirable, IPerceivable {

    private final PhysicsBody body;
    private final float relativeY;
    private boolean expired;

    // render-only state
    private float renderRotation;

    public Pedestrian(float startX, float relativeY, float width, float height, PhysicsBody body) {
        super("pedestrian.png", startX, relativeY, width, height);
        this.body = body;
        this.relativeY = relativeY;
        this.expired = false;
        this.renderRotation = 0f;

        if (body != null) {
            body.setUserData(this);
            body.setLinearDamping(999f);
        }
    }

    public void updateScreenPosition(float scrollOffset) {
        setY(relativeY + scrollOffset);
    }

    public void syncBodyToSprite() {
        if (body == null) {
            return;
        }

        float bodyX = (getX() + getW() / 2f) / Constants.PPM;
        float bodyY = (getY() + getH() / 2f) / Constants.PPM;
        body.setPosition(bodyX, bodyY);
        body.setVelocity(0f, 0f);
    }

    public void syncSpriteFromBody() {
        if (body == null) {
            return;
        }

        setX(body.getPosition().x * Constants.PPM - getW() / 2f);
        setY(body.getPosition().y * Constants.PPM - getH() / 2f);
    }

    public PhysicsBody getPhysicsBody() {
        return body;
    }

    public float getRelativeY() {
        return relativeY;
    }

    public void setRenderRotation(float renderRotation) {
        this.renderRotation = renderRotation;
    }

    public float getRenderRotation() {
        return renderRotation;
    }

    public void resetRenderRotation() {
        this.renderRotation = 0f;
    }

    public void markExpired() {
        expired = true;
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    @Override
    public PerceptionCategory getPerceptionCategory() {
        return PerceptionCategory.PEDESTRIAN;
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (getTexture() == null) {
            return;
        }

        if (Math.abs(renderRotation) < 0.01f) {
            batch.draw(getTexture(), getX(), getY(), getW(), getH());
            return;
        }

        batch.draw(
                getTexture(),
                getX(), getY(),
                getW() / 2f, getH() / 2f,
                getW(), getH(),
                1f, 1f,
                renderRotation,
                0, 0,
                getTexture().getWidth(),
                getTexture().getHeight(),
                false, false);
    }

    @Override
    public void dispose() {
        if (body != null) {
            body.destroy();
        }
    }
}