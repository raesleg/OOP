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
import com.badlogic.gdx.audio.Music;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.movement.AIControlled;
import io.github.raesleg.engine.movement.MovementManager;
import io.github.raesleg.engine.movement.UserControlled;
import io.github.raesleg.engine.collision.CollisionManager;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.entity.Shape;
import io.github.raesleg.engine.entity.Surfaces;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.physics.IPhysics;
import io.github.raesleg.engine.physics.PhysicsWorld;
import io.github.raesleg.engine.scene.Scene;

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
    private ArrayList<Shape> zones = new ArrayList<>();

    private float worldW = VIRTUAL_WIDTH / Constants.PPM;
    private float worldH = VIRTUAL_HEIGHT / Constants.PPM;

    private float zoneW = worldW * 0.12f;
    private float zoneH = worldH * 0.45f;

    // Background music purposes
    private Music bgm;
    // Condition to demo sound effect when object is moving or not moving
    private boolean isMoving = false;

    public GameScene() {
        super();
        this.isPaused = false;
        this.gameTime = 0f;
    }

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

        // Initialize sound manager and load sounds in the GameScene
        soundManager.addSound("menu", "uiMenu_sound.wav"); // Add menu navigation sound
        soundManager.addSound("selected", "uiSelected_sound.wav"); // Add selection sound
        soundManager.addSound("move", "moving_sound.wav"); // Add moving object sound
        soundManager.addSound("explosion", "collide_sound.wav"); // Add collision sound

        GameCollisionHandler handler = new GameCollisionHandler(entityManager, soundManager);
        new CollisionManager(physics, handler);

        // test entities
        bucket = new MovableEntity(
                physics,
                "bucket.png",
                200,
                200,
                64f,
                64f,
                new UserControlled(ioManager.getInput()),
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

        // Start background music
        bgm = Gdx.audio.newMusic(Gdx.files.internal("bgm.ogg"));
        bgm.setLooping(true);
        bgm.setVolume(0.2f); // Set volume to 20%
        bgm.play();
    }

    @Override
    public void handleInput(float deltaTime) {
        if (controls.isPause(deltaTime)) {
            sceneManager.push(new PauseScene());
            return;
        }

        // Press M to mute/unmute all sounds
        if (controls.isMute(deltaTime)) {
            soundManager.toggleMute();

            // Stop all the sound when is muted
            if (soundManager.isMuted()) {
                soundManager.stopSound("move"); // stop moving sound when the game is muted
                isMoving = false; // reset moving state
                if (bgm != null) bgm.setVolume(0f);
            } else {
                if (bgm != null) bgm.setVolume(0.2f);
            }
            
            // If currently moving, restart loop immediately
            Vector2 v = bucket.getPhysicsBody().getVelocity();
            boolean objMoving = Math.abs(v.x) > 0.05f || Math.abs(v.y) > 0.05f;

            if (objMoving) {
                soundManager.loopSound("move");
                isMoving = true;
            }

        }

    }

    @Override
    public void update(float deltaTime) {
        if (isPaused) {
            soundManager.stopSound("move"); // Stop moving sound if paused
            return;
        }
        gameTime += deltaTime;

        handleInput(deltaTime);
        entityManager.update(deltaTime);
        movementManager.update(deltaTime);

        // Demo: Play moving sound effect when object is moving
        Vector2 velocity = bucket.getPhysicsBody().getVelocity();
        boolean objMoving =
            Math.abs(velocity.x) > 0.05f ||
            Math.abs(velocity.y) > 0.05f;

        if (objMoving && !isMoving && !soundManager.isMuted()) {
            soundManager.loopSound("move");
            isMoving = true;
        } 
        
        if (!objMoving || isMoving && soundManager.isMuted()) {
            soundManager.stopSound("move");
            isMoving = false;
        }
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

        String muteText = ioManager.getSound().isMuted() ? "M to unmute" : "M to mute";
        font.draw(batch,
                "Use WASD/Arrows to move | ESC to pause | " + muteText,
                10, VIRTUAL_HEIGHT - 45);
        batch.end();
    }

    @Override
    public void pause() {
        isPaused = true;

        if (bgm != null) {
            bgm.pause();
        }

        Gdx.app.log("GameScene", "Scene paused - Game state preserved");
    }

    @Override
    public void resume() {
        isPaused = false;

        if (bgm != null) {
            bgm.play();
        }

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

        if (bgm != null) {
            bgm.stop();
            bgm.dispose();
        }


        Gdx.app.log("GameScene", "Scene disposed - All managers and resources cleaned up");
    }
}
