package io.github.raesleg.demo;

import java.util.ArrayList;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.movement.AIControlled;
import io.github.raesleg.engine.movement.IOManager;
import io.github.raesleg.engine.movement.MovementManager;
import io.github.raesleg.engine.movement.UserControlled;
import io.github.raesleg.engine.collision.CollisionManager;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.entity.Shape;
import io.github.raesleg.engine.entity.Surfaces;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.physics.IPhysics;
import io.github.raesleg.engine.physics.PhysicsWorld;
import io.github.raesleg.engine.scene.PauseScene;
import io.github.raesleg.engine.scene.Scene;
import io.github.raesleg.engine.sound.SoundManager;

public class GameScene extends Scene {

    private BitmapFont font;

    // Game state
    private boolean isPaused;
    private float gameTime;

    // movable entities implementing box2d physics engine
    private PhysicsWorld physicsWorld;
    private IPhysics physics;
    private MovableEntity bucket;
    private MovableEntity droplet;

    // movement — use Constants.PPM for single source of truth
    private ShapeRenderer shapeRenderer;
    private final ArrayList<Shape> zones = new ArrayList<>();

    // Sound manager for menu navigation and selection sounds
    private SoundManager soundManager;

    private float worldW = VIRTUAL_WIDTH / Constants.PPM;
    private float worldH = VIRTUAL_HEIGHT / Constants.PPM;

    private float zoneW = worldW * 0.12f;
    private float zoneH = worldH * 0.45f;

    public GameScene() {
        super();
        this.isPaused = false;
        this.gameTime = 0f;
    }

    /**
     * GameScene uses an {@link ExtendViewport} so the visible world area
     * grows when the window is enlarged, rather than stretching or
     * letter-boxing.
     */
    @Override
    protected Viewport createViewport(OrthographicCamera cam) {
        return new ExtendViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, cam);
    }

    @Override
    public void show() {
        // Initialize rendering resources
        font = new BitmapFont();
        font.setColor(Color.WHITE);

        // out of bound walls — use virtual resolution for consistent bounds
        physicsWorld = new PhysicsWorld(new Vector2(0, 0));

        physics = physicsWorld; //physicsWorld implements IPhysics

        physicsWorld.createBoundsPixels((int) VIRTUAL_WIDTH, (int) VIRTUAL_HEIGHT, Constants.PPM);

        // create friction zone, forces from box2d
        physicsWorld.createMotionZone(worldW * 0.65f, worldH * 0.5f, zoneW * 0.5f, zoneH * 0.5f,
                MotionTuning.LOW_TRACTION);
        physicsWorld.createMotionZone(worldW * 0.35f, worldH * 0.5f, zoneW * 0.5f, zoneH * 0.5f,
                MotionTuning.HIGH_FRICTION);

        shapeRenderer = new ShapeRenderer();

        // low friction
        zones.add(new Surfaces(
                (worldW * 0.65f - zoneW * 0.5f) * Constants.PPM,
                (worldH * 0.5f - zoneH * 0.5f) * Constants.PPM,
                zoneW * Constants.PPM,
                zoneH * Constants.PPM,
                Color.BLUE));

        // high friction
        zones.add(new Surfaces(
                (worldW * 0.35f - zoneW * 0.5f) * Constants.PPM,
                (worldH * 0.5f - zoneH * 0.5f) * Constants.PPM,
                zoneW * Constants.PPM,
                zoneH * Constants.PPM,
                Color.RED // red
        ));

        entityManager = new EntityManager();
        movementManager = new MovementManager(physics, entityManager);
        GameCollisionHandler handler = new GameCollisionHandler(entityManager);
        new CollisionManager(physics, handler);

        // test entities
        bucket = new MovableEntity(
                physics,
                "bucket.png",
                200,
                200,
                64f,
                64f,
                new UserControlled(ioManager),
                MotionTuning.DEFAULT);

        droplet = new MovableEntity(
                physics,
                "droplet.png",
                500,
                300,
                64f,
                64f,
                new AIControlled(),
                MotionTuning.DEFAULT);

        entityManager.addEntity(bucket);
        entityManager.addEntity(droplet);

        // Initialize sound manager and load sounds
        soundManager = new SoundManager();
        soundManager.addSound("menu", "uiMenu_sound.wav"); // add navigating option sound
        soundManager.addSound("selected", "uiSelected_sound.wav"); // add select option sound
    }

    @Override
    public void handleInput() {
        if (ioManager.isPauseRequested()) {
            sceneManager.push(new PauseScene());
            return;
        }
    }

    @Override
    public void update(float deltaTime) {
        if (isPaused) {
            return;
        }
        gameTime += deltaTime;

        handleInput();
        entityManager.update(deltaTime);
        movementManager.update(deltaTime);
    }

    @Override
    public void render(SpriteBatch batch) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Apply the viewport so the GL viewport matches & apply camera
        viewport.apply();
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // movement texture zones demo
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Shape z : zones)
            z.draw(shapeRenderer);
        shapeRenderer.end();

        // --- World rendering (ExtendViewport) ---
        batch.begin();
        entityManager.render(batch);
        batch.end();

        // --- HUD rendering (FitViewport — pixel-stable, never distorted) ---
        uiViewport.apply();
        uiCamera.update();
        batch.setProjectionMatrix(uiCamera.combined);

        batch.begin();
        font.getData().setScale(2f);
        font.draw(batch, "Game Time: " + String.format("%.1f", gameTime) + "s", 10, VIRTUAL_HEIGHT - 10);

        // Dynamic key name — stays accurate even after rebinding
        String pauseKey = ioManager.getKeyName(IOManager.PAUSE);
        font.draw(batch, "Use WASD/Arrows to move | " + pauseKey + " to pause", 10, VIRTUAL_HEIGHT - 45);
        batch.end();
    }

    @Override
    public void pause() {
        isPaused = true;
        Gdx.app.log("GameScene", "Scene paused - Game state preserved");
    }

    @Override
    public void resume() {
        isPaused = false;
        Gdx.app.log("GameScene", "Scene resumed - Continuing gameplay");
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        // Player stays in bounds — physics walls are based on VIRTUAL size, not window
        // size
    }

    @Override
    public void dispose() {
        font.dispose();
        shapeRenderer.dispose();
        zones.clear();
        entityManager.dispose();
        physics.dispose();   

        Gdx.app.log("GameScene", "Scene disposed - All managers and resources cleaned up");
    }
}
