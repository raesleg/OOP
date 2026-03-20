package io.github.raesleg.game.collision.handlers;

import com.badlogic.gdx.math.Vector2;

import io.github.raesleg.engine.entity.Entity;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.entities.vehicles.npc.world.effects.ExplosionParticle;

/**
 * Handles legacy AI entity explosions.
 */
public class ExplosionCollisionHandler {

    private final EntityManager entityManager;
    private final SoundDevice soundManager;
    private final float explosionForceThreshold;

    public ExplosionCollisionHandler(EntityManager entityManager, SoundDevice soundManager,
            float explosionForceThreshold) {
        this.entityManager = entityManager;
        this.soundManager = soundManager;
        this.explosionForceThreshold = explosionForceThreshold;
    }

    public boolean canHandle(Entity a, Entity b) {
        // Check for AI vs User collision (legacy Part 1 feature)
        MovableEntity aiEntity = getAIEntity(a, b);
        MovableEntity userEntity = getUserEntity(a, b);
        return aiEntity != null && userEntity != null;
    }

    public void handleImpact(Entity entityA, Entity entityB, float force, Vector2 impactPoint) {
        if (force < explosionForceThreshold) return;

        MovableEntity aiEntity = getAIEntity(entityA, entityB);
        MovableEntity userEntity = getUserEntity(entityA, entityB);

        if (aiEntity != null && userEntity != null) {
            // Play explosion sound
            if (soundManager != null) {
                soundManager.playSound("explosion", 1.0f);
            }

            // Create radial explosion force
            createExplosion(impactPoint, force);

            // Spawn visual particles
            ExplosionParticle.spawnExplosion(entityManager, impactPoint, force);
        }
    }

    private MovableEntity getAIEntity(Entity a, Entity b) {
        if (a instanceof MovableEntity ma && ma.isAIControlled()) return ma;
        if (b instanceof MovableEntity mb && mb.isAIControlled()) return mb;
        return null;
    }

    private MovableEntity getUserEntity(Entity a, Entity b) {
        if (a instanceof MovableEntity ma && !ma.isAIControlled()) return ma;
        if (b instanceof MovableEntity mb && !mb.isAIControlled()) return mb;
        return null;
    }

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

                if (dist2 < radius2 && dist2 > 0.000001f) {
                    float dist = (float) Math.sqrt(dist2);
                    float nx = dx / dist;
                    float ny = dy / dist;
                    float falloff = 1f - (dist / radius);
                    float explosionForce = force * falloff * 5000f;

                    Vector2 impulse = new Vector2(nx, ny).scl(explosionForce);
                    pb.applyImpulseAtCenter(impulse);
                }
            }
        }
    }
}