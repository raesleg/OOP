package io.github.raesleg.demo;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.raesleg.engine.entity.IExpirable;
import io.github.raesleg.engine.entity.TextureObject;
import io.github.raesleg.engine.physics.PhysicsBody;

/**
 * NPCCar — Non-player traffic vehicle entity.
 * <p>
 * NPC cars are part of the scrolling road environment. Unlike player-controlled
 * entities, NPC cars:
 * <ul>
 * <li>Move with the road scroll (don't use MovableEntity)</li>
 * <li>Have kinematic Box2D bodies (create real collisions but move manually)</li>
 * <li>Are positioned relative to the player's speed (road scroll offset)</li>
 * <li>Implement IExpirable for automatic cleanup when off-screen</li>
 * </ul>
 * <p>
 * <b>Design Pattern:</b> This demonstrates <b>Composition over Inheritance</b>
 * — NPCCar extends TextureObject directly rather than MovableEntity, because
 * it doesn't need physics-driven movement. It has a kinematic PhysicsBody for
 * collision detection AND physical response.
 * <p>
 * <b>Scalability:</b> Easy to add different NPC car types (truck, bus, sports car)
 * by extending this class or using a factory pattern.
 */
public class NPCCar extends TextureObject implements IExpirable {
    
    private final PhysicsBody body;
    private final int laneIndex; // 0, 1, or 2 (left, center, right)
    private final float relativeY; // Initial Y position relative to scroll
    private boolean expired; // Marks this NPC for removal
    
    /**
     * Creates an NPC car at a specific lane and scroll position.
     * 
     * @param filename Texture asset path (e.g., "npc_car.png")
     * @param laneIndex Lane number (0=left, 1=center, 2=right)
     * @param relativeY Y position relative to current scroll offset
     * @param width Car width in pixels
     * @param height Car height in pixels
     * @param body PhysicsBody (should be kinematic for collision response)
     */
    public NPCCar(String filename, int laneIndex, float relativeY, 
                  float width, float height, PhysicsBody body) {
        // Start position will be updated every frame based on scroll
        super(filename, 0, 0, width, height);
        
        this.laneIndex = laneIndex;
        this.relativeY = relativeY;
        this.body = body;
        this.expired = false;
        
        // Link this entity to the physics body for collision detection
        body.setUserData(this);
    }
    
    /**
     * Updates NPC car position based on road scroll offset.
     * <p>
     * Called every frame by NPCCarSpawner. This makes NPC cars "move with
     * the road" — as the player drives forward (increasing scroll offset),
     * NPC cars scroll backward relative to the screen.
     * 
     * For kinematic bodies, we need to update the physics body position
     * using Box2D's transform system.
     * 
     * @param scrollOffset Current road scroll offset (negative = player moving forward)
     */
    public void updatePosition(float scrollOffset, float screenHeight) {
        // Calculate lane X position (same as RoadRenderer lane positioning)
        float laneX = RoadRenderer.ROAD_LEFT + (laneIndex + 0.5f) * RoadRenderer.ROAD_WIDTH / 3f;
        
        // Calculate Y position based on scroll
        // relativeY is the car's "fixed" position in the world
        // scrollOffset moves the entire world up/down
        float screenY = relativeY + scrollOffset;
        
        // Update visual position (centered on lane)
        setX(laneX - getW() / 2f);
        setY(screenY);
        
        // Sync kinematic body position (convert pixels to meters)
        float bodyX = laneX / io.github.raesleg.engine.Constants.PPM;
        float bodyY = (screenY + getH() / 2f) / io.github.raesleg.engine.Constants.PPM;
        
        // Update kinematic body position manually
        if (body != null) {
            body.setPosition(bodyX, bodyY);
        }
    }
    
    // ═══════════════════════════════════════════════════════════
    // IExpirable Implementation
    // ═══════════════════════════════════════════════════════════
    
    @Override
    public boolean isExpired() {
        return expired;
    }
    
    /**
     * Manually marks this NPC as expired (used by NPCCarSpawner.clearAll()).
     * EntityManager will automatically remove it on the next update.
     */
    public void markExpired() {
        expired = true;
    }
    
    // ═══════════════════════════════════════════════════════════
    // Accessors
    // ═══════════════════════════════════════════════════════════
    
    @Override
    public void draw(SpriteBatch batch) {
        if (getTexture() != null) {
            // Standard texture rendering
            batch.draw(
                getTexture(),
                getX(),
                getY(),
                getW(),
                getH()
            );
        }
    }
    
    @Override
    public void dispose() {
        // Texture is disposed by TextureObject parent
        // PhysicsBody disposal is handled by PhysicsWorld
        if (body != null) {
            body.destroy();
        }
    }
    
    // ═══════════════════════════════════════════════════════════
    // Accessors
    // ═══════════════════════════════════════════════════════════
    
    public PhysicsBody getPhysicsBody() {
        return body;
    }
    
    public int getLaneIndex() {
        return laneIndex;
    }
    
    public float getRelativeY() {
        return relativeY;
    }
}