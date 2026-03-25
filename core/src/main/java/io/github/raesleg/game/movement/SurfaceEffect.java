package io.github.raesleg.game.movement;

/**
 * Multipliers applied on top of a VehicleProfile when inside a zone
 *
 * speedMultiplier   - scales forward speed
 * gripMultiplier    - scales steering effectiveness
 * slippery          - enables sideways slip behaviour
 * momentumRetention - how much sideways drift is retained each step
 * steerInfluence    - how much steering input can correct the drift
 * stickiness        - how strongly drift is dragged back toward zero
 */
public final class SurfaceEffect {
        
        private final float speedMultiplier;
        // private final float accelerationMultiplier;
        private final float lateralMultiplier;
        private final float dampingMultiplier;
        private final boolean isSlippery;
        private final float momentumRetention;
        private final float steerInfluence;
        private final float stickyness;

        private SurfaceEffect(
                float speedMultiplier,
                // float accelerationMultiplier,
                float lateralMultiplier,
                float dampingMultiplier,
                boolean isSlippery,
                float momentumRetention,
                float steerInfluence,
                float stickyness) {
                this.speedMultiplier = speedMultiplier;
                // this.accelerationMultiplier = accelerationMultiplier;
                this.lateralMultiplier = lateralMultiplier;
                this.dampingMultiplier = dampingMultiplier;
                this.isSlippery = isSlippery;
                this.momentumRetention      = momentumRetention;
                this.steerInfluence         = steerInfluence;
                this.stickyness             = stickyness;
        }

        public float getForwardSpeedMultiplier() { return speedMultiplier; }
        // public float getAccelerationMultiplier() { return accelerationMultiplier; }
        public float getLateralMultiplier()  { return lateralMultiplier; }
        public float getDampingMultiplier()    { return dampingMultiplier; }

        public boolean isSlippery()              { return isSlippery; }
        public float getMomentumRetention()      { return momentumRetention; }
        public float getSteerInfluence()         { return steerInfluence; }
        public float getStickyness()             { return stickyness; }

        public static final SurfaceEffect DEFAULT = new SurfaceEffect(
                1f, 
                // 1f,
                1f, 
                1f,
                false, 
                0f, 
                1f, 
                0f);

        // Low-friction surface: slippery, weaker steering, moderate speed loss
        public static final SurfaceEffect LOW_FRICTION = new SurfaceEffect(
                1.50f,
                // 0.80f,
                0.15f, 
                0.35f,
                true, 
                0.93f, 
                0.08f, 
                0.0f);

        // High-friction surface: very slow, strongly resisted movement
        public static final SurfaceEffect HIGH_FRICTION = new SurfaceEffect(
                0.05f,
                // 0.20f,
                0.15f,
                18f,
                true,
                0.85f,
                0.05f,
                0.55f);

        // Reduced-speed zone, but still normal road grip
        public static final SurfaceEffect SLOW_ZONE = new SurfaceEffect(
                0.55f,
                // 0.60f,
                1.0f,
                1.20f,
                false,
                0f,
                1f,
                0f
        );

        // Heavier speed restriction than slow zone
        public static final SurfaceEffect VERY_SLOW_ZONE = new SurfaceEffect(
                0.45f,
                // 0.40f,
                1.0f,
                1.50f,

                false,
                0f,
                1f,
                0f
        );
}