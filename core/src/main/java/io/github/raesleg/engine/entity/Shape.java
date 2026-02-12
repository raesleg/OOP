package io.github.raesleg.engine.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public abstract class Shape extends Entity {

    private Color color;

    public Shape() {
        super(0,0,0,0);
        this.color = null;
    };

    public Shape(float x, float y, float w, float h, Color c) {
        super(x,y,w,h);
        this.color = c;
    };

    public Color getColor() {
        return color;
    };

    public void setSize(float w, float h) { 
        w = getW(); 
        h = getH();
    }

    public void setColor(Color c) { 
        this.color = c; 
    }

    @Override
    public void draw(SpriteBatch batch) {}

    public void draw(ShapeRenderer shape) {};

    public void dispose() {};
}
