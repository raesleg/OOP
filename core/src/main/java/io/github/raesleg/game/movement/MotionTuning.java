package io.github.raesleg.game.movement;

public final class MotionTuning {
    private final float maxSpeed;
    private final float maxForce;
    private final float lateralGrip;
    private final float linearDamping;

    private MotionTuning(float maxSpeed, float maxForce, float lateralGrip, float linearDamping) {
        this.maxSpeed = maxSpeed;
        this.maxForce = maxForce;
        this.lateralGrip = lateralGrip;
        this.linearDamping = linearDamping;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public float getMaxForce() {
        return maxForce;
    }

    public float getLateralGrip() {
        return lateralGrip;
    }

    public float getLinearDamping() {
        return linearDamping;
    }

    public static final MotionTuning LOW_TRACTION =
            new MotionTuning(90f, 16f, 0.05f, 0.35f);

    public static final MotionTuning HIGH_FRICTION =
            new MotionTuning(60f, 30f, 0.35f, 4.0f);

    public static final MotionTuning DEFAULT =
            new MotionTuning(90f, 24f, 0.18f, 2.2f);
}