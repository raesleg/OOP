package io.github.raesleg.OOP;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public abstract class Shape extends Entity {

    /* Private Variables */
    private float width;
    private float height;
    private Color color;

    /* Public Functions */
    public Shape() {
        super();
        this.width = 0;
        this.height = 0;
        this.color = null;
    };

    public Shape(float x, float y, float speed, float w, float h, Color c) {
        this.width = w;
        this.height = h;
        this.color = c;

    };

    public float getWidth() {
        return width;
    };

    public float getHeight() {
        return height;
    };

    public Color getColor() {
        return color;
    };

    @Override
    public void draw(SpriteBatch batch) {
        // Empty implementation - Shape uses ShapeRenderer, not SpriteBatch
    }

    public void draw(ShapeRenderer shape) {
    };

    public void dispose() {
    };
}
