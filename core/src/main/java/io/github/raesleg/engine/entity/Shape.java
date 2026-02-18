package io.github.raesleg.engine.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public abstract class Shape extends Entity {

    private Color color;

    public Shape(float x, float y, float w, float h, Color c) {
        super(x, y, w, h);
        this.color = c;
    };

    public Color getColor() {
        return color;
    };

    public void setColor(Color c) {
        this.color = c;
    }

    public void draw(ShapeRenderer shape) {
    };
}
