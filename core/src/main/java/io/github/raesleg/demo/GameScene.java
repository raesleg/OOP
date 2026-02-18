package io.github.raesleg.demo;

import java.util.ArrayList;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.audio.Music;

import io.github.raesleg.engine.movement.AIControlled;
import io.github.raesleg.engine.movement.MovementManager;
import io.github.raesleg.engine.movement.MovementModel;
import io.github.raesleg.engine.movement.UserControlled;
import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.collision.CollisionManager;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.entity.Shape;
import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.engine.physics.PhysicsWorld;
import io.github.raesleg.engine.scene.Scene;

public class GameScene extends Scene {

    private BitmapFont font;

    // Game state
    private boolean isPaused;
    private float gameTime;

    // Box2d physics engine
    private PhysicsWorld world;

    // Movable entities
    private MovableEntity bucket;
    private MovableEntity droplet;

    // Zone rendering
    private ShapeRenderer shapeRenderer; // draw as colored rect.
    private ArrayList<Shape> zones = new ArrayList<>();

    // World size in meters (pixels/PPM)
    private float w = VIRTUAL_WIDTH / Constants.PPM;
    private float h = VIRTUAL_HEIGHT / Constants.PPM;

    // Zone dimensions in meters (relative to world size)
    private float zoneW = w * 0.12f;
    private float zoneH = h * 0.45f;

    // Audio
    private SoundDevice sound;
    private Music bgm;

    public GameScene() {
        super();
        this.isPaused = false;
        this.gameTime = 0f;
    }

