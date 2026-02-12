package io.github.raesleg.engine.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class Surfaces extends Shape {

    public Surfaces(float x, float y, float w, float h, Color c) {
        super(x,y,w,h,c);
    }

    @Override
    public void draw(ShapeRenderer sr) {
        sr.setColor(getColor());
        sr.rect(getX(),getY(),getW(),getH());
    }
}
