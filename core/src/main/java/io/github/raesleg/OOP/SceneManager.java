package io.github.raesleg.OOP;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.Stack;

/**
 * SceneManager - Manages game scenes using a Stack-based Finite State Machine.
 * 
 * This class is responsible for:
 * - Managing a stack of scenes (push/pop/set operations)
 * - Routing render and update calls to the top scene
 * - Routing input events ONLY to the top scene (Input Focus Rule)
 * - Properly disposing scenes when removed from the stack
 * 
 * ARCHITECTURAL NOTES (Following SOLID Principles):
 * - Single Responsibility: Only manages scene lifecycle and transitions
 * - Does NOT hold references to EntityManager, CollisionManager, or
 * MovementManager
 * - Each Scene owns its own managers (Scene Sovereignty principle)
 */
public class SceneManager {

    /* Private Variables */
    private final Stack<Scene> sceneStack;
    private final SpriteBatch batch;

    /* Constructor */
    /**
     * Creates a new SceneManager with the provided SpriteBatch.
     * 
     * @param batch The SpriteBatch used for rendering (owned by CoreEngine/Main)
     */
    public SceneManager(SpriteBatch batch) {
        this.sceneStack = new Stack<>();
        this.batch = batch;
    }

    /* Public Functions */

    /**
     * Pushes a new scene onto the stack.
     * The current top scene is paused (not disposed).
     * The new scene becomes the active scene receiving input and render calls.
     * 
     * @param scene The scene to push onto the stack
     */
    public void push(Scene scene) {
        if (!sceneStack.isEmpty()) {
            sceneStack.peek().pause();
        }
        scene.setSceneManager(this);
        sceneStack.push(scene);
        scene.show();
    }

    /**
     * Removes the top scene from the stack.
     * The removed scene is disposed.
     * The scene below it is resumed and becomes active.
     */
    public void pop() {
        if (!sceneStack.isEmpty()) {
            Scene removedScene = sceneStack.pop();
            removedScene.hide();
            removedScene.dispose();

            if (!sceneStack.isEmpty()) {
                sceneStack.peek().resume();
            }
        }
    }

    /**
     * Clears the entire stack and sets a new base scene.
     * All existing scenes are disposed.
     * Use this for major transitions like Main Menu -> Game.
     * 
     * @param scene The new base scene
     */
    public void set(Scene scene) {
        // Dispose all scenes in the stack
        while (!sceneStack.isEmpty()) {
            Scene removedScene = sceneStack.pop();
            removedScene.hide();
            removedScene.dispose();
        }

        // Push the new scene as the base
        scene.setSceneManager(this);
        sceneStack.push(scene);
        scene.show();
    }

    /**
     * Updates the top scene on the stack.
     * Only the top scene receives update calls (Input Focus Rule).
     * 
     * @param deltaTime Time elapsed since last frame
     */
    public void update(float deltaTime) {
        if (!sceneStack.isEmpty()) {
            sceneStack.peek().update(deltaTime);
        }
    }

    /**
     * Renders scenes on the stack.
     * If the top scene is transparent (like a pause overlay),
     * the scene below it is rendered first.
     */
    public void render() {
        if (sceneStack.isEmpty()) {
            return;
        }

        Scene topScene = sceneStack.peek();

        // If top scene is transparent and there's a scene below, render that first
        if (topScene.isTransparent() && sceneStack.size() > 1) {
            Scene sceneBelow = sceneStack.get(sceneStack.size() - 2);
            sceneBelow.render(batch);
        }

        // Always render the top scene
        topScene.render(batch);
    }

    /**
     * Handles input for the top scene only.
     * Background scenes (paused) do not receive input events.
     */
    public void handleInput() {
        if (!sceneStack.isEmpty()) {
            sceneStack.peek().handleInput();
        }
    }

    /**
     * Resizes all scenes in the stack.
     * 
     * @param width  New screen width
     * @param height New screen height
     */
    public void resize(int width, int height) {
        for (Scene scene : sceneStack) {
            scene.resize(width, height);
        }
    }

    /**
     * Disposes all scenes and clears the stack.
     * Call this when the game is shutting down.
     */
    public void dispose() {
        while (!sceneStack.isEmpty()) {
            Scene scene = sceneStack.pop();
            scene.hide();
            scene.dispose();
        }
    }

    /**
     * Returns the current top scene.
     * 
     * @return The scene at the top of the stack, or null if empty
     */
    public Scene getCurrentScene() {
        return sceneStack.isEmpty() ? null : sceneStack.peek();
    }

    /**
     * Returns the SpriteBatch for rendering.
     * 
     * @return The SpriteBatch instance
     */
    public SpriteBatch getBatch() {
        return batch;
    }

    /**
     * Returns the number of scenes currently on the stack.
     * 
     * @return Stack size
     */
    public int getStackSize() {
        return sceneStack.size();
    }

    /**
     * Checks if the stack is empty.
     * 
     * @return true if no scenes are on the stack
     */
    public boolean isEmpty() {
        return sceneStack.isEmpty();
    }
}
