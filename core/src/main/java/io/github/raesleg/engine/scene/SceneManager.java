package io.github.raesleg.engine.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.raesleg.engine.io.IOManager;

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
 * - Dependency Injection: Holds the shared IOManager and injects it into
 * every scene via {@link Scene#setIOManager(IOManagerr)} so no scene
 * ever creates its own instance.
 * - Does NOT hold references to EntityManager, CollisionManager, or
 * MovementManager
 * - Each Scene owns its own managers (Scene Sovereignty principle)
 */
public class SceneManager {

    /* Private Variables */
    private final Stack<Scene> sceneStack;
    private final SpriteBatch batch;
    private final IOManager ioManager;

    /* Constructor */
    /**
     * Creates a new SceneManager with the provided SpriteBatch and shared
     * IOManager.
     */

    public SceneManager(SpriteBatch batch, IOManager ioManager) {
        this.sceneStack = new Stack<>();
        this.batch = batch;
        this.ioManager = ioManager;
    }

    /**
     * Pushes a new scene onto the stack.
     * The current top scene is paused (not disposed).
     * The new scene becomes the active scene receiving input and render calls.
     */

    public void push(Scene scene) {
        if (!sceneStack.isEmpty()) {
            sceneStack.peek().pause();
        }
        scene.setSceneManager(this);
        scene.setIOManager(ioManager);

        sceneStack.push(scene);

        ioManager.pushInputContext(); // input device binding

        scene.show();
        scene.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
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

            ioManager.popInputContext();

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

        ioManager.resetInputContexts();
        ioManager.pushInputContext();

        // Push the new scene as the base
        scene.setSceneManager(this);
        scene.setIOManager(ioManager);
        sceneStack.push(scene);
        scene.show();
        scene.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
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
}
