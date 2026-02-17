package io.github.raesleg.engine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.Box2D;

import io.github.raesleg.demo.Keyboard;
import io.github.raesleg.demo.StartScene;
import io.github.raesleg.engine.io.IOManager;
import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.scene.SceneManager;
import io.github.raesleg.engine.sound.SoundManager;

public class GameMaster extends ApplicationAdapter {

    private SpriteBatch batch;
    private SceneManager sceneManager;
    private IOManager ioManager;

    @Override
    public void create() {
        Box2D.init();
        batch = new SpriteBatch();

        // create devices
        SoundDevice sound = new SoundManager();
        Keyboard keyboard = new Keyboard();
        
        // single IOManager instance — injected into every Scene by SceneManager
        // io manager many inputs + one output
        ioManager = new IOManager(sound);
        ioManager.addInput(keyboard);

        // Start with the StartScene (main menu)
        sceneManager = new SceneManager(batch, ioManager);
        sceneManager.push(new StartScene());
        Gdx.app.log("Main", "Game initialized with Scene Management System");
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        ioManager.update(); //trigger keybinds

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
        ioManager.dispose();
        batch.dispose();
        Gdx.app.log("Main", "Game disposed - all resources cleaned up");
    }
}
