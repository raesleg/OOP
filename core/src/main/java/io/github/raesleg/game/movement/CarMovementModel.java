package io.github.raesleg.game.movement;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.movement.MovementModel;
import io.github.raesleg.engine.physics.PhysicsBody;

/**
 * Shared movement execution for both player and NPC cars.
 * Player and AI differ by their strategy/input source and VehicleProfile,
 * not by needing completely separate movement models.
 */
public class CarMovementModel implements MovementModel {

    private final VehicleProfile profile;
    private SurfaceEffect surface = SurfaceEffect.DEFAULT;
    private float currentForwardSpeed = 0f;

    private boolean slideRecoveryActive = false;
    private float slideRecoveryTimer = 0f;

    // Slip state — used by all slippery surfaces, not just puddles
    private boolean wasSlipping = false;
    private float driftVx = 0f;

    // Particle spawning effect
    private final EntityManager entityManager;
    private boolean inHazardZone = false;
    private float splashTimer = 0f;
    private static final float SPLASH_INTERVAL = 0.08f;

    public CarMovementModel(VehicleProfile profile, EntityManager entityManager) {
        this.profile = profile;
        this.entityManager = entityManager;
    }

    @Override
    public void step(PhysicsBody body, float x, float y, float dt) {
        if (body == null) {
            return;
        }

        float steerInput = MathUtils.clamp(x, -1f, 1f);
        float throttleInput = MathUtils.clamp(y, -1f, 1f);

        if (Math.abs(steerInput) < profile.getSteeringDeadzone()) {
            steerInput = 0f;
        }

        Vector2 velocity = body.getVelocity().cpy();
        float vx;

        if (surface.isSlippery()) {
            if (!wasSlipping) {
                if (surface.getStickyness() > 0f) {
                    // Oil — seed from real velocity, no kick, just locks you in
                    driftVx = velocity.x;
                } else {
                    // Puddle — random sideways kick for dramatic uncontrolled sway
                    float kickDir = (Math.random() > 0.5f) ? 1f : -1f;
                    driftVx = kickDir * profile.getMaxLateralSpeed() * 0.7f;
                }
            }
            wasSlipping = true;

            // Unified slip calculation — same formula for all slippery surfaces
            driftVx *= surface.getMomentumRetention();
            float steerContrib = steerInput * profile.getMaxLateralSpeed()
                    * surface.getSteerInfluence();

            // Oil: actively drag vx back toward zero each frame — feels like glue
            // stickiness = 0 means no drag (puddle), > 0 means surface fights movement
            float stuck = driftVx * surface.getStickyness();
            vx = driftVx + steerContrib - stuck;

            vx = MathUtils.clamp(vx,
                    -profile.getMaxLateralSpeed() * 0.75f,
                    profile.getMaxLateralSpeed() * 0.75f);

        } else {
            wasSlipping = false;
            driftVx = 0f;
            float gripMultiplier = getCurrentGripMultiplier(dt);
            float steeringResponse = profile.getSteeringResponse() * gripMultiplier;
            float targetVx = steerInput * profile.getMaxLateralSpeed() * gripMultiplier;
            vx = approach(velocity.x, targetVx, steeringResponse * dt);
        }

        // Spawn continuous splash particles while in hazard
        if (inHazardZone && entityManager != null) {
            splashTimer += dt;
            if (splashTimer >= SPLASH_INTERVAL) {
                splashTimer = 0f;
                
                // Get car center position (in pixels)
                Vector2 pos = body.getPosition();
                float px = pos.x * io.github.raesleg.engine.Constants.PPM;
                float py = pos.y * io.github.raesleg.engine.Constants.PPM;
                
                // Spawn appropriate particle type based on surface
                if (surface == SurfaceEffect.PUDDLE) {
                    io.github.raesleg.game.entities.misc.Particle
                        .spawnContinuousSplash(entityManager, px, py);
                } else if (surface == SurfaceEffect.MUD) {
                    // Mud particles less frequent - only spawn 50% of the time
                    if (Math.random() > 0.5) {
                        io.github.raesleg.game.entities.misc.Particle
                            .spawnMudSplatter(entityManager, px, py, 2);
                    }
                }
            }
        }

        float vy = computeForwardVelocity(throttleInput, dt);

        body.setLinearDamping(profile.getLinearDamping() * surface.getDampingMultiplier());
        body.setAngularVelocity(0f);
        body.setVelocity(vx, vy);
    }

