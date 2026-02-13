// package io.github.raesleg.engine.physics;

// import com.badlogic.gdx.physics.box2d.BodyDef;
// import com.badlogic.gdx.physics.box2d.ContactListener;

// public class PhysicsManager {

//     private IPhysics world;

//     public PhysicsManager(IPhysics world) {
//         this.world = world;
//     }

//     public void step(float deltaTime) {
//         world.step(deltaTime);
//     }

//     public void setContactListener(ContactListener listener) {
//         world.setContactListener(listener);
//     }

//     public PhysicsBody createBody(
//             BodyDef.BodyType type,
//             float xM, float yM,
//             float halfW, float halfH,
//             float density,
//             float friction,
//             boolean isSensor,
//             Object userData) {

//         return world.createBody(type, xM, yM, halfW, halfH, density, friction, isSensor, userData);
//     }

//     public void destroy(PhysicsBody body) {
//         world.destroy(body);
//     }

//     public void dispose() {
//         world.dispose();
//     }    
// }
