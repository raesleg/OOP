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
import com.badlogic.gdx.audio.Sound;

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

    // movable entities implementing box2d physics engine
    private PhysicsWorld world;
    // private IPhysics physics;
    private MovableEntity bucket;
    private MovableEntity droplet;

    // movement — use Constants.PPM for single source of truth
    private ShapeRenderer shapeRenderer;
    private ArrayList<Shape> zones = new ArrayList<>();

    private float worldW = VIRTUAL_WIDTH / Constants.PPM;
    private float worldH = VIRTUAL_HEIGHT / Constants.PPM;

    private float zoneW = worldW * 0.12f;
    private float zoneH = worldH * 0.45f;

    private SoundDevice sound;

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
        world = new PhysicsWorld(new Vector2(0, 0));

        sound = ioManager.getSound();

        float w = VIRTUAL_WIDTH / Constants.PPM;
        float h = VIRTUAL_HEIGHT / Constants.PPM;
        float t = 0.2f;

        world.createBody(BodyDef.BodyType.StaticBody, w/2, t/2, w/2, t/2, 0, 0.4f, false, null);
        world.createBody(BodyDef.BodyType.StaticBody, w/2, h+t/2, w/2, t/2, 0, 0.4f, false, null);
        world.createBody(BodyDef.BodyType.StaticBody, t/2, h/2, t/2, h/2, 0, 0.4f, false, null);
        world.createBody(BodyDef.BodyType.StaticBody, w+t/2, h/2, t/2, h/2, 0, 0.4f, false, null);

        // world.createBody(
        //     BodyDef.BodyType.StaticBody,
        //     worldW * 0.65f, worldH * 0.5f,
        //     zoneW * 0.5f, zoneH * 0.5f,
        //     0f, 0f,
        //     true,
        //     MotionTuning.LOW_TRACTION   // userData can be MotionProfile
        // );

        // world.createBody(
        //     BodyDef.BodyType.StaticBody,
        //     worldW * 0.35f, worldH * 0.5f,
        //     zoneW * 0.5f, zoneH * 0.5f,
        //     0f, 0f,
        //     true,
        //     MotionTuning.HIGH_FRICTION
        // );

        PhysicsBody bucketBody = world.createBody(
            BodyDef.BodyType.DynamicBody,
            (200 + 64f/2f) / Constants.PPM,
            (200 + 64f/2f) / Constants.PPM,
            (64f / Constants.PPM) / 2f,
            (64f / Constants.PPM) / 2f,
            1f, 0.3f, false,
            null
        );

        PhysicsBody dropletBody = world.createBody(
            BodyDef.BodyType.DynamicBody,
            (500 + 64f/2f) / Constants.PPM,
            (300 + 64f/2f) / Constants.PPM,
            (64f / Constants.PPM) / 2f,
            (64f / Constants.PPM) / 2f,
            1f, 0.3f, false,
            null
        );

        MovementModel bucketMovement = new FrictionMovement(MotionTuning.DEFAULT);
        MovementModel dropletMovement = new FrictionMovement(MotionTuning.DEFAULT);

        // world.createBoundsPixels((int) VIRTUAL_WIDTH, (int) VIRTUAL_HEIGHT, Constants.PPM);

        // // create friction zone, forces from box2d
        // world.createMotionZone(worldW * 0.65f, worldH * 0.5f, zoneW * 0.5f, zoneH * 0.5f,
        //         MotionTuning.LOW_TRACTION);
        // world.createMotionZone(worldW * 0.35f, worldH * 0.5f, zoneW * 0.5f, zoneH * 0.5f,
        //         MotionTuning.HIGH_FRICTION);

        shapeRenderer = new ShapeRenderer();

        // // low friction
        // zones.add(new Surfaces(
        //         (worldW * 0.65f - zoneW * 0.5f) * Constants.PPM,
        //         (worldH * 0.5f - zoneH * 0.5f) * Constants.PPM,
        //         zoneW * Constants.PPM,
        //         zoneH * Constants.PPM,
        //         Color.BLUE));

        // // high friction
        // zones.add(new Surfaces(
        //         (worldW * 0.35f - zoneW * 0.5f) * Constants.PPM,
        //         (worldH * 0.5f - zoneH * 0.5f) * Constants.PPM,
        //         zoneW * Constants.PPM,
        //         zoneH * Constants.PPM,
        //         Color.RED // red
        // ));

        entityManager = new EntityManager();
        movementManager = new MovementManager(world, entityManager);


        MotionZone low = new MotionZone(
            world,
            worldW * 0.65f, worldH * 0.5f,
            zoneW * 0.5f, zoneH * 0.5f,
            MotionTuning.LOW_TRACTION,
            Color.BLUE
        );

        MotionZone high = new MotionZone(
            world,
            worldW * 0.35f, worldH * 0.5f,
            zoneW * 0.5f, zoneH * 0.5f,
            MotionTuning.HIGH_FRICTION,
            Color.RED
        );

        System.out.println("LOW zone px: x=" + low.getX() + " y=" + low.getY() +
                   " w=" + low.getW() + " h=" + low.getH());

        // IMPORTANT: add to entityManager so CollisionManager -> listener receives Entity–Entity
        entityManager.addEntity(low);
        entityManager.addEntity(high);
        zones.add(low);
        zones.add(high);

        /* Key Binds for Game Scene Implementation */
        Keyboard kb = ioManager.getInputs(Keyboard.class);
        UserControlled user = new UserControlled(kb);
        AIControlled ai = new AIControlled();

        kb.bindAction(Input.Keys.A, Constants.LEFT);
        kb.bindAction(Input.Keys.D, Constants.RIGHT);
        kb.bindAction(Input.Keys.W, Constants.UP);
        kb.bindAction(Input.Keys.S, Constants.DOWN);
        kb.bindAction(Input.Keys.SPACE, Constants.ACTION);

        kb.addBind(Input.Keys.ESCAPE, this::openPause, true);
        kb.addBind(Input.Keys.M, this::toggleMute, true);
        
        // Collision Handler
        GameCollisionHandler handler = new GameCollisionHandler(entityManager, sound);
        collisionManager = new CollisionManager(world, handler);

        // test entities
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

        entityManager.addEntity(bucket);
        entityManager.addEntity(droplet);

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

    public void handleInput(float deltaTime) {}

    @Override
    public void update(float deltaTime) {
        if (isPaused) {
            sound.stopSound("move"); // Stop moving sound if paused
            isMoving = false;
            return;
        }

        gameTime += deltaTime;

        entityManager.update(deltaTime);
        movementManager.update(deltaTime);

        // Demo: Play moving sound effect when object is moving
        Vector2 velocity = bucket.getPhysicsBody().getVelocity();
        boolean objMoving =
            Math.abs(velocity.x) > 0.05f ||
            Math.abs(velocity.y) > 0.05f;

        updateMoveLoop(objMoving);
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
        // Player stays in bounds — physics walls are based on VIRTUAL size, not window size
    }

    @Override
    public void dispose() {
        font.dispose();
        shapeRenderer.dispose();
        zones.clear();
        entityManager.dispose();
        world.dispose();   

        if (bgm != null) {
            bgm.stop();
            bgm.dispose();
        }


        Gdx.app.log("GameScene", "Scene disposed - All managers and resources cleaned up");
    }

    private void stopMoveLoop() {
        sound.stopSound("move");
        isMoving = false;
    }

    private void updateMoveLoop(boolean objMoving) {
        if (sound.isMuted() || !objMoving) {
            stopMoveLoop();
            return;
        }

        if (!isMoving) {
            sound.loopSound("move");
            isMoving = true;
        }
    }

    private void openPause() {
        sceneManager.push(new PauseScene());
    }

    private void toggleMute() {
        sound.toggleMute();

        if (bgm != null) bgm.setVolume(sound.isMuted() ? 0f : 0.2f);

        Vector2 v = bucket.getPhysicsBody().getVelocity();
        boolean objMoving = Math.abs(v.x) > 0.05f || Math.abs(v.y) > 0.05f;

        updateMoveLoop(objMoving);
    }
}
