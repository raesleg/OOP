package io.github.raesleg.engine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.Box2D;

import io.github.raesleg.engine.entity.TextureObject;
import io.github.raesleg.engine.io.IOManager;
import io.github.raesleg.engine.io.InputDevice;
import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.scene.Scene;
import io.github.raesleg.engine.scene.SceneManager;

/**
 * GameMaster — Application entry point.
 *
 * Accepts the initial {@link Scene}, {@link InputDevice}, and
 * {@link SoundDevice} via constructor injection so this engine-level class
 * never depends on the demo/game layer (Dependency Inversion Principle).
 * Concrete types — including which sounds to pre-register — are wired by
 * the launcher (the composition root).
 */
public class GameMaster extends ApplicationAdapter {

    private SpriteBatch batch;
    private SceneManager sceneManager;
    private IOManager ioManager;

    private final Scene initialScene;
    private final InputDevice inputDevice;
    private final SoundDevice soundDevice;

    /**
     * @param initialScene the first scene to push (e.g. a title screen)
     * @param inputDevice  the input device to register (e.g. keyboard)
     * @param soundDevice  a pre-configured sound device (sounds already
     *                     registered by the composition root)
     */
    public GameMaster(Scene initialScene, InputDevice inputDevice, SoundDevice soundDevice) {
        this.initialScene = initialScene;
        this.inputDevice = inputDevice;
        this.soundDevice = soundDevice;
    }

    @Override
    public void create() {
        Box2D.init();
        batch = new SpriteBatch();

        // Register shared UI sounds — done here (after LibGDX init) so Gdx.files
        // is available when SoundEffect loads the asset (DIP: filenames are
        // game-layer knowledge injected at the composition root boundary).
        soundDevice.addSound("menu", "uiMenu_sound.wav");
        soundDevice.addSound("selected", "uiSelected_sound.wav");

        // single IOManager instance — injected into every Scene by SceneManager
        // io manager many inputs + one output
        ioManager = new IOManager(soundDevice);
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
        TextureObject.disposeAllTextures(); // free shared GPU textures
        Gdx.app.log("Main", "Game disposed - all resources cleaned up");
    }
}