    private static final float REVERSE_MAX_SPEED = 3f;
    private static final float REVERSE_ACCEL = 8f;

    private float computeForwardVelocity(float throttleInput, float dt) {
        if (!profile.allowsForwardMotion()) {
            // Forward prohibited — handle optional reverse
            if (profile.allowsReverseMotion() && throttleInput < 0f) {
                float reverseTarget = throttleInput * REVERSE_MAX_SPEED;
                currentForwardSpeed = approach(currentForwardSpeed, reverseTarget, REVERSE_ACCEL * dt);
                return currentForwardSpeed;
            }
            // Decelerate back to zero when not pressing reverse
            currentForwardSpeed = approach(currentForwardSpeed, 0f, REVERSE_ACCEL * dt);
            return currentForwardSpeed;
        }

        float clampedThrottle = Math.max(0f, throttleInput);
        float targetForwardSpeed = clampedThrottle
                * profile.getMaxForwardSpeed()
                * surface.getForwardSpeedMultiplier();

        float acceleration = profile.getAcceleration() * surface.getAccelerationMultiplier();
        float braking = profile.getBrakeStrength() * Math.max(0.6f, surface.getAccelerationMultiplier());

        if (targetForwardSpeed > currentForwardSpeed) {
            currentForwardSpeed = Math.min(currentForwardSpeed + acceleration * dt, targetForwardSpeed);
        } else {
            currentForwardSpeed = Math.max(currentForwardSpeed - braking * dt, targetForwardSpeed);
        }

        if (!profile.allowsReverseMotion() && currentForwardSpeed < 0f) {
            currentForwardSpeed = 0f;
        }

        return currentForwardSpeed;
    }

    private float getCurrentGripMultiplier(float dt) {
        if (!slideRecoveryActive) {
            return surface.getLateralGripMultiplier();
        }

        slideRecoveryTimer -= dt;
        if (slideRecoveryTimer <= 0f) {
            slideRecoveryActive = false;
            slideRecoveryTimer = 0f;
            return SurfaceEffect.DEFAULT.getLateralGripMultiplier();
        }

        float blend = slideRecoveryTimer / profile.getSlideRecoveryTime();
        float lowGrip = SurfaceEffect.PUDDLE.getLateralGripMultiplier();
        return lowGrip + (1f - lowGrip) * (1f - blend);
    }

    @Override
    public void onEnterZone(PhysicsBody body, Object zoneTuning) {
        if (zoneTuning instanceof SurfaceEffect effect) {
            surface = effect;
            inHazardZone = true;
            splashTimer = 0f;     
            
            if (body != null) {
                body.setLinearDamping(profile.getLinearDamping() * surface.getDampingMultiplier());
                
                // Spawn initial splash burst
                if (entityManager != null) {
                    Vector2 pos = body.getPosition();
                    float px = pos.x * io.github.raesleg.engine.Constants.PPM;
                    float py = pos.y * io.github.raesleg.engine.Constants.PPM;
                    
                    if (effect == SurfaceEffect.PUDDLE) {
                        // Big splash on entry
                        io.github.raesleg.game.entities.misc.Particle
                            .spawnWaterSplash(entityManager, px, py, 12);
                    } else if (effect == SurfaceEffect.MUD) {
                        // Heavy splatter on entry
                        io.github.raesleg.game.entities.misc.Particle
                            .spawnMudSplatter(entityManager, px, py, 8);
                    }
                }
            }
        }
    }

    @Override
    public void onExitZone(PhysicsBody body) {
        if (profile.usesPuddleSlideRecovery() && surface.isSlippery()) {
            slideRecoveryActive = true;
            slideRecoveryTimer = profile.getSlideRecoveryTime();
        }

        surface = SurfaceEffect.DEFAULT;
        inHazardZone = false;
        splashTimer = 0f;

        if (body != null) {
            body.setLinearDamping(profile.getLinearDamping());
        }
    }

    private float approach(float current, float target, float amount) {
        if (current < target) {
            return Math.min(current + amount, target);
        }
        if (current > target) {
            return Math.max(current - amount, target);
        }
        return target;
    }
}
