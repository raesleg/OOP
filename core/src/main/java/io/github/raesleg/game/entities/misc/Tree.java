package io.github.raesleg.game.entities.misc;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.raesleg.engine.entity.IExpirable;
import io.github.raesleg.engine.entity.TextureObject;
import io.github.raesleg.game.entities.IPerceivable;
import io.github.raesleg.game.entities.PerceptionCategory;

/**
 * Tree — Decorative scrollable entity rendered on the road shoulders.
 * Uses tree1.png or tree2.png. Implements IExpirable for auto-cleanup.
 */
public class Tree extends TextureObject implements IExpirable, IPerceivable {

    private final float relativeY;
    private boolean expired;

    public Tree(String filename, float x, float relativeY, float w, float h) {
        super(filename, x, relativeY, w, h);
        this.relativeY = relativeY;
        this.expired = false;
    }

    public void updatePosition(float scrollOffset) {
        float screenY = relativeY + scrollOffset;
        setY(screenY);
        if (screenY < -getH() * 2f) {
            expired = true;
        }
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    @Override
    public PerceptionCategory getPerceptionCategory() {
        return PerceptionCategory.OBSTACLE;
    }

    public void markExpired() {
        expired = true;
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (getTexture() != null) {
            batch.draw(getTexture(), getX(), getY(), getW(), getH());
        }
    }
}
