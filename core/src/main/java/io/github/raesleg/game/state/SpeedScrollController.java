package io.github.raesleg.game.state;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.system.IGameSystem;
import io.github.raesleg.game.GameConstants;
import io.github.raesleg.game.io.Keyboard;

/**
 * SpeedScrollController — Manages simulated speed and world scroll offset.
 * <p>
 * Extracted from BaseGameScene to satisfy SRP: speed/scroll is one
 * responsibility, independent of rendering, audio, or scoring.
 * <p>
 * The scene supplies keyboard state each frame; this controller computes
 * the resulting speed and cumulative scroll offset.
 */
public final class SpeedScrollController implements IGameSystem {

    private final float maxSpeed;
    private final float acceleration;
    private final float brakeRate;
    private final float maxScrollPixelsPerSecond;

    private float simulatedSpeed;
    private float scrollOffset;

    private Keyboard keyboard;

    public SpeedScrollController(float maxSpeed, float acceleration,
            float brakeRate, float maxScrollPixelsPerSecond) {
        this.maxSpeed = maxSpeed;
        this.acceleration = acceleration;
        this.brakeRate = brakeRate;
        this.maxScrollPixelsPerSecond = maxScrollPixelsPerSecond;
        this.simulatedSpeed = 0f;
        this.scrollOffset = 0f;
    }

    /** Must be called once after construction to supply the keyboard reference. */
    public void setKeyboard(Keyboard keyboard) {
        this.keyboard = keyboard;
    }

    @Override
    public void update(float deltaTime) {
        if (keyboard == null)
            return;

        if (keyboard.isHeld(Constants.UP)) {
            simulatedSpeed = Math.min(maxSpeed, simulatedSpeed + acceleration * deltaTime);
        } else if (keyboard.isHeld(Constants.DOWN)) {
            simulatedSpeed = Math.max(0f, simulatedSpeed - brakeRate * deltaTime);
        } else {
            simulatedSpeed = Math.max(0f, simulatedSpeed - GameConstants.PASSIVE_DECEL * deltaTime);
        }

        scrollOffset -= getScrollSpeedPixelsPerSecond() * deltaTime;
    }

    @Override
    public void dispose() {
        // No resources to release
    }

    public float getSimulatedSpeed() {
        return simulatedSpeed;
    }

    public float getScrollOffset() {
        return scrollOffset;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public float getScrollSpeedPixelsPerSecond() {
        float t = simulatedSpeed / maxSpeed;
        t = Math.max(0f, Math.min(1f, t));
        return t * maxScrollPixelsPerSecond;
    }

    /**
     * NPC scroll speed with a minimum floor so NPCs always drift
     * down the screen even when the player is stationary.
     */
    public float getNpcScrollSpeedPixelsPerSecond() {
        float minScroll = maxScrollPixelsPerSecond * 0.25f;
        return Math.max(minScroll, getScrollSpeedPixelsPerSecond());
    }

    /**
     * Applies a multiplicative speed penalty (e.g. 0.5 = halve speed).
     * Used by Level 2 on crash to slow the player down instead of exploding.
     */
    public void applySpeedPenalty(float factor) {
        simulatedSpeed *= factor;
    }
}
