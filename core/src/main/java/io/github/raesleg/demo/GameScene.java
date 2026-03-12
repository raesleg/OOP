package io.github.raesleg.demo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.audio.Music;

import io.github.raesleg.engine.movement.MovementManager;
import io.github.raesleg.engine.movement.MovementModel;
import io.github.raesleg.engine.movement.UserControlled;
import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.collision.CollisionManager;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.engine.physics.PhysicsWorld;
import io.github.raesleg.engine.scene.Scene;

public class GameScene extends Scene {

    // HUD
    private DashboardUI dashboard;

    // Game state
    private boolean isPaused;
    private float gameTime;
    private int score;
    private int rulesBroken;

    // Simulated forward speed (KM/H) and road scroll
    private static final float MAX_SPEED = 200f;
    private static final float ACCELERATION = 60f;
    private static final float BRAKE_RATE = 80f;
    private static final float SCROLL_FACTOR = 2.0f;
    private static final float LEVEL_LENGTH = 50000f;
    private float simulatedSpeed;
    private float scrollOffset;

    // Box2D physics engine
    private PhysicsWorld world;

    // Player car (bucket.png placeholder)
    private MovableEntity playerCar;

    // Road rendering
    private ShapeRenderer shapeRenderer;
    private RoadRenderer roadRenderer;

    // World size in metres (pixels / PPM)
    private float w = VIRTUAL_WIDTH / Constants.PPM;
    private float h = VIRTUAL_HEIGHT / Constants.PPM;

    // Audio
    private SoundDevice sound;
    private Music bgm;

    public GameScene() {
        super();
        this.isPaused = false;
        this.gameTime = 0f;
        this.score = 0;
        this.rulesBroken = 0;
        this.simulatedSpeed = 0f;
        this.scrollOffset = 0f;
    }

