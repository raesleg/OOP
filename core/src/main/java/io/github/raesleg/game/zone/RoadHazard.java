package io.github.raesleg.game.zone;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.IExpirable;
import io.github.raesleg.engine.entity.TextureObject;
import io.github.raesleg.engine.physics.BodyType;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.engine.physics.PhysicsWorld;
import io.github.raesleg.game.movement.SurfaceEffect;

/**
 * RoadHazard — Generic scrollable MotionZone for surface hazards.
 */
public class RoadHazard extends MotionZone implements IExpirable {
    
    private final float relativeY;
    private Texture texture;
    private boolean expired;

    public RoadHazard(PhysicsWorld world, float centreXPx, float relativeY,
            float wPx, float hPx, SurfaceEffect effect, String texturePath) {
        
        // Call super with inline body creation
        super(centreXPx - wPx / 2f, 0, wPx, hPx,
            effect,
            new Color(0.3f, 0.5f, 0.9f, 0.35f),
            createDynamicSensorBody(world, centreXPx, relativeY, wPx, hPx));
        
        this.relativeY = relativeY;
        this.expired = false;
        this.texture = TextureObject.getOrLoadTexture(texturePath);
    }
    
    /**
     * Helper method to create dynamic sensor body.
     * Called before super() - compatible with older Java versions.
     */
    private static PhysicsBody createDynamicSensorBody(PhysicsWorld world, 
            float centreXPx, float relativeY, float wPx, float hPx) {
        PhysicsBody body = world.createBody(
            BodyType.DYNAMIC,  // Changed from KINEMATIC for reliable collision
            centreXPx / Constants.PPM,
            relativeY / Constants.PPM,
            (wPx / Constants.PPM) / 2f,
            (hPx / Constants.PPM) / 2f,
            0.1f,   // Density (very light)
            0f,     // Friction
            true,   // Sensor = true (pass through)
            null
        );
        
        // Set high damping to prevent drift
        body.setLinearDamping(999f);
        
        return body;
    }

    public void updatePosition(float scrollOffset) {
        float screenY = relativeY + scrollOffset;
        setY(screenY);
        
        // Update body position
        getBody().setPosition(
            (getX() + getW() / 2f) / Constants.PPM,
            (screenY + getH() / 2f) / Constants.PPM
        );
        
        // Zero velocity to prevent drift (like Pickupable) - TBC
        getBody().setVelocity(0f, 0f);
        
        if (screenY < -getH() * 3f) {
            expired = true;
        }
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    public void markExpired() {
        expired = true;
    }

    /** Called by spawner for correct z-order (drawn before entity pass). */
    public void drawHazard(SpriteBatch batch) {
        if (texture != null) {
            batch.draw(texture, getX(), getY(), getW(), getH());
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        // no-op — drawn explicitly by spawner for z-ordering
    }

    @Override
    public void draw(ShapeRenderer sr) {
        // no-op
    }

    @Override
    public void dispose() {
        getBody().destroy();
        // Note: shared textures (puddle/mud) are intentionally not disposed here
        // as other instances may still reference them
    }
}