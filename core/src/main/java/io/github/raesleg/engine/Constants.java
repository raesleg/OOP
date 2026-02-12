package io.github.raesleg.engine;

/**
 * Constants — Single source of truth for project-wide magic numbers.
 *
 * <p>
 * Centralises values that were previously duplicated across
 * {@code GameScene}, {@code MovableEntity}, {@code CollisionManager},
 * and {@code ExplosionParticle}.
 * </p>
 *
 * <h3>OOP Principle: DRY (Don't Repeat Yourself)</h3>
 * Every constant lives in exactly one place. All other classes
 * reference {@code Constants.PPM} instead of declaring their own copy.
 */
public final class Constants {

    private Constants() {
        // Utility class — no instances
    }

    /**
     * Pixels-Per-Meter: the scale factor between render pixels and
     * Box2D meters. Used by physics, rendering, and collision systems.
     */
    public static final float PPM = 100f;
}
