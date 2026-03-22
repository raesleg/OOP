package io.github.raesleg.game.movement;

/**
 * Multipliers applied on top of a VehicleProfile when inside a zone.
 *
 * slippery        — enters slip physics path in CarMovementModel
 * momentumRetention — lateral velocity kept per frame (0.93 bleeds, 0.97 locks)
 * steerInfluence  — how much input fights momentum (0.04 = barely, 0.08 = a little)
 * stickyness      — fraction of driftVx dragged back to zero per frame
 *                   0.0 = free slide (puddle), 0.55 = mud/glue (oil)
 * Fields:
 *   forwardSpeedMultiplier — caps max forward speed
 *   accelerationMultiplier — how fast speed builds/falls
 *   lateralGripMultiplier  — steering input effect
 *   dampingMultiplier      — physics engine resistance
*/
public final class SurfaceEffect {

    private final float forwardSpeedMultiplier;
    private final float accelerationMultiplier;
    private final float lateralGripMultiplier;
    private final float dampingMultiplier;
    private final boolean slippery;
    private final float momentumRetention;
    private final float steerInfluence;
    private final float stickyness;

    private SurfaceEffect(
            float forwardSpeedMultiplier,
            float accelerationMultiplier,
            float lateralGripMultiplier,
            float dampingMultiplier,
            boolean slippery,
            float momentumRetention,
            float steerInfluence,
            float stickyness) {
        this.forwardSpeedMultiplier = forwardSpeedMultiplier;
        this.accelerationMultiplier = accelerationMultiplier;
        this.lateralGripMultiplier  = lateralGripMultiplier;
        this.dampingMultiplier      = dampingMultiplier;
        this.slippery               = slippery;
        this.momentumRetention      = momentumRetention;
        this.steerInfluence         = steerInfluence;
        this.stickyness             = stickyness;
    }

    public float getForwardSpeedMultiplier() { return forwardSpeedMultiplier; }
    public float getAccelerationMultiplier() { return accelerationMultiplier; }
    public float getLateralGripMultiplier()  { return lateralGripMultiplier; }
    public float getDampingMultiplier()      { return dampingMultiplier; }
    public boolean isSlippery()              { return slippery; }
    public float getMomentumRetention()      { return momentumRetention; }
    public float getSteerInfluence()         { return steerInfluence; }
    public float getStickyness()             { return stickyness; }

    public static final SurfaceEffect DEFAULT = new SurfaceEffect(
            1f, 1f, 1f, 1f,
            false, 0f, 1f, 0f);

    // Puddle — random kick on entry, free lateral slide, bleeds off over time
    public static final SurfaceEffect PUDDLE = new SurfaceEffect(
            0.95f, 0.80f, 0.25f, 0.35f,
            true, 0.93f, 0.08f, 0.0f);

    // Oil — no kick, but movement locked and actively resisted, speed gutted
    // stickyness 0.55 = each frame 55% of drift dragged back to zero (feels like mud)
    // dampingMultiplier 25 = physics engine fights velocity hard
    public static final SurfaceEffect MUD = new SurfaceEffect(
            0.05f, 0.02f, 0.12f, 25f,
            true, 0.85f, 0.04f, 0.55f);

    public static final SurfaceEffect CROSSWALK = new SurfaceEffect(
            0.55f, 0.65f, 1.00f, 1.20f,
            false, 0f, 1f, 0f);

    public static final SurfaceEffect SCHOOL_ZONE = new SurfaceEffect(
            0.45f, 0.55f, 1.00f, 1.10f,
            false, 0f, 1f, 0f);
}