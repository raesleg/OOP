package io.github.raesleg.game.scene;

import java.util.HashMap;
import java.util.Map;

import io.github.raesleg.engine.entity.Entity;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.game.entities.misc.Particle;
import io.github.raesleg.game.movement.SurfaceEffect;

/**
 * SurfaceParticleDispatcher — Polymorphic dispatch of particle effects
 * based on the current {@link SurfaceEffect}.
 * <p>
 * Replaces the if/else chains in Level2Scene with a Map-based strategy
 * lookup, satisfying SRP (scene no longer owns particle-selection logic)
 * and OCP (new surfaces are added by registering a new entry, not by
 * modifying existing code).
 */
public class SurfaceParticleDispatcher {

    /**
     * Functional interface for a single particle-spawning action.
     */
    @FunctionalInterface
    public interface ParticleSpawner {
        void spawn(EntityManager em, float x, float y);
    }

    /**
     * Groups the three particle actions for one surface type.
     */
    public record SurfaceParticleEffect(
            ParticleSpawner onEntry,
            ParticleSpawner onTrail,
            ParticleSpawner onExit) {
    }

    private final Map<SurfaceEffect, SurfaceParticleEffect> effects = new HashMap<>();

    public SurfaceParticleDispatcher() {
        register(SurfaceEffect.PUDDLE, new SurfaceParticleEffect(
                (em, x, y) -> Particle.spawnWaterSplash(em, x, y, 12),
                (em, x, y) -> Particle.spawnContinuousSplash(em, x, y),
                (em, x, y) -> Particle.spawnWaterSplash(em, x, y, 6)));

        register(SurfaceEffect.MUD, new SurfaceParticleEffect(
                (em, x, y) -> Particle.spawnMudSplatter(em, x, y, 8),
                (em, x, y) -> {
                    if (Math.random() > 0.5)
                        Particle.spawnMudSplatter(em, x, y, 2);
                },
                (em, x, y) -> Particle.spawnMudSplatter(em, x, y, 4)));
    }

    /** Registers (or replaces) the particle effect for a surface type. */
    public void register(SurfaceEffect surface, SurfaceParticleEffect effect) {
        effects.put(surface, effect);
    }

    public void dispatchEntry(SurfaceEffect surface, EntityManager em, float x, float y) {
        SurfaceParticleEffect e = effects.get(surface);
        if (e != null && e.onEntry() != null)
            e.onEntry().spawn(em, x, y);
    }

    public void dispatchTrail(SurfaceEffect surface, EntityManager em, float x, float y) {
        SurfaceParticleEffect e = effects.get(surface);
        if (e != null && e.onTrail() != null)
            e.onTrail().spawn(em, x, y);
    }

    public void dispatchExit(SurfaceEffect surface, EntityManager em, float x, float y) {
        SurfaceParticleEffect e = effects.get(surface);
        if (e != null && e.onExit() != null)
            e.onExit().spawn(em, x, y);
    }

    // Overloads that accept an Entity directly — computes centre internally (SRP).
    public void dispatchEntry(SurfaceEffect surface, EntityManager em, Entity entity) {
        dispatchEntry(surface, em, entity.getX() + entity.getW() * 0.5f,
                entity.getY() + entity.getH() * 0.5f);
    }

    public void dispatchTrail(SurfaceEffect surface, EntityManager em, Entity entity) {
        dispatchTrail(surface, em, entity.getX() + entity.getW() * 0.5f,
                entity.getY() + entity.getH() * 0.5f);
    }

    public void dispatchExit(SurfaceEffect surface, EntityManager em, Entity entity) {
        dispatchExit(surface, em, entity.getX() + entity.getW() * 0.5f,
                entity.getY() + entity.getH() * 0.5f);
    }
}
