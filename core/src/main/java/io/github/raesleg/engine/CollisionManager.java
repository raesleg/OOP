package io.github.raesleg.engine;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.WorldManifold;

import io.github.raesleg.demo.ExplosionParticle;
import io.github.raesleg.engine.movement.ControlState;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.engine.physics.PhysicsWorld;

public class CollisionManager implements ContactListener {

    private EntityManager entityManager;
    private PhysicsWorld physicsWorld;

    public CollisionManager(PhysicsWorld physicsWorld, EntityManager entityManager) {
        this.entityManager = entityManager;
        this.physicsWorld = physicsWorld;
        // contact listener for Box2D lib world
        physicsWorld.setContactListener(this);
    }

    // Box2D ContactListener interface methods
    @Override
    public void beginContact(Contact contact) {
        // called when 2 fixtures touch
        Object objectA = contact.getFixtureA().getBody().getUserData();
        Object objectB = contact.getFixtureB().getBody().getUserData();

        if (handleMotionZone(contact.getFixtureA(), contact.getFixtureB(), true))
            return;
        if (handleMotionZone(contact.getFixtureB(), contact.getFixtureA(), true))
            return;

        if (objectA instanceof Entity && objectB instanceof Entity) {
            handleCollision((Entity) objectA, (Entity) objectB, contact);
        }
    }

    /* Empty functions required by ContactListener interface, TBC */
    @Override
    public void endContact(Contact contact) {
        if (handleMotionZone(contact.getFixtureA(), contact.getFixtureB(), false))
            return;
        if (handleMotionZone(contact.getFixtureB(), contact.getFixtureA(), false))
            return;
    } // Called when contact stops

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
    } // called before physics resolution

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
        // called after physics resolution - used for impact detection

        if (contact.getFixtureA().isSensor() || contact.getFixtureB().isSensor())
            return;

        Object objectA = contact.getFixtureA().getBody().getUserData();
        Object objectB = contact.getFixtureB().getBody().getUserData();

        if (objectA instanceof Entity && objectB instanceof Entity) {
            float impactForce = impulse.getNormalImpulses()[0];
            handleImpact((Entity) objectA, (Entity) objectB, impactForce, contact);
        }
    }

    private boolean handleMotionZone(Fixture entityFix, Fixture zoneFix, boolean begin) {
        Object entityObj = entityFix.getBody().getUserData();
        Object zoneObj = zoneFix.getUserData();

        if (entityObj instanceof MovableEntity me && zoneObj instanceof MotionProfile mp) {
            if (begin) {
                me.onEnterZone(mp);
            } else {
                me.onExitZone();
            }
            return true; // handled as zone, not a normal collision
        }
        return false;
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
        return entity.getControlSource() instanceof ControlState.AIControlled;
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

                    pb.getBody().applyLinearImpulse(
                        impulse,
                        pb.getBody().getWorldCenter(),
                        true
                    );
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
