package io.github.raesleg.engine.collision;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.WorldManifold;

import io.github.raesleg.engine.entity.Entity;
import io.github.raesleg.engine.physics.PhysicsWorld;

/**
 * Engine-level collision detection using Box2D
 * - listen to collision events
 * - extract entity pairs from physics bodies
 * - delegate to ICollisionListener for game-specific logic
 */

public class CollisionManager implements ContactListener {

    private final ICollisionListener listener;

    private record Pair(Object a, Object b) {}

    // collision manager
    public CollisionManager(PhysicsWorld world, ICollisionListener listener) {
        this.listener = listener;
        // contact listener for Box2D lib world
        world.setContactListener(this);
    }

    /**
     * ContactListener Implementation
     */

    @Override
    public void beginContact(Contact contact) {
        // debug logging
        System.out.println("=== BOX2D CONTACT BEGIN ===");

        Pair p = extractPair(contact);

        // notify listener if both are entities ( UPDATE : allow null entities for wall)
        if (listener != null) {
            Entity ea = p.a() instanceof Entity ? (Entity) p.a() : null;
            Entity eb = p.b() instanceof Entity ? (Entity) p.b() : null;
            listener.onCollisionBegin(ea, eb);
        }
    }

    @Override
    public void endContact(Contact contact) {
        Pair p = extractPair(contact);
        
        // notify listener if both are entities (UPDATE: Allow null entities)
        if (listener != null) {
            Entity ea = p.a() instanceof Entity ? (Entity) p.a() : null;
            Entity eb = p.b() instanceof Entity ? (Entity) p.b() : null;
            listener.onCollisionEnd(ea, eb);
        }
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {} // called before physics resolution

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
        // called after physics resolution - used for impact detection
        if (contact.getFixtureA().isSensor() || contact.getFixtureB().isSensor())
            return;

        Pair p = extractPair(contact);

        // notify listener with impact data
        if (listener != null && p.a instanceof Entity ea && p.b instanceof Entity eb) {
            float impactForce = impulse.getNormalImpulses()[0];
            WorldManifold manifold = contact.getWorldManifold();
            Vector2 impactPoint = manifold.getPoints()[0].cpy(); // copy to avoid mutation

            listener.onImpact(ea, eb, impactForce, impactPoint);
        }
    }

    /**
     *  Helper Methods
     */

    private Pair extractPair(Contact contact) {
        Object a = contact.getFixtureA().getBody().getUserData();
        Object b = contact.getFixtureB().getBody().getUserData();

        return new Pair(a, b);
    }

}
