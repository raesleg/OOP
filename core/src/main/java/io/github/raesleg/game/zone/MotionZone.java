package io.github.raesleg.game.zone;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.physics.box2d.BodyDef;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.Shape;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.engine.physics.PhysicsWorld;
import io.github.raesleg.game.movement.MotionTuning;

public class MotionZone extends Shape {

    private MotionTuning tuning;
    private PhysicsBody body;

    public MotionZone(
                PhysicsWorld world,
                float x, float y,
                float w, float h,
                MotionTuning tuning,
                Color color) 
    {
        super( // in pixels
            (x - w) * Constants.PPM,   
            (y - h) * Constants.PPM,   
            (w * 2f) * Constants.PPM,         
            (h * 2f) * Constants.PPM,        
            color
        );

        this.tuning = tuning;

        this.body = world.createBody(
                BodyDef.BodyType.StaticBody,
                x, y,
                w, h,
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
