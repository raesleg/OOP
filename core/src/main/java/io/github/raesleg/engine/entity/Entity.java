package io.github.raesleg.engine.entity;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public abstract class Entity {

    private float x;
    private float y;
    private float w;
    private float h;

    public Entity(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public void update(float deltaTime) {
    };

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getW() {
        return w;
    }

    public float getH() {
        return h;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setW(float w) {
        this.w = w;
    }

    public void setH(float h) {
        this.h = h;
    }

    /**
     * Renders this entity. Default is no-op; subclasses override as needed.
     * Non-abstract so entities that use a different renderer (e.g. ShapeRenderer)
     * are not forced into a meaningless empty override (Liskov Substitution).
     */
    public void draw(SpriteBatch batch) {
    }

    /**
     * Disposes resources owned by this entity. Default is no-op.
     */
    public void dispose() {
    }
}
