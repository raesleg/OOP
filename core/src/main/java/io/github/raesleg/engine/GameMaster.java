package io.github.raesleg.engine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.Box2D;

import io.github.raesleg.engine.io.IOManager;
import io.github.raesleg.engine.io.InputDevice;
import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.scene.Scene;
import io.github.raesleg.engine.scene.SceneManager;
import io.github.raesleg.engine.sound.SoundManager;

/**
 * GameMaster — Application entry point.
 *
 * Accepts the initial {@link Scene} and {@link InputDevice} via constructor
 * injection so this engine-level class never depends on the demo/game layer
 * (Dependency Inversion Principle). Concrete types are wired by the launcher
 * (the composition root).
 */
public class GameMaster extends ApplicationAdapter {

    private SpriteBatch batch;
    private SceneManager sceneManager;
    private IOManager ioManager;

    private final Scene initialScene;
    private final InputDevice inputDevice;

    /**
     * @param initialScene the first scene to push (e.g. a title screen)
     * @param inputDevice  the input device to register (e.g. keyboard)
     */
    public GameMaster(Scene initialScene, InputDevice inputDevice) {
        this.initialScene = initialScene;
        this.inputDevice = inputDevice;
    }

    @Override
    public void create() {
        Box2D.init();
        batch = new SpriteBatch();

        // create devices
        SoundDevice sound = new SoundManager();

        // Register shared UI sounds once (DRY — avoids duplication across scenes)
        sound.addSound("menu", "uiMenu_sound.wav");
        sound.addSound("selected", "uiSelected_sound.wav");

        // single IOManager instance — injected into every Scene by SceneManager
        // io manager many inputs + one output
        ioManager = new IOManager(sound);
        ioManager.addInput(inputDevice);

        // Start with the injected initial scene
        sceneManager = new SceneManager(batch, ioManager);
        sceneManager.push(initialScene);
        Gdx.app.log("Main", "Game initialized with Scene Management System");
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        ioManager.update(); // trigger keybinds

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
