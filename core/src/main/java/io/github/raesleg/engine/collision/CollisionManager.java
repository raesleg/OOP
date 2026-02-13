package io.github.raesleg.engine.collision;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.WorldManifold;

import io.github.raesleg.demo.ExplosionParticle;
import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.Entity;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.movement.AIControlled;
import io.github.raesleg.engine.movement.MotionZoneHandler;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.physics.IPhysics;
import io.github.raesleg.engine.physics.PhysicsBody;

public class CollisionManager implements ContactListener {

    private EntityManager entityManager;
    private MotionZoneHandler zoneHandler = new MotionZoneHandler();
    private record Pair(Object a, Object b) {}

    private Pair pair(Contact c) {
        Object a = c.getFixtureA().getBody().getUserData();
        Object b = c.getFixtureB().getBody().getUserData();
        return new Pair(a, b);
    }

    public CollisionManager(IPhysics physics, EntityManager entityManager) {
        this.entityManager = entityManager;
        // contact listener for Box2D lib world
        physics.setContactListener(this);
    }

    // Box2D ContactListener interface methods
    @Override
    public void beginContact(Contact contact) {
        Pair p = pair(contact);
        zoneHandler.handle(p.a, p.b, true);

        if (p.a() instanceof Entity ea && p.b() instanceof Entity eb) {
            handleCollision(ea, eb, contact);
        }
    }

    @Override
    public void endContact(Contact contact) {
        Pair p = pair(contact);
        zoneHandler.handle(p.a(), p.b(), false);
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {} // called before physics resolution

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
        // called after physics resolution - used for impact detection
        if (contact.getFixtureA().isSensor() || contact.getFixtureB().isSensor())
            return;

        Pair p = pair(contact);

        if (p.a() instanceof Entity ea && p.b() instanceof Entity eb) {
            float impactForce = impulse.getNormalImpulses()[0];
            handleImpact(ea, eb, impactForce, contact);
        }
    }

    private void handleCollision(Entity a, Entity b, Contact contact) {
        // game logic here (what happens when collision?)
        System.out.println("Collision detected between entities"); // print logging for now
    }

    private void handleImpact(Entity a, Entity b, float force, Contact contact) {
        System.out.println("Impact force: " + force); // logging for debugging
        // check for explosion threshold
        if (force > 0.1f) { // threshold value
            WorldManifold manifold = contact.getWorldManifold();
            Vector2 impactPoint = manifold.getPoints()[0];

            // check if entity is AI-controlled / user-controlled
            MovableEntity aiEntity = null;
            MovableEntity userEntity = null;

            if (a instanceof MovableEntity) {
                if (isAIControlled((MovableEntity) a)) {
                    aiEntity = (MovableEntity) a;
                } else {
                    userEntity = (MovableEntity) a;
                }
            }

            if (b instanceof MovableEntity) {
                if (isAIControlled((MovableEntity) b)) {
                    aiEntity = (MovableEntity) b;
                } else {
                    userEntity = (MovableEntity) b;
                }
            }

            // if user-controlled collides with ai-controlled, explode ai entity
            if (aiEntity != null && userEntity != null) {
                System.out.println("Explosion triggered at " + impactPoint); // logging
                createParticleExplosion(aiEntity, impactPoint, force);
            }
            // apply physics explosion force
            createExplosion(impactPoint, force);
        }
    }

    private boolean isAIControlled(MovableEntity entity) {
        // check if entity uses AIControlled
        return entity.getControls() instanceof AIControlled;
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

                    // direction normalized
                    float nx = dx / dist;
                    float ny = dy / dist;

                    float falloff = 1f - (dist / radius);
                    float explosionForce = force * falloff * 5000f;

                    Vector2 impulse = new Vector2(nx, ny).scl(explosionForce);

                    pb.applyImpulseAtCenter(impulse);

                    // pb.getBody().applyLinearImpulse(
                    //     impulse,
                    //     pb.getBody().getWorldCenter(),
                    //     true
                    // );
                }
            }
        }
    }

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
