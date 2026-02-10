package io.github.raesleg.engine;

public final class MotionProfile {
    public float maxSpeed; 
    public float maxForce;
    public float lateralGrip;
    public float linearDamping;

    public MotionProfile(float maxSpeed, float maxForce, float lateralGrip, float linearDamping) {
        this.maxSpeed = maxSpeed;
        this.maxForce = maxForce;
        this.lateralGrip = lateralGrip;
        this.linearDamping = linearDamping;
    }
}
