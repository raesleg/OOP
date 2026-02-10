package io.github.raesleg.demo;

import io.github.raesleg.engine.MotionProfile;

public final class MotionTuning {

    private MotionTuning() {} 

    public static final MotionProfile LOW_TRACTION =
        new MotionProfile(4.5f, 2.0f, 0.03f, 0.02f);

    public static final MotionProfile HIGH_FRICTION =
        new MotionProfile(3.0f, 25f, 1.5f, 2.0f);

    public static final MotionProfile DEFAULT =
        new MotionProfile(4.5f, 25f, 0.12f, 0.05f);
}
