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

// PlayerCar Entity with flashing effect, does not decide which movement classes to use (SRP)
public class PlayerCar extends MovableEntity implements IFlashable {

    private static final float FLASH_DURATION = 0.8f;
    private static final float FLASH_FREQUENCY = 8f;

    private float flashTimer;
    private boolean isFlashing;

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
    public void triggerDamageFlash() {
        flashTimer = FLASH_DURATION;
        isFlashing = true;
    }

    @Override
    public boolean isFlashing() {
        return isFlashing;
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);

        if (isFlashing) {
            flashTimer -= deltaTime;
            if (flashTimer <= 0f) {
                flashTimer = 0f;
                isFlashing = false;
            }
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (getTexture() == null) {
            return;
        }

        float alpha = 1.0f;

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

    public CarMovementModel getCarMovementModel() {
        return (CarMovementModel) getMovementModel();
    }
}