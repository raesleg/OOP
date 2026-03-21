package io.github.raesleg.game.movement;

/**
 * Multipliers applied on top of a VehicleProfile when the vehicle is inside a zone.
 * This replaces MotionTuning with lighter, clearer intent.
 */
public final class SurfaceEffect {

    private final float forwardSpeedMultiplier;
    private final float accelerationMultiplier;
    private final float lateralGripMultiplier;
    private final float dampingMultiplier;
    private final boolean slippery;

    private SurfaceEffect(
            float forwardSpeedMultiplier,
            float accelerationMultiplier,
            float lateralGripMultiplier,
            float dampingMultiplier,
            boolean slippery) {
        this.forwardSpeedMultiplier = forwardSpeedMultiplier;
        this.accelerationMultiplier = accelerationMultiplier;
        this.lateralGripMultiplier = lateralGripMultiplier;
        this.dampingMultiplier = dampingMultiplier;
        this.slippery = slippery;
    }

    public float getForwardSpeedMultiplier() {
        return forwardSpeedMultiplier;
    }

    public float getAccelerationMultiplier() {
        return accelerationMultiplier;
    }

    public float getLateralGripMultiplier() {
        return lateralGripMultiplier;
    }

    public float getDampingMultiplier() {
        return dampingMultiplier;
    }

    public boolean isSlippery() {
        return slippery;
    }

    public static final SurfaceEffect DEFAULT =
            new SurfaceEffect(1f, 1f, 1f, 1f, false);

    public static final SurfaceEffect PUDDLE =
            new SurfaceEffect(0.95f, 0.80f, 0.25f, 0.35f, true);

    public static final SurfaceEffect CROSSWALK =
            new SurfaceEffect(0.55f, 0.65f, 1.00f, 1.20f, false);

    public static final SurfaceEffect SCHOOL_ZONE =
            new SurfaceEffect(0.45f, 0.55f, 1.00f, 1.10f, false);
}
