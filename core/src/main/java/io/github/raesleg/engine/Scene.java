package io.github.raesleg.engine;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Scene — Abstract base class for all game scenes.
 *
 * <h3>Scene Sovereignty Principle</h3>
 * Each Scene is the absolute owner of its game world and MUST instantiate
 * and own its own private versions of:
 * <ul>
 * <li>EntityManager (Manages entities)</li>
 * <li>CollisionManager (Handles physics)</li>
 * <li>MovementManager (Handles position updates)</li>
 * <li>IOManager (Handles inputs specific to that scene)</li>
 * </ul>
 * When a Scene is disposed, it MUST cascade the {@link #dispose()} call to
 * all its managers.
 *
 * <h3>Viewport Principle</h3>
 * The base Scene owns the Camera and delegates Viewport creation to the
 * {@link #createViewport(OrthographicCamera)} hook so subclasses can choose
 * their own strategy:
 * <ul>
 * <li>{@link com.badlogic.gdx.utils.viewport.FitViewport} — stable UI
 * (default, used by menus/overlays)</li>
 * <li>{@link com.badlogic.gdx.utils.viewport.ExtendViewport} — the world
 * grows when the window grows (used by gameplay scenes)</li>
 * </ul>
 * {@link #resize(int, int)} is implemented here so every subclass gets
 * correct scaling for free. Subclasses may override but <b>must</b> call
 * {@code super.resize(width, height)}.
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

    /** Shared camera — subclasses use this for projection. */
    protected OrthographicCamera camera;

    /** Viewport — handles letterboxing / scaling on resize. */
    protected Viewport viewport;

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
     * <li>Applies the viewport immediately so the camera is centred.</li>
     * </ol>
     */
    public Scene() {
        this.transparent = false;

        camera = new OrthographicCamera();
        viewport = createViewport(camera);
        viewport.apply(true);
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
     * Updates the viewport so the game scales correctly (letterboxing).
     * Subclasses may override for additional work but MUST call
     * {@code super.resize(width, height)}.
     * 
     * @param width  New screen width
     * @param height New screen height
     */
    public void resize(int width, int height) {
        viewport.update(width, height, true);
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
