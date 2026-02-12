package io.github.raesleg.engine;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.raesleg.engine.movement.IOManager;
import io.github.raesleg.engine.movement.MovementManager;

/**
 * Scene - Abstract base class for all game scenes.
 * 
 * SCENE SOVEREIGNTY PRINCIPLE:
 * Each Scene is the absolute owner of its game world and MUST instantiate
 * and own its own private versions of:
 * - EntityManager (Manages entities)
 * - CollisionManager (Handles physics)
 * - MovementManager (Handles position updates)
 * - IOManager (Handles inputs specific to that scene)
 * 
 * When a Scene is disposed, it MUST cascade the dispose() call to all its
 * managers.
 * 
 * ARCHITECTURAL NOTES:
 * - Follows Single Responsibility Principle (SRP)
 * - Each scene manages its own lifecycle
 * - Input is handled only when scene is active (top of stack)
 * - No global state - data passed via constructors/setters (Dependency
 * Injection)
 */
public abstract class Scene {

    /* Protected Variables - Available to subclasses */
    protected SceneManager sceneManager;
    protected EntityManager entityManager;
    protected MovementManager movementManager;
    protected CollisionManager collisionManager;
    protected IOManager ioManager;

    /**
     * Whether this scene allows the scene below to be visible (e.g., pause overlay)
     */
    protected boolean transparent;

    /* Constructor */
    /**
     * Creates a new Scene.
     * Subclasses should initialize their managers in show() or constructor.
     */
    public Scene() {
        this.transparent = false;
    }

    /* Abstract Methods - Must be implemented by subclasses */

    /**
     * Called when the scene is first shown or pushed onto the stack.
     * Initialize managers, load assets, and set up the scene here.
     */
    public abstract void show();

    /**
     * Updates the scene logic.
     * Typically calls manager updates in the correct order.
     * 
     * @param deltaTime Time elapsed since last frame in seconds
     */
    public abstract void update(float deltaTime);

    /**
     * Renders the scene.
     * 
     * @param batch The SpriteBatch to use for rendering
     */
    public abstract void render(SpriteBatch batch);

    /**
     * Handles input for this scene.
     * Called only when this scene is at the top of the stack (has input focus).
     */
    public abstract void handleInput();

    /* Lifecycle Methods - Can be overridden */

    /**
     * Called when the scene is hidden (another scene pushed on top).
     * The scene remains in memory but is no longer active.
     */
    public void hide() {
        // Override in subclasses if needed
    }

    /**
     * Called when the scene is paused (another scene pushed on top).
     * Stop any animations or time-sensitive operations.
     */
    public void pause() {
        // Override in subclasses if needed
    }

    /**
     * Called when the scene is resumed (scene above was popped).
     * Restart any paused operations.
     */
    public void resume() {
        // Override in subclasses if needed
    }

    /**
     * Called when the screen is resized.
     * Update viewports, cameras, or UI layouts.
     * 
     * @param width  New screen width
     * @param height New screen height
     */
    public void resize(int width, int height) {
        // Override in subclasses if needed
    }

    /**
     * Called when the scene is removed from the stack.
     * MUST dispose all managers and resources owned by this scene.
     * This is crucial for preventing memory leaks.
     */
    public void dispose() {
        // Subclasses MUST override and dispose their managers
        // Example implementation in subclass:
        // if (entityManager != null) entityManager.dispose();
        // if (ioManager != null) ioManager.dispose();
    }

    /* Getters and Setters */

    /**
     * Sets the SceneManager reference (called by SceneManager).
     * This allows scenes to trigger transitions.
     * 
     * @param sceneManager The SceneManager managing this scene
     */
    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    /**
     * Returns whether this scene is transparent.
     * Transparent scenes allow the scene below to be rendered first.
     * 
     * @return true if transparent (e.g., pause menu overlay)
     */
    public boolean isTransparent() {
        return transparent;
    }

    /**
     * Sets whether this scene is transparent.
     * 
     * @param transparent true to allow scene below to be visible
     */
    protected void setTransparent(boolean transparent) {
        this.transparent = transparent;
    }
}
