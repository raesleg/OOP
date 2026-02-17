package io.github.raesleg.engine.scene;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.raesleg.engine.GameMaster;
import io.github.raesleg.engine.collision.CollisionManager;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.io.ControlSource;
import io.github.raesleg.engine.io.IOManager;
import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.movement.MovementManager;

/**
 * Scene — Abstract base class for all game scenes.
 *
 * <h3>Dependency Injection</h3>
 * A single {@link IOManagerr} instance is created by {@link GameMaster} and
 * injected into every Scene via {@link #setIOManager(IOManagerr)}, called
 * automatically by {@link SceneManager}. Scenes must <b>never</b> create
 * their own {@code IOManager}.
 *
 * <h3>Scene Sovereignty Principle</h3>
 * Each Scene is the absolute owner of its game world and MUST instantiate
 * and own its own private versions of:
 * <ul>
 * <li>EntityManager (Manages entities)</li>
 * <li>CollisionManager (Handles physics)</li>
 * <li>MovementManager (Handles position updates)</li>
 * </ul>
 * When a Scene is disposed, it MUST cascade the {@link #dispose()} call to
 * all its managers.
 *
 * <h3>Viewport Principle</h3>
 * The base Scene owns <b>two</b> viewport/camera pairs:
 * <ol>
 * <li><b>World viewport</b> ({@link #viewport} / {@link #camera}) —
 * created by the {@link #createViewport(OrthographicCamera)} hook.
 * Default is {@link FitViewport}; gameplay scenes override to
 * {@link com.badlogic.gdx.utils.viewport.ExtendViewport}.</li>
 * <li><b>UI viewport</b> ({@link #uiViewport} / {@link #uiCamera}) —
 * always a {@link FitViewport} so HUD text is pixel-stable and
 * never distorted by the world viewport.</li>
 * </ol>
 * {@link #resize(int, int)} updates <b>both</b> viewports.
 */
public abstract class Scene {

    /* ── Virtual resolution (design size) ── */
    public static final float VIRTUAL_WIDTH = 1280f;
    public static final float VIRTUAL_HEIGHT = 720f;

    /* Protected Variables — Available to subclasses */
    protected SceneManager sceneManager;
    protected EntityManager entityManager;
    protected MovementManager movementManager;
    protected CollisionManager collisionManager;
    protected IOManager ioManager;
    protected ControlSource controls;
    protected SoundDevice soundManager;

    /** Shared camera — subclasses use this for projection. */
    protected OrthographicCamera camera;

    /** Viewport — handles letterboxing / scaling on resize. */
    protected Viewport viewport;

    /** UI camera — always uses a stable FitViewport for HUD elements. */
    protected OrthographicCamera uiCamera;

    /** UI viewport — FitViewport for pixel-stable HUD rendering. */
    protected Viewport uiViewport;

    /**
     * Whether this scene allows the scene below to be visible (e.g., pause
     * overlay).
     */
    protected boolean transparent;

    /* ── Constructor ── */

    /**
     * Creates a new Scene.
     * <ol>
     * <li>Initialises a shared {@link OrthographicCamera}.</li>
     * <li>Calls {@link #createViewport(OrthographicCamera)} so the subclass
     * can pick the appropriate viewport type.</li>
     * <li>Creates a second camera/viewport pair for UI rendering.</li>
     * <li>Applies both viewports immediately so cameras are centred.</li>
     * </ol>
     */
    public Scene() {
        this.transparent = false;

        camera = new OrthographicCamera();
        viewport = createViewport(camera);
        viewport.apply(true);

        uiCamera = new OrthographicCamera();
        uiViewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, uiCamera);
        uiViewport.apply(true);
    }

    /* ── Viewport factory hook ── */

    /**
     * Called once from the constructor to create the viewport for this scene.
     * <p>
     * The default implementation returns a {@link FitViewport} (letter-boxed,
     * pixel-stable UI). Override in gameplay scenes to return an
     * {@link com.badlogic.gdx.utils.viewport.ExtendViewport} so the player
     * sees more of the world when the window is enlarged.
     *
     * @param cam the camera this viewport should control
     * @return a fully-constructed Viewport (never null)
     */
    protected Viewport createViewport(OrthographicCamera cam) {
        return new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, cam);
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
    public abstract void handleInput(float deltaTime);

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
     * Updates the viewport so the game scales correctly (letterboxing).
     * Subclasses may override for additional work but MUST call
     * {@code super.resize(width, height)}.
     * 
     * @param width  New screen width
     * @param height New screen height
     */
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        uiViewport.update(width, height, true);
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
     * Receives the shared IOManager instance (injected by SceneManager).
     * Scenes must <b>never</b> create their own IOManager.
     *
     */
    public void setIOManager(IOManager ioManager) {
        this.ioManager = ioManager;
        this.soundManager = ioManager.getSound();
        // this.controls = new UserControlled(io.getInput());
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
