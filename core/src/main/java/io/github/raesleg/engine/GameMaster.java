package io.github.raesleg.engine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.Box2D;

import io.github.raesleg.engine.movement.IOManager;
import io.github.raesleg.engine.scene.SceneManager;
import io.github.raesleg.engine.scene.StartScene;

public class GameMaster extends ApplicationAdapter {

    private SpriteBatch batch;
    private SceneManager sceneManager;
    private IOManager ioManager;

    @Override
    public void create() {
        Box2D.init();
        batch = new SpriteBatch();
        // Single IOManager instance — injected into every Scene by SceneManager
        ioManager = new IOManager();
        sceneManager = new SceneManager(batch, ioManager);

        // Start with the StartScene (main menu)
        sceneManager.push(new StartScene());
        Gdx.app.log("Main", "Game initialized with Scene Management System");
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        // Update input state ONCE per frame before any scene logic
        ioManager.update();

        // SceneManager routes to the correct scene (top of stack)
        sceneManager.update(deltaTime);
        sceneManager.render();
    }

    @Override
    public void resize(int width, int height) {
        sceneManager.resize(width, height);
    }

    @Override
    public void dispose() {
        sceneManager.dispose();
        batch.dispose();
        Gdx.app.log("Main", "Game disposed - all resources cleaned up");
    }
}
