package io.github.raesleg.engine.physics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.ContactListener;

public interface IPhysics {

    enum BodyType { STATIC, DYNAMIC, KINEMATIC }

    void step(float deltaTime);
    void setContactListener(ContactListener listener);

    PhysicsBody createBody(BodyType type, float xM, float yM, float halfW, float halfH, float density,
                    float friction, boolean isSensor, Object userData);

    Vector2 getGravity();

    void destroy(PhysicsBody body);
    void dispose();    
}
