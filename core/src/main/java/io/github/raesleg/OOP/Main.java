package io.github.raesleg.OOP;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Main - Application entry point that uses the Scene Management System.
 * 
 * This class follows the Anti-God Class Rule:
 * - Acts as COORDINATOR ONLY
 * - Only holds SceneManager and SpriteBatch
 * - Does NOT hold EntityManager, CollisionManager, or MovementManager
 * - All game logic lives in the Scenes (Scene Sovereignty principle)
 * 
 * ARCHITECTURAL NOTES (SOLID Principles):
 * - Single Responsibility: Only coordinates LibGDX lifecycle with SceneManager
 * - Dependency Inversion: SpriteBatch is passed to SceneManager (Dependency
 * Injection)
 */
public class Main extends ApplicationAdapter {

    /* Private Variables - Only what's allowed by Anti-God Class Rule */
    private SpriteBatch batch;
    private SceneManager sceneManager;

    @Override
    public void create() {
        // Create the shared SpriteBatch (owned by this coordinator)
        batch = new SpriteBatch();

        // Create SceneManager with dependency injection of SpriteBatch
        sceneManager = new SceneManager(batch);

        // Start with the StartScene (main menu)
        sceneManager.push(new StartScene());

        Gdx.app.log("Main", "Game initialized with Scene Management System");
    }

    @Override
    public void render() {
        // Get delta time
        float deltaTime = Gdx.graphics.getDeltaTime();

        // Delegate all logic to SceneManager
        // SceneManager routes to the correct scene (top of stack)
        sceneManager.update(deltaTime);
        sceneManager.render();
    }

    @Override
    public void resize(int width, int height) {
        // Delegate resize to SceneManager
        sceneManager.resize(width, height);
    }

    @Override
    public void pause() {
        // LibGDX pause (app goes to background on mobile)
        // Could pause the current scene if needed
    }

    @Override
    public void resume() {
        // LibGDX resume (app returns to foreground on mobile)
        // Could resume the current scene if needed
    }

    @Override
    public void dispose() {
        // Dispose SceneManager (cascades to all scenes and their managers)
        if (sceneManager != null) {
            sceneManager.dispose();
        }

        // Dispose the SpriteBatch (owned by this coordinator)
        if (batch != null) {
            batch.dispose();
        }

        Gdx.app.log("Main", "Game disposed - all resources cleaned up");
    }
}
