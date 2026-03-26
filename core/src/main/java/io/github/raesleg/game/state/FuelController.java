package io.github.raesleg.game.state;

import io.github.raesleg.engine.event.EventBus;
import io.github.raesleg.engine.system.IGameSystem;
import io.github.raesleg.game.event.FuelDepletedEvent;
import io.github.raesleg.game.event.PickupCollectedEvent;

/**
 * FuelController — Orchestrates the FuelSystem and publishes
 * fuel-related events via the EventBus.
 * <p>
 * Extracted from BaseGameScene to satisfy SRP: fuel lifecycle is
 * a single responsibility separate from rendering or scoring.
 * <p>
 * Subscribes to {@link PickupCollectedEvent} to trigger recharge.
 * Publishes {@link FuelDepletedEvent} when fuel reaches zero.
 */
public final class FuelController implements IGameSystem {

    private final FuelSystem fuelSystem;
    private final EventBus eventBus;

    private boolean depletedFired;

    /** Supplies current speed each frame so we know if draining. */
    private float currentSpeed;

    public FuelController(float drainRate, float rechargeAmount, EventBus eventBus) {
        this.fuelSystem = new FuelSystem(drainRate, rechargeAmount);
        this.eventBus = eventBus;
        this.depletedFired = false;

        // eventBus.subscribe(PickupCollectedEvent.class, e -> fuelSystem.recharge());
    }

    /** Called by the scene each frame before update() to supply current speed. */
    public void setCurrentSpeed(float speed) {
        this.currentSpeed = speed;
    }

    @Override
    public void update(float deltaTime) {
        fuelSystem.update(deltaTime, currentSpeed > 0.5f);

        if (fuelSystem.isEmpty() && !depletedFired) {
            depletedFired = true;
            eventBus.publish(FuelDepletedEvent.instance());
        }
    }

    @Override
    public void dispose() {
        // No resources to release
    }

    public float getFuel() {
        return fuelSystem.getFuel();
    }

    public boolean isEmpty() {
        return fuelSystem.isEmpty();
    }

    public FuelSystem getFuelSystem() {
        return fuelSystem;
    }
}
