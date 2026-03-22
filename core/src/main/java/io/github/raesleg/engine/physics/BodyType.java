package io.github.raesleg.engine.physics;

/**
 * Engine-level body type enum that hides Box2D's {@code BodyDef.BodyType}
 * from the game layer. Game code must use this enum instead of importing
 * {@code com.badlogic.gdx.physics.box2d.BodyDef} directly.
 * <p>
 * <b>Facade Pattern:</b> Part of the PhysicsWorld/PhysicsBody facade that
 * encapsulates all Box2D types behind engine abstractions.
 */
public enum BodyType {
    STATIC,
    DYNAMIC,
    KINEMATIC
}
