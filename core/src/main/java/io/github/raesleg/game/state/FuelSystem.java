package io.github.raesleg.game.state;

/**
 * FuelSystem — Tracks the player's fuel level which drains over time.
 * <p>
 * The player must collect charge pickups to replenish fuel.
 * When fuel reaches zero, the level is lost.
 * <p>
 * <b>SRP:</b> Only manages fuel state — does not handle rendering or scoring.
 */
public class FuelSystem {

    private float fuel;
    private final float drainRate;
    private final float rechargeAmount;

    /**
     * @param drainRate      fuel lost per second while moving (0.0–1.0 scale)
     * @param rechargeAmount fuel restored per pickup (0.0–1.0 scale)
     */
    public FuelSystem(float drainRate, float rechargeAmount) {
        this.fuel = 1.0f;
        this.drainRate = drainRate;
        this.rechargeAmount = rechargeAmount;
    }

    /**
     * Drains fuel when the vehicle is moving.
     *
     * @param deltaTime frame time
     * @param isMoving  true if the player car is currently moving
     */
    public void update(float deltaTime, boolean isMoving) {
        if (isMoving) {
            fuel = Math.max(0f, fuel - drainRate * deltaTime);
        }
    }

    /** Restores fuel by the recharge amount (capped at 1.0). */
    public void recharge() {
        fuel = Math.min(1.0f, fuel + rechargeAmount);
    }

    /** Returns fuel as 0.0 (empty) to 1.0 (full). */
    public float getFuel() {
        return fuel;
    }

    /** Returns true when fuel has been completely depleted. */
    public boolean isEmpty() {
        return fuel <= 0f;
    }
}
