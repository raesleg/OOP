package io.github.raesleg.game.entities.vehicles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.raesleg.engine.entity.IFlashable;
import io.github.raesleg.engine.io.ControlSource;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.movement.PlayerMovement;
import io.github.raesleg.game.movement.PlayerMovementModel;

/**
 * PlayerCar — Extended MovableEntity with damage flash capability.
 * <p>
 * This class extends the engine's MovableEntity and implements IFlashable
 * to add visual feedback for collisions without modifying the abstract engine.
 * <p>
 * <b>Design Pattern:</b> This demonstrates the <b>Open/Closed Principle</b>
 * — the engine's MovableEntity is closed for modification but open for
 * extension. We add game-specific features (flash effect) through extension
 * and interface implementation.
 * <p>
 * <b>Flash Effect:</b> When triggered, the car rapidly blinks between visible
 * and semi-transparent for a short duration, similar to invincibility frames
 * in classic platformers like Super Mario.
 */
public class PlayerCar extends MovableEntity implements IFlashable {
    
    /* Flash effect configuration */
    private static final float FLASH_DURATION = 0.8f; // Total flash time in seconds
    private static final float FLASH_FREQUENCY = 8f;  // Blinks per second
    
    /* Flash state */
    private float flashTimer;
    private boolean isFlashing;
    
    /**
     * Creates a player-controlled car with flash capability.
     * 
     * @param filename Texture asset path
     * @param x Initial X position (pixels)
     * @param y Initial Y position (pixels)
     * @param w Width (pixels)
     * @param h Height (pixels)
     * @param controls Control source (UserControlled or AIControlled)
     * @param movement Movement model (friction, etc.)
     * @param body Physics body
     */
    public PlayerCar(
        String filename, 
        float x, float y, 
        float w, float h,
        ControlSource controls, 
        PhysicsBody body) {

        super(filename, x, y, w, h, controls, new PlayerMovementModel(), body);

        setMovementStrategy(new PlayerMovement());
        
        this.flashTimer = 0f;
        this.isFlashing = false;
    }
    
    // ═══════════════════════════════════════════════════════════
    // IFlashable Implementation
    // ═══════════════════════════════════════════════════════════
    
    @Override
    public void triggerDamageFlash() {
        // Reset flash timer to start effect
        flashTimer = FLASH_DURATION;
        isFlashing = true;
        
        System.out.println("Damage flash triggered!");
    }
    
    @Override
    public boolean isFlashing() {
        return isFlashing;
    }
    
    // ═══════════════════════════════════════════════════════════
    // Overridden Entity Methods
    // ═══════════════════════════════════════════════════════════
    
    @Override
    public void update(float deltaTime) {
        // Call parent update (physics sync, movement)
        super.update(deltaTime);
        
        // Update flash timer
        if (isFlashing) {
            flashTimer -= deltaTime;
            
            if (flashTimer <= 0) {
                flashTimer = 0;
                isFlashing = false;
            }
        }
    }
    
    @Override
    public void draw(SpriteBatch batch) {
        if (getTexture() == null) return;
        
        // Calculate flash alpha (oscillates between 0.3 and 1.0)
        float alpha = 1.0f;
        
        if (isFlashing) {
            // Create blinking effect using sine wave
            float phase = flashTimer * FLASH_FREQUENCY * (float) Math.PI * 2f;
            float blink = (float) Math.sin(phase);
            
            // Map sine wave (-1 to 1) to alpha range (0.3 to 1.0)
            alpha = 0.65f + blink * 0.35f;
        }
        
        // Apply alpha to batch color
        Color oldColor = batch.getColor().cpy();
        batch.setColor(1f, 1f, 1f, alpha);
        
        // Draw with current alpha
        batch.draw(
            getTexture(),
            getX(),
            getY(),
            getW(),
            getH()
        );
        
        // Restore original color
        batch.setColor(oldColor);
    }
}