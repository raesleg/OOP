package io.github.raesleg.demo;

public final class MotionTuning {
    private float maxSpeed, maxForce, lateralGrip, linearDamping;

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

    public static final MotionTuning LOW_TRACTION = new MotionTuning(4.5f, 2.0f, 0.03f, 0.02f);

    public static final MotionTuning HIGH_FRICTION = new MotionTuning(3.0f, 25f, 1.5f, 2.0f);

    public static final MotionTuning DEFAULT = new MotionTuning(4.5f, 25f, 0.12f, 0.05f);
}
