package io.github.raesleg.OOP.lwjgl3;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public abstract class Shape extends Entity {

    /* Private Variables */
    private float width;
    private float height;
    private Color color;

    /* Public Functions */
    public Shape(){};

    public Shape(float x, float y, float speed, float w, float h, Color c) {};

    public float getWidth() {return width;};

    public float getHeight() {return height;};

    public Color getColor() {return color;};

    public void draw(ShapeRenderer shape){};

    public void dispose(){};
}
