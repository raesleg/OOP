package io.github.raesleg.engine.collision;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
 * - delegate to ICollisionListeners for game-specific logic
 */

public class CollisionManager implements ContactListener {

    private final List<ICollisionListener> listeners = new CopyOnWriteArrayList<>();

    private record Pair(Object a, Object b) {
    }

    public CollisionManager(PhysicsWorld world) {
        world.setContactListener(this);
    }

    public CollisionManager(PhysicsWorld world, ICollisionListener listener) {
        this(world);
        addListener(listener);
    }

    public void addListener(ICollisionListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(ICollisionListener listener) {
        listeners.remove(listener);
    }

    /**
     * ContactListener Implementation
     */

    @Override
    public void beginContact(Contact contact) {
        Pair p = extractPair(contact);

        Entity ea = p.a() instanceof Entity ? (Entity) p.a() : null;
        Entity eb = p.b() instanceof Entity ? (Entity) p.b() : null;
        for (ICollisionListener l : listeners) {
            l.onCollisionBegin(ea, eb);
        }
    }

    @Override
    public void endContact(Contact contact) {
        Pair p = extractPair(contact);

        Entity ea = p.a() instanceof Entity ? (Entity) p.a() : null;
        Entity eb = p.b() instanceof Entity ? (Entity) p.b() : null;
        for (ICollisionListener l : listeners) {
            l.onCollisionEnd(ea, eb);
        }
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
        if (contact.getFixtureA().isSensor() || contact.getFixtureB().isSensor())
            return;

        Pair p = extractPair(contact);

        if (p.a instanceof Entity ea && p.b instanceof Entity eb) {
            float impactForce = impulse.getNormalImpulses()[0];
            WorldManifold manifold = contact.getWorldManifold();
            Vector2 impactPoint = manifold.getPoints()[0].cpy();

            for (ICollisionListener l : listeners) {
                l.onImpact(ea, eb, impactForce, impactPoint);
            }
        }
    }

    /**
     * Helper Methods
     */

    private Pair extractPair(Contact contact) {
        Object a = contact.getFixtureA().getBody().getUserData();
        Object b = contact.getFixtureB().getBody().getUserData();

        return new Pair(a, b);
    }

}
