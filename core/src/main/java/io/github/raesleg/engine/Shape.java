package io.github.raesleg.engine;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public abstract class Shape extends Entity {

    private float width;
    private float height;
    private Color color;

    public Shape() {
        super(0,0);
        this.width = 0;
        this.height = 0;
        this.color = null;
    };

    public Shape(float x, float y, float w, float h, Color c) {
        super(x,y);
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

    public void setSize(float w, float h) { 
        this.width = w; 
        this.height = h; 
    }

    public void setColor(Color c) { 
        this.color = c; 
    }

    @Override
    public void draw(SpriteBatch batch) {}

    public void draw(ShapeRenderer shape) {};

    public void dispose() {};
}
