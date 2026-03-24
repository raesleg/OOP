package io.github.raesleg.game.entities.vehicles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.raesleg.engine.entity.IFlashable;
import io.github.raesleg.engine.io.ControlSource;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.movement.MovementModel;
import io.github.raesleg.engine.movement.MovementStrategy;
import io.github.raesleg.engine.physics.PhysicsBody;

import io.github.raesleg.game.movement.CarMovementModel;

// Player vehicle with damage flash effect - delegates movement strategy to injected dependencies (SRP)
public class PlayerCar extends MovableEntity implements IFlashable {

    private static final float FLASH_DURATION = 0.8f; // How long damage flash persists
    private static final float FLASH_FREQUENCY = 8f; // Blink cycles per second during flash

    private float flashTimer; // Countdown for active flash effect
    private boolean isFlashing; // Whether damage flash is currently visible

    public PlayerCar(
            String filename,
            float x, float y,
            float w, float h,
            ControlSource controls,
            MovementStrategy strategy,
            MovementModel movementModel,
            PhysicsBody body) {

        super(filename, x, y, w, h, controls, movementModel, body);
        setMovementStrategy(strategy);

        this.flashTimer = 0f;
        this.isFlashing = false;
    }

    @Override
    // Start damage flash effect when player collides with hazard
    public void triggerDamageFlash() {
        flashTimer = FLASH_DURATION;
        isFlashing = true;
    }

    @Override
    // Check if damage flash effect is currently active
    public boolean isFlashing() {
        return isFlashing;
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);

        if (isFlashing) {
            // Countdown flash timer and stop flashing when duration expires
            flashTimer -= deltaTime;
            if (flashTimer <= 0f) {
                flashTimer = 0f;
                isFlashing = false;
            }
        }
    }

    @Override
    // Render with dynamic alpha blending to create damage blink effect during collision feedback
    public void draw(SpriteBatch batch) {
        if (getTexture() == null) {
            return;
        }

        float alpha = 1.0f;

        // Calculate alpha based on sinusoidal wave for smooth blinking during flash
        if (isFlashing) {
            float phase = flashTimer * FLASH_FREQUENCY * (float) Math.PI * 2f;
            float blink = (float) Math.sin(phase);
            alpha = 0.65f + blink * 0.35f;
        }

        Color oldColor = batch.getColor().cpy();
        batch.setColor(1f, 1f, 1f, alpha);

        batch.draw(getTexture(), getX(), getY(), getW(), getH());

        batch.setColor(oldColor);
    }

    // Access to underlying movement model for physics queries (e.g., speed, traction)
    public CarMovementModel getCarMovementModel() {
        return (CarMovementModel) getMovementModel();
    }
}