    @Override
    protected Viewport createViewport(OrthographicCamera cam) {
        // ExtendViewport keeps aspect ratio without cropping, shows more world on
        // bigger screens
        return new ExtendViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, cam);
    }

    @Override
    public void show() {
        // Initialize rendering resources
        font = new BitmapFont();
        font.setColor(Color.WHITE);

        shapeRenderer = new ShapeRenderer();

        /* Physics world setup : uses METERS, (0,0) top-down movement */
        world = new PhysicsWorld(new Vector2(0, 0));

        // (injected via abstract scene)
        sound = getIOManager().getSound();

        /* Managers */
        setEntityManager(new EntityManager());
        setMovementManager(new MovementManager(world, getEntityManager()));
        GameCollisionHandler handler = new GameCollisionHandler(getEntityManager(), sound);
        setCollisionManager(new CollisionManager(world, handler));

        // wall thickness, static body use solid bounds
        float t = 0.2f;

        // create 4 booundary walls, static fixtures, not sensors
        world.createBody(BodyDef.BodyType.StaticBody, w / 2, t / 2, w / 2, t / 2, 0, 0.4f, false, null);
        world.createBody(BodyDef.BodyType.StaticBody, w / 2, h + t / 2, w / 2, t / 2, 0, 0.4f, false, null);
        world.createBody(BodyDef.BodyType.StaticBody, t / 2, h / 2, t / 2, h / 2, 0, 0.4f, false, null);
        world.createBody(BodyDef.BodyType.StaticBody, w + t / 2, h / 2, t / 2, h / 2, 0, 0.4f, false, null);

        /* Dynamic bodies creation, passing into movableEntity */
        // game scene controls where/how bodies are spawned
        PhysicsBody bucketBody = world.createBody(
                BodyDef.BodyType.DynamicBody,
                (200 + 64f / 2f) / Constants.PPM,
                (200 + 64f / 2f) / Constants.PPM,
                (64f / Constants.PPM) / 2f,
                (64f / Constants.PPM) / 2f,
                1f, 0.3f, false,
                null);

        PhysicsBody dropletBody = world.createBody(
                BodyDef.BodyType.DynamicBody,
                (500 + 64f / 2f) / Constants.PPM,
                (300 + 64f / 2f) / Constants.PPM,
                (64f / Constants.PPM) / 2f,
                (64f / Constants.PPM) / 2f,
                1f, 0.3f, false,
                null);

        /* Movement models, defining how movement feels (friction) */
        MovementModel bucketMovement = new FrictionMovement(MotionTuning.DEFAULT);
        MovementModel dropletMovement = new FrictionMovement(MotionTuning.DEFAULT);

        /* Motion zones (sensor areas) for collision entry detection */
        MotionZone low = new MotionZone(
                world,
                w * 0.65f, h * 0.5f,
                zoneW * 0.5f, zoneH * 0.5f,
                MotionTuning.LOW_TRACTION,
                Color.BLUE);

        MotionZone high = new MotionZone(
                world,
                w * 0.35f, h * 0.5f,
                zoneW * 0.5f, zoneH * 0.5f,
                MotionTuning.HIGH_FRICTION,
                Color.RED);

        // logging
        System.out.println("LOW zone px: x=" + low.getX() + " y=" + low.getY() +
                " w=" + low.getW() + " h=" + low.getH());

        // collision listener receives Entity–Entity
        getEntityManager().addEntity(low);
        getEntityManager().addEntity(high);
        // stored for debug rendering
        zones.add(low);
        zones.add(high);

        /* Key Binds for Game Scene Implementation */
        Keyboard kb = getIOManager().getInputs(Keyboard.class);
        UserControlled user = new UserControlled(kb);
        AIControlled ai = new AIControlled();

        kb.bindAction(Input.Keys.A, Constants.LEFT);
        kb.bindAction(Input.Keys.LEFT, Constants.LEFT);
        kb.bindAction(Input.Keys.D, Constants.RIGHT);
        kb.bindAction(Input.Keys.RIGHT, Constants.RIGHT);
        kb.bindAction(Input.Keys.W, Constants.UP);
        kb.bindAction(Input.Keys.UP, Constants.UP);
        kb.bindAction(Input.Keys.S, Constants.DOWN);
        kb.bindAction(Input.Keys.DOWN, Constants.DOWN);
        kb.bindAction(Input.Keys.SPACE, Constants.ACTION);

        kb.addBind(Input.Keys.ESCAPE, this::openPause, true);
        kb.addBind(Input.Keys.M, this::toggleMute, true);

        /*
         * Testing game objects
         * - movableEntity have texture, controller, movement model and physicsbody
         * (box2d)
         */
        bucket = new MovableEntity(
                "bucket.png", 200, 200, 64f, 64f,
                user,
                bucketMovement,
                bucketBody);

        droplet = new MovableEntity(
                "droplet.png", 500, 300, 64f, 64f,
                ai,
                dropletMovement,
                dropletBody);

        getEntityManager().addEntity(bucket);
        getEntityManager().addEntity(droplet);

        // Start background music
        bgm = Gdx.audio.newMusic(Gdx.files.internal("bgm.ogg"));
        bgm.setLooping(true);
        bgm.setVolume(0.2f); // Set volume to 20%
        bgm.play();

        // Initialize sound manager and load sounds in the GameScene
        sound.addSound("menu", "uiMenu_sound.wav"); // Add menu navigation sound
        sound.addSound("selected", "uiSelected_sound.wav"); // Add selection sound
        sound.addSound("move", "moving_sound.wav"); // Add moving object sound
        sound.addSound("explosion", "collide_sound.wav"); // Add collision sound
    }

    public void handleInput(float deltaTime) {
    }

    @Override
    public void update(float deltaTime) {
        if (isPaused) {
            sound.stopSound("move"); // Stop moving sound if paused
            return;
        }

        gameTime += deltaTime;

        getEntityManager().update(deltaTime);
        getMovementManager().update(deltaTime);

        // Use bucket.isMoving() — proper encapsulation instead of duplicating velocity
        // check
        updateMoveLoop(bucket.isMoving());
    }

    @Override
    public void render(SpriteBatch batch) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Apply the viewport so the GL viewport matches & apply camera
        getViewport().apply();
        getCamera().update();
        batch.setProjectionMatrix(getCamera().combined);
        shapeRenderer.setProjectionMatrix(getCamera().combined);

        // movement texture zones demo
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Shape z : zones)
            z.draw(shapeRenderer);
        shapeRenderer.end();

        // --- World rendering (ExtendViewport) ---
        batch.begin();
        getEntityManager().render(batch);
        batch.end();

        // --- HUD rendering (FitViewport — pixel-stable, never distorted) ---
        getUiViewport().apply();
        getUiCamera().update();
        batch.setProjectionMatrix(getUiCamera().combined);

        batch.begin();
        font.getData().setScale(2f);
        font.draw(batch, "Game Time: " + String.format("%.1f", gameTime) + "s", 10, VIRTUAL_HEIGHT - 10);

        String muteText = getIOManager().getSound().isMuted() ? "M to unmute" : "M to mute";
        font.draw(batch,
                "Use WASD/Arrows to move | ESC to pause | " + muteText,
                10, VIRTUAL_HEIGHT - 45);
        batch.end();
    }

    @Override
    public void pause() {
        isPaused = true;
        stopMoveLoop();

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
    }

    @Override
    public void dispose() {
        font.dispose();
        shapeRenderer.dispose();
        zones.clear();
        getEntityManager().dispose();
        world.dispose();

        if (bgm != null) {
            bgm.stop();
            bgm.dispose();
        }

        Gdx.app.log("GameScene", "Scene disposed - All managers and resources cleaned up");
    }

    /* Private Helpers for movement SFX loop */
    private void stopMoveLoop() {
        sound.stopSound("move");
    }

    private void updateMoveLoop(boolean objMoving) {
        // if muted or not moving ensure loop is stopped
        if (sound.isMuted() || !objMoving) {
            stopMoveLoop();
            return;
        }
        // if moving and not already looping start it (query sound system — no duplicate
        // state)
        if (!sound.isLooping("move")) {
            sound.loopSound("move");
        }
    }

    /* Private Scene controls */
    private void openPause() {
        getSceneManager().push(new PauseScene());
    }

    private void toggleMute() {
        sound.toggleMute();

        if (bgm != null)
            bgm.setVolume(sound.isMuted() ? 0f : 0.2f);

        // Use bucket.isMoving() — delegates to MovableEntity (Law of Demeter)
        updateMoveLoop(bucket.isMoving());
    }
}
