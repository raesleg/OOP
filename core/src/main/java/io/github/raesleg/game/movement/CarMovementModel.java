package io.github.raesleg.game.movement;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.movement.MovementModel;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.GameConstants;

// Shared movement execution for both player and NPC cars
// Player and AI differ by their strategy/input source and VehicleProfilew
// Exposes effect signals for scene layer, does not handle spawning of particles
public class CarMovementModel implements MovementModel {

    private final VehicleProfile profile;
    private SurfaceEffect surface = SurfaceEffect.DEFAULT;
    private float currentForwardSpeed = 0f;

    private boolean slideRecoveryActive = false;
    private float slideRecoveryTimer = 0f;

    // Slip state
    private boolean wasSlipping = false;
    private float driftVx = 0f;

    // Hazard visual effect state
    private boolean inHazardZone = false;
    private float effectTimer = 0f;
    private boolean emitTrailEffectThisStep = false;
    private boolean emitEntryEffect = false;
    private boolean emitExitEffect = false;

    public CarMovementModel(VehicleProfile profile) {
        this.profile = profile;
    }

    public CarMovementModel(VehicleProfile profile, EntityManager ignoredEntityManager) {
        this(profile);
    }

    @Override
    public void step(PhysicsBody body, float x, float y, float dt) {
        if (body == null) {
            return;
        }

        emitTrailEffectThisStep = false;

        float steerInput = sanitizeSteeringInput(x);
        float throttleInput = MathUtils.clamp(y, -1f, 1f);

        Vector2 velocity = body.getVelocity().cpy();
        float vx = computeLateralVelocity(velocity, steerInput, dt);
        float vy = computeForwardVelocity(throttleInput, dt);

        if (inHazardZone) {
            effectTimer += dt;
            if (effectTimer >= GameConstants.EFFECT_INTERVAL) {
                effectTimer = 0f;
                emitTrailEffectThisStep = true;
            }
        }

        body.setLinearDamping(profile.getLinearDamping() * surface.getDampingMultiplier());
        body.setAngularVelocity(0f);
        body.setVelocity(vx, vy);
    }

    private float sanitizeSteeringInput(float input) {
        float steerInput = MathUtils.clamp(input, -1f, 1f);
        if (Math.abs(steerInput) < profile.getSteeringDeadzone()) {
            return 0f;
        }
        return steerInput;
    }

    private float computeLateralVelocity(Vector2 velocity, float steerInput, float dt) {
        if (surface.isSlippery()) {
            return computeSlipperyLateralVelocity(velocity, steerInput);
        }

        wasSlipping = false;
        driftVx = 0f;

        float gripMultiplier = getCurrentGripMultiplier(dt);
        float steeringResponse = profile.getSteeringResponse() * gripMultiplier;
        float targetVx = steerInput * profile.getMaxLateralSpeed() * gripMultiplier;
        return approach(velocity.x, targetVx, steeringResponse * dt);
    }

    private float computeSlipperyLateralVelocity(Vector2 velocity, float steerInput) {
        if (!wasSlipping) {
            driftVx = createInitialSlipVelocity(velocity.x);
        }
        wasSlipping = true;

        driftVx *= surface.getMomentumRetention();
        float steerContrib = steerInput * profile.getMaxLateralSpeed() * surface.getSteerInfluence();
        float stuck = driftVx * surface.getStickyness();
        float vx = driftVx + steerContrib - stuck;

        return MathUtils.clamp(
                vx,
                -profile.getMaxLateralSpeed() * 0.75f,
                profile.getMaxLateralSpeed() * 0.75f);
    }

    private float createInitialSlipVelocity(float currentVelocityX) {
        if (surface.getStickyness() > 0f) {
            return currentVelocityX;
        }

        float kickDirection = Math.random() > 0.5f ? 1f : -1f;
        return kickDirection * profile.getMaxLateralSpeed() * 0.7f;
    }

    private float computeForwardVelocity(float throttleInput, float dt) {
        if (!profile.allowsForwardMotion()) {
            return computeRestrictedForwardVelocity(throttleInput, dt);
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

    private float computeRestrictedForwardVelocity(float throttleInput, float dt) {
        if (profile.allowsReverseMotion() && throttleInput < 0f) {
            float reverseTarget = throttleInput * GameConstants.REVERSE_MAX_SPEED;
            currentForwardSpeed = approach(currentForwardSpeed, reverseTarget, GameConstants.REVERSE_ACCEL * dt);
            return currentForwardSpeed;
        }

        currentForwardSpeed = approach(currentForwardSpeed, 0f, GameConstants.REVERSE_ACCEL * dt);
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
        if (!(zoneTuning instanceof SurfaceEffect effect)) {
            return;
        }

        surface = effect;
        inHazardZone = true;
        effectTimer = 0f;
        emitEntryEffect = true;

        if (body != null) {
            body.setLinearDamping(profile.getLinearDamping() * surface.getDampingMultiplier());
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
        effectTimer = 0f;
        emitExitEffect = true;

        if (body != null) {
            body.setLinearDamping(profile.getLinearDamping());
        }
    }

    public boolean consumeEntryEffectSignal() {
        boolean result = emitEntryEffect;
        emitEntryEffect = false;
        return result;
    }

    public boolean consumeExitEffectSignal() {
        boolean result = emitExitEffect;
        emitExitEffect = false;
        return result;
    }

    public boolean didEmitTrailEffectThisStep() {
        return emitTrailEffectThisStep;
    }

    public boolean isInHazardZone() {
        return inHazardZone;
    }

    public SurfaceEffect getSurfaceEffect() {
        return surface;
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
