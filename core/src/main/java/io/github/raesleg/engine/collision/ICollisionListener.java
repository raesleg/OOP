package io.github.raesleg.engine.collision;

import com.badlogic.gdx.math.Vector2;
import io.github.raesleg.engine.entity.Entity;

/*
    Interface for game-specific collision logic
    - CollisionManager detects collisions via Box2D,
    - notifies implementations of this interface to handle game-specific responses
*/

public interface ICollisionListener {
    // called when two entities start touching
    void onCollisionBegin(Entity entityA, Entity entityB);

    // called when two entities stop touching
    void onCollisionEnd(Entity entityA, Entity entityB);

    // called after physics resolution with impact force data - used for impact-based effects
    void onImpact(Entity entityA, Entity entityB, float impactForce, Vector2 impactPoint);

}
