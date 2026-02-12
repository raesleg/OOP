package io.github.raesleg.engine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.Box2D;

import io.github.raesleg.demo.StartScene;

public class GameMaster extends ApplicationAdapter {

    private SpriteBatch batch;
    private SceneManager sceneManager;

    @Override
    public void create() {
        Box2D.init();
        batch = new SpriteBatch();
        sceneManager = new SceneManager(batch);

        // Start with the StartScene (main menu)
        sceneManager.push(new StartScene());
        Gdx.app.log("Main", "Game initialized with Scene Management System");
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();
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
