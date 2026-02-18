package io.github.raesleg.demo;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.physics.box2d.BodyDef;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.Shape;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.engine.physics.PhysicsWorld;

public class MotionZone extends Shape {

    private final MotionTuning tuning;
    private final PhysicsBody body;

    public MotionZone(
                PhysicsWorld world,
                float centerXM, float centerYM,
                float halfWM, float halfHM,
                MotionTuning tuning,
                Color color) 
    {
        super( // in pixels
            (centerXM - halfWM) * Constants.PPM,   
            (centerYM - halfHM) * Constants.PPM,   
            (halfWM * 2f) * Constants.PPM,         
            (halfHM * 2f) * Constants.PPM,        
            color
        );

        this.tuning = tuning;

        this.body = world.createBody(
                BodyDef.BodyType.StaticBody,
                centerXM, centerYM,
                halfWM, halfHM,
                0f, 0f,
                true,
                this
        );
    }

    public MotionTuning getTuning() { 
        return tuning; 
    }

    @Override
    public void draw(ShapeRenderer sr) {
        sr.setColor(getColor());
        sr.rect(getX(), getY(), getW(), getH());
    }

    @Override
    public void dispose() {
        body.destroy();
    }
}
