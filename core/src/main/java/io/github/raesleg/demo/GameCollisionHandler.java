package io.github.raesleg.demo;

import com.badlogic.gdx.math.Vector2;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.collision.ICollisionListener;
import io.github.raesleg.engine.entity.Entity;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.physics.PhysicsBody;

public class GameCollisionHandler implements ICollisionListener {

    private final EntityManager entityManager;
    private final float explosionForceThreshold;
    private final SoundDevice soundManager;

    /** Helper record to normalise the order of entity pairs (DRY). */
    private record ZoneCollision(MovableEntity movable, MotionZone zone) {
    }

    /**
     * Extracts a MovableEntity/MotionZone pair regardless of A/B ordering.
     * Returns null if the pair is not a zone collision.
     */
    private ZoneCollision extractZoneCollision(Entity a, Entity b) {
        if (a instanceof MovableEntity m && b instanceof MotionZone z)
            return new ZoneCollision(m, z);
        if (b instanceof MovableEntity m && a instanceof MotionZone z)
            return new ZoneCollision(m, z);
        return null;
    }

    public GameCollisionHandler(EntityManager entityManager, SoundDevice soundManager) {
        this(entityManager, soundManager, 0.1f); // default threshold
    }

    public GameCollisionHandler(EntityManager entityManager, SoundDevice soundManager, float explosionForceThreshold) {
        this.entityManager = entityManager;
        this.soundManager = soundManager;
        this.explosionForceThreshold = explosionForceThreshold;
    }

    @Override
    public void onCollisionBegin(Entity entityA, Entity entityB) {
        ZoneCollision zc = extractZoneCollision(entityA, entityB);
        if (zc != null && zc.movable().getMovementModel() instanceof FrictionMovement fm) {
            fm.onEnterZone(zc.movable().getPhysicsBody(), zc.zone().getTuning());
        }
        // logging
        System.out.println("Collision detected between " + entityA.getClass().getSimpleName() + " and " +
                entityB.getClass().getSimpleName());
    }

    @Override
    public void onCollisionEnd(Entity entityA, Entity entityB) {
        ZoneCollision zc = extractZoneCollision(entityA, entityB);
        if (zc != null && zc.movable().getMovementModel() instanceof FrictionMovement fm) {
            fm.onExitZone(zc.movable().getPhysicsBody());
        }
    }

    @Override
    public void onImpact(Entity entityA, Entity entityB, float impactForce, Vector2 impactPoint) {
        System.out.println("Impact force: " + impactForce);

        // only trigger explosion if impact is strong enough
        if (impactForce < explosionForceThreshold) {
            return;
        }

        // Determine if entity is AI-controlled / user-controlled
        MovableEntity aiEntity = null;
        MovableEntity userEntity = null;

        if (entityA instanceof MovableEntity movableA) {
            if (movableA.isAIControlled()) {
                aiEntity = movableA;
            } else {
                userEntity = movableA;
            }
        }

        if (entityB instanceof MovableEntity movableB) {
            if (movableB.isAIControlled()) {
                aiEntity = movableB;
            } else {
                userEntity = movableB;
            }
        }

        // Game Rule: User hitting AI entity casues explosion
        if (aiEntity != null && userEntity != null) {
            System.out.println("User entity hit AI entity - creating explosion!");

            if (soundManager != null) {
                soundManager.playSound("explosion", 1.0f);
            }

            createParticleExplosion(aiEntity, impactPoint, impactForce);
            createExplosion(impactPoint, impactForce);
        }
    }

    /**
     * Game-Specific Logic
     */

    // apply radial explosion force to all nearby MoveableEntities
    private void createExplosion(Vector2 center, float force) {
        float radius = 5f;
        float radius2 = radius * radius;

        for (Entity e : entityManager.getSnapshot()) {
            if (e instanceof MovableEntity m) {
                PhysicsBody pb = m.getPhysicsBody();

                Vector2 bodyPos = pb.getPosition();

                float dx = bodyPos.x - center.x;
                float dy = bodyPos.y - center.y;
                float dist2 = dx * dx + dy * dy;

                // only affect entities within explosion radius
                if (dist2 < radius2 && dist2 > 0.000001f) {
                    float dist = (float) Math.sqrt(dist2);

                    // direction normalized
                    float nx = dx / dist;
                    float ny = dy / dist;

                    // linear falloff: entities further away get less force
                    float falloff = 1f - (dist / radius);
                    float explosionForce = force * falloff * 5000f; // scale factor

                    Vector2 impulse = new Vector2(nx, ny).scl(explosionForce);

                    pb.applyImpulseAtCenter(impulse);
                }
            }
        }
    }

    // create visual particle explosion at impact point
    private void createParticleExplosion(Entity aiEntity, Vector2 impactPoint, float force) {
        int numParticles = 12;
        float particleSize = 16f;
        float particleLifetime = 1.0f;

        float explosionX = impactPoint.x * Constants.PPM;
        float explosionY = impactPoint.y * Constants.PPM;

        for (int i = 0; i < numParticles; i++) {
            float angle = (float) (Math.PI * 2 * i / numParticles) + (float) (Math.random() * 0.5f - 0.25f); // random
                                                                                                             // scatter
                                                                                                             // pattern

            float speed = 50f + (float) Math.random() * 100f; // random speed

            Vector2 particleVelocity = new Vector2(
                    (float) Math.cos(angle) * speed,
                    (float) Math.sin(angle) * speed);

            // random offset from exact impact point for scatter effect
            float offsetX = (float) (Math.random() * 10 - 5); // -5 to +5 pixels
            float offsetY = (float) (Math.random() * 10 - 5);

            ExplosionParticle particle = new ExplosionParticle(
                    "droplet.png",
                    explosionX + offsetX,
                    explosionY + offsetY,
                    particleSize,
                    particleSize,
                    particleVelocity,
                    particleLifetime);

            entityManager.addEntity(particle);
        }

        System.out.println("Created " + numParticles + " particle droplets"); // logging
    }

}
