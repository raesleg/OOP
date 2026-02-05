package io.github.raesleg.OOP;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public abstract class Entity {

    /* Private Variables */
    private float x;
    private float y;

    /* Public Functions */
    public Entity() {
    };

    public Entity(float curr_x, float curr_y) {
        x = curr_x;
        y = curr_y;
    }

    public void update(float deltaTime) {
    };

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public void setX(float newX) {
        x = newX;
    }

    public void setY(float newY) {
        y = newY;
    }

    public abstract void draw(SpriteBatch batch);


    public abstract void dispose();
}
