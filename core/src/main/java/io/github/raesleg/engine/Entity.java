package io.github.raesleg.engine;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public abstract class Entity {

    private float x;
    private float y;
    private float w;
    private float h;

    public Entity() {};

    public Entity(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public void update(float deltaTime) {};

    public float getX() { return x; }

    public float getY() { return y; }

    public float getW() { return w; }

    public float getH() { return h; }

    public void setX(float x) { this.x = x; }

    public void setY(float y) { this.y = y; }

    public void setW(float w) { this.w = w; }

    public void setH(float h) { this.h = h; }

    public abstract void draw(SpriteBatch batch);

    public abstract void dispose();
}
