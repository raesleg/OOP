package io.github.raesleg.OOP.lwjgl3;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public abstract class Entity implements IMovable{

    /* Private Variables */
    private float x;
    private float y;
    private float speed;

    /* Public Functions */
    public Entity() {};

    public Entity(float curr_x, float curr_y, float curr_speed) {
        x = curr_x;
        y = curr_y;
        speed = curr_speed;
    }

    public void update(double deltaTime) {};

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getSpeed() {
        return speed;
    }

    public void setX(float newX) {
        x = newX;
    }

    public void setY(float newY) {
        y = newY;
    }

    public void setSpeed(float newSpeed) {
        speed = newSpeed;
    }

    public void draw(SpriteBatch batch) {};

    public void draw(ShapeRenderer shape) {};

    public void dispose() {};

}