    @Override
    protected Viewport createViewport(OrthographicCamera cam) {
        // ExtendViewport keeps aspect ratio without cropping, shows more world on
        // bigger screens
        return new ExtendViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, cam);
    }

    @Override
    public void show() {
        shapeRenderer = new ShapeRenderer();
        roadRenderer = new RoadRenderer(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        dashboard = new DashboardUI(getUiViewport());

        /* Physics world setup: zero gravity (top-down) */
        world = new PhysicsWorld(new Vector2(0, 0));

        sound = getIOManager().getSound();

        /* Managers */
        setEntityManager(new EntityManager());
        setMovementManager(new MovementManager(world, getEntityManager()));
        GameCollisionHandler handler = new GameCollisionHandler(getEntityManager(), sound);
        setCollisionManager(new CollisionManager(world, handler));

        /* Road boundary walls (in metres) */
        float t = 0.2f;
        float roadLeftM = RoadRenderer.ROAD_LEFT / Constants.PPM;
        float roadRightM = RoadRenderer.ROAD_RIGHT / Constants.PPM;

        // Left road edge
        world.createBody(BodyDef.BodyType.StaticBody,
                roadLeftM - t / 2f, h / 2f, t / 2f, h / 2f, 0, 0.4f, false, null);
        // Right road edge
        world.createBody(BodyDef.BodyType.StaticBody,
                roadRightM + t / 2f, h / 2f, t / 2f, h / 2f, 0, 0.4f, false, null);
        // Bottom wall
        world.createBody(BodyDef.BodyType.StaticBody,
                w / 2f, -t / 2f, w / 2f, t / 2f, 0, 0.4f, false, null);
        // Top wall
        world.createBody(BodyDef.BodyType.StaticBody,
                w / 2f, h + t / 2f, w / 2f, t / 2f, 0, 0.4f, false, null);

        /* Player car — center lane, near bottom */
        float carPixelX = RoadRenderer.ROAD_LEFT + RoadRenderer.ROAD_WIDTH / 2f;
        float carPixelY = 100f;
        float carW = 64f;
        float carH = 64f;

        PhysicsBody carBody = world.createBody(
                BodyDef.BodyType.DynamicBody,
                carPixelX / Constants.PPM,
                (carPixelY + carH / 2f) / Constants.PPM,
                (carW / Constants.PPM) / 2f,
                (carH / Constants.PPM) / 2f,
                1f, 0.3f, false, null);

        MovementModel carMovement = new FrictionMovement(MotionTuning.DEFAULT);

        /* Input bindings */
        Keyboard kb = getIOManager().getInputs(Keyboard.class);
        UserControlled user = new UserControlled(kb);

        // Left / right steering (physics-driven via ControlSource)
        kb.bindAction(Input.Keys.A, Constants.LEFT);
        kb.bindAction(Input.Keys.LEFT, Constants.LEFT);
        kb.bindAction(Input.Keys.D, Constants.RIGHT);
        kb.bindAction(Input.Keys.RIGHT, Constants.RIGHT);

        // Accelerate / brake — drives simulated speed, NOT physics vertical movement
        kb.bindAction(Input.Keys.W, "ACCEL");
        kb.bindAction(Input.Keys.UP, "ACCEL");
        kb.bindAction(Input.Keys.S, "BRAKE");
        kb.bindAction(Input.Keys.DOWN, "BRAKE");

        kb.bindAction(Input.Keys.SPACE, Constants.ACTION);
        kb.addBind(Input.Keys.ESCAPE, this::openPause, true);
        kb.addBind(Input.Keys.M, this::toggleMute, true);

        /* Player entity (bucket.png as car placeholder) */
        playerCar = new MovableEntity(
                "car_game_8bit.png",
                carPixelX - carW / 2f, carPixelY,
                carW, carH,
                user, carMovement, carBody);

        getEntityManager().addEntity(playerCar);

        // Background music
        bgm = Gdx.audio.newMusic(Gdx.files.internal("bgm.ogg"));
        bgm.setLooping(true);
        bgm.setVolume(0.2f);
        bgm.play();

        // Scene-specific sounds
        sound.addSound("move", "moving_sound.wav");
        sound.addSound("explosion", "collide_sound.wav");
    }

    @Override
    public void update(float deltaTime) {
        if (isPaused) {
            sound.stopSound("move");
            return;
        }

        gameTime += deltaTime;

        // Speed control via action bindings (W / UP = accelerate, S / DOWN = brake)
        Keyboard kb = getIOManager().getInputs(Keyboard.class);
        if (kb.isHeld("ACCEL")) {
            simulatedSpeed = Math.min(MAX_SPEED, simulatedSpeed + ACCELERATION * deltaTime);
        } else if (kb.isHeld("BRAKE")) {
            simulatedSpeed = Math.max(0f, simulatedSpeed - BRAKE_RATE * deltaTime);
        }

        // Advance road scroll
        scrollOffset += simulatedSpeed * SCROLL_FACTOR * deltaTime;

        getEntityManager().update(deltaTime);
        getMovementManager().update(deltaTime);

        // Dashboard updates
        score = (int) (gameTime * 10f);
        float progress = Math.min(1f, scrollOffset / LEVEL_LENGTH);

        dashboard.onScoreUpdated(score);
        dashboard.onSpeedChanged((int) simulatedSpeed);
        dashboard.onProgressUpdated(progress);
        dashboard.act(deltaTime);

        updateMoveLoop(playerCar.isMoving());
    }

    @Override
    public void render(SpriteBatch batch) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Apply world viewport
        getViewport().apply();
        getCamera().update();
        shapeRenderer.setProjectionMatrix(getCamera().combined);
        batch.setProjectionMatrix(getCamera().combined);

        // Draw road background (grass, asphalt, scrolling lane dashes)
        roadRenderer.draw(shapeRenderer, scrollOffset);

        // Draw entities (player car)
        batch.begin();
        getEntityManager().render(batch);
        batch.end();

        // HUD overlay (DashboardUI owns its Stage / UI viewport)
        dashboard.draw();
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
    public void dispose() {
        dashboard.dispose();
        shapeRenderer.dispose();
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
        updateMoveLoop(playerCar.isMoving());
    }
}
