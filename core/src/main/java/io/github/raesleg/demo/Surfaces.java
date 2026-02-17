package io.github.raesleg.demo;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import io.github.raesleg.engine.entity.Shape;

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
