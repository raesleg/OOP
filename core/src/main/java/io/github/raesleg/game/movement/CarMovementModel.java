package io.github.raesleg.game.movement;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import io.github.raesleg.engine.movement.MovementModel;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.GameConstants;

// Shared movement execution for both player and NPC cars
// Player and AI differ by their strategy/input source and VehicleProfilew
// Exposes effect signals for scene layer, does not handle spawning of particles (HazardEffectSystem handles that)
public class CarMovementModel implements MovementModel {

    private final VehicleProfile profile;

    // Current driving state
    private SurfaceEffect currentSurface = SurfaceEffect.DEFAULT;
    private float forwardSpeed = 0f;

    // Slip / recovery state
    private boolean recoveringGrip = false;
    private float recoveryTimer = 0f;
    private boolean drifting = false;
    private float driftLateralSpeed = 0f;

    // Hazard effect signals used by HazardEffectSystem
    private boolean insideHazard = false;
    private float trailEffectTimer = 0f;
    private boolean trailEffectThisStep = false;
    private boolean entryEffectPending = false;
    private boolean exitEffectPending = false;

    public CarMovementModel(VehicleProfile profile) {
        this.profile = profile;
    }

    @Override
    public void step(PhysicsBody body, float x, float y, float dt) {
        if (body == null) {
            return;
        }

        trailEffectThisStep = false;

        float steerInput = MathUtils.clamp(x, -1f, 1f);
        float throttleInput = MathUtils.clamp(y, 0f, 1f);

        Vector2 velocity = body.getVelocity().cpy();
        float sidewaysSpeed = calculateSidewaysSpeed(velocity, steerInput, dt);
        float nextForwardSpeed = calculateForwardSpeed(throttleInput, dt);

        updateHazardTrailTimer(dt);

        body.setLinearDamping(profile.getLinearDamping() * currentSurface.getDampingMultiplier());
        body.setAngularVelocity(0f);
        body.setVelocity(sidewaysSpeed, nextForwardSpeed);
    }

    private float calculateSidewaysSpeed(Vector2 velocity, float steerInput, float dt) {
        if (currentSurface.isSlippery()) {
            return calculateSlipperySidewaysSpeed(velocity, steerInput);
        }

        drifting = false;
        driftLateralSpeed = 0f;

        float gripMultiplier = calculateGripMultiplier(dt);
        float steeringPower = profile.getSteeringResponse() * gripMultiplier;
        float targetSidewaysSpeed = steerInput * profile.getMaxLateralSpeed() * gripMultiplier;

        return moveTowards(velocity.x, targetSidewaysSpeed, steeringPower * dt);
    }

    private float calculateSlipperySidewaysSpeed(Vector2 velocity, float steerInput) {
        if (!drifting) {
            driftLateralSpeed = createSlipKick(velocity.x);
        }
        drifting = true;

        driftLateralSpeed *= currentSurface.getMomentumRetention();

        float steerContribution =
                steerInput * profile.getMaxLateralSpeed() * currentSurface.getSteerInfluence();
        float stickyLoss = driftLateralSpeed * currentSurface.getStickyness();

        float sidewaysSpeed = driftLateralSpeed + steerContribution - stickyLoss;

        return MathUtils.clamp(
                sidewaysSpeed,
                -profile.getMaxLateralSpeed() * 0.75f,
                profile.getMaxLateralSpeed() * 0.75f
        );
    }

    private float createSlipKick(float currentSidewaysSpeed) {
        if (currentSurface.getStickyness() > 0f) {
            return currentSidewaysSpeed;
        }

        float direction = Math.random() > 0.5f ? 1f : -1f;
        return direction * profile.getMaxLateralSpeed() * 0.7f;
    }

    private float calculateForwardSpeed(float throttleInput, float dt) {
        float targetForwardSpeed =
                throttleInput
                        * profile.getMaxForwardSpeed()
                        * currentSurface.getForwardSpeedMultiplier();

        float effectiveAcceleration =
                profile.getAcceleration();

        forwardSpeed = moveTowards(forwardSpeed, targetForwardSpeed, effectiveAcceleration * dt);
        return forwardSpeed;
    }

    private float calculateGripMultiplier(float dt) {
        if (!recoveringGrip) {
            return currentSurface.getLateralMultiplier();
        }

        recoveryTimer -= dt;
        if (recoveryTimer <= 0f) {
            recoveringGrip = false;
            recoveryTimer = 0f;
            return SurfaceEffect.DEFAULT.getLateralMultiplier();
        }

        if (profile.getSlideRecoveryTime() <= 0f) {
            return SurfaceEffect.DEFAULT.getLateralMultiplier();
        }

        float blend = recoveryTimer / profile.getSlideRecoveryTime();
        float lowGrip = SurfaceEffect.LOW_FRICTION.getLateralMultiplier();
        return lowGrip + (1f - lowGrip) * (1f - blend);
    }

    private void updateHazardTrailTimer(float dt) {
        if (!insideHazard) {
            return;
        }

        trailEffectTimer += dt;
        if (trailEffectTimer >= GameConstants.EFFECT_INTERVAL) {
            trailEffectTimer = 0f;
            trailEffectThisStep = true;
        }
    }

    @Override
    public void onEnterZone(PhysicsBody body, Object zoneTuning) {
        if (!(zoneTuning instanceof SurfaceEffect effect)) {
            return;
        }

        currentSurface = effect;
        insideHazard = true;
        trailEffectTimer = 0f;
        entryEffectPending = true;

        if (body != null) {
            body.setLinearDamping(profile.getLinearDamping() * currentSurface.getDampingMultiplier());
        }
    }

    @Override
    public void onExitZone(PhysicsBody body) {
        if (currentSurface.isSlippery() && profile.getSlideRecoveryTime() > 0f) {
            recoveringGrip = true;
            recoveryTimer = profile.getSlideRecoveryTime();
        }

        currentSurface = SurfaceEffect.DEFAULT;
        insideHazard = false;
        trailEffectTimer = 0f;
        exitEffectPending = true;

        if (body != null) {
            body.setLinearDamping(profile.getLinearDamping());
        }
    }

    public boolean consumeEntryEffectSignal() {
        boolean result = entryEffectPending;
        entryEffectPending = false;
        return result;
    }

    public boolean consumeExitEffectSignal() {
        boolean result = exitEffectPending;
        exitEffectPending = false;
        return result;
    }

    public boolean didEmitTrailEffectThisStep() {
        return trailEffectThisStep;
    }

    public boolean isInsideHazard() {
        return insideHazard;
    }

    public SurfaceEffect getSurfaceEffect() {
        return currentSurface;
    }

    private float moveTowards(float current, float target, float amount) {
        if (current < target) {
            return Math.min(current + amount, target);
        }
        if (current > target) {
            return Math.max(current - amount, target);
        }
        return target;
    }
}