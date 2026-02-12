package io.github.raesleg.demo;

import java.util.ArrayList;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

import io.github.raesleg.engine.CollisionManager;
import io.github.raesleg.engine.EntityManager;
import io.github.raesleg.engine.Scene;
import io.github.raesleg.engine.Shape;
import io.github.raesleg.engine.Surfaces;
import io.github.raesleg.engine.movement.ControlState;
import io.github.raesleg.engine.movement.IOManager;
import io.github.raesleg.engine.movement.MovableEntity;
import io.github.raesleg.engine.movement.MovementManager;
import io.github.raesleg.engine.physics.PhysicsWorld;

public class GameScene extends Scene {

    private BitmapFont font;

    // Game state
    private boolean isPaused;
    private float gameTime;

    // movable entities implementing box2d physics engine
    private PhysicsWorld physicsWorld;
    private MovableEntity bucket;
    private MovableEntity droplet;

    // movement
    private float PPM = 100f;
    private ShapeRenderer shapeRenderer;
    private final ArrayList<Shape> zones = new ArrayList<>();

    private float worldW = Gdx.graphics.getWidth() / PPM;
    private float worldH = Gdx.graphics.getHeight() / PPM;

    private float zoneW = worldW * 0.12f;
    private float zoneH = worldH * 0.45f;

    public GameScene() {
        super();
        this.isPaused = false;
        this.gameTime = 0f;
    }

    @Override
    public void show() {
        // Initialize rendering resources
        font = new BitmapFont();
        font.setColor(Color.WHITE);

        // out of bound walls
        physicsWorld = new PhysicsWorld(new Vector2(0, 0));
        physicsWorld.createBoundsPixels(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), PPM);

        // create friction zone, forces from box2d
        physicsWorld.createMotionZone(worldW * 0.65f, worldH * 0.5f, zoneW * 0.5f, zoneH * 0.5f, MotionTuning.LOW_TRACTION);
        physicsWorld.createMotionZone(worldW * 0.35f, worldH * 0.5f, zoneW * 0.5f, zoneH * 0.5f, MotionTuning.HIGH_FRICTION);

        shapeRenderer = new ShapeRenderer();


        // low friction
        zones.add(new Surfaces(
            (worldW * 0.65f - zoneW * 0.5f) * PPM,
            (worldH * 0.5f - zoneH * 0.5f) * PPM,
            zoneW * PPM,
            zoneH * PPM,
            Color.BLUE
        ));

        // high friction
        zones.add(new Surfaces(
            (worldW * 0.35f - zoneW * 0.5f) * PPM,
            (worldH * 0.5f - zoneH * 0.5f) * PPM,
            zoneW * PPM,
            zoneH * PPM,
            Color.RED // red
        ));

        entityManager = new EntityManager();
        movementManager = new MovementManager(physicsWorld, entityManager);
        collisionManager = new CollisionManager(physicsWorld, entityManager);
        ioManager = new IOManager();

        // test entities 
        bucket = new MovableEntity(
                    physicsWorld, 
                    "bucket.png",
                    200,
                    200, 
                    64f,
                    64f,
                    new ControlState.UserControlled(ioManager),
                    MotionTuning.DEFAULT
                );

        droplet = new MovableEntity(
                    physicsWorld, 
                    "droplet.png",
                    500,
                    300, 
                    64f,
                    64f,
                    new ControlState.AIControlled(),
                    MotionTuning.DEFAULT
                );

        entityManager.addEntity(bucket);
        entityManager.addEntity(droplet);
    }

    @Override
    public void handleInput() {
        if (ioManager.isKeyJustPressed(Input.Keys.ESCAPE)) {
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

        ioManager.update();
        handleInput();
        entityManager.update(deltaTime);
        movementManager.update(deltaTime);
    }

    @Override
    public void render(SpriteBatch batch) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());

        // movement texture zones demo
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            for (Shape z : zones) z.draw(shapeRenderer);
        shapeRenderer.end();

        batch.begin();
            entityManager.render(batch);
            // HUD rendering
            font.draw(batch, "Game Time: " + String.format("%.1f", gameTime) + "s", 10, Gdx.graphics.getHeight() - 10);
            font.draw(batch, "Use WASD/Arrows to move | ESC to pause", 10, Gdx.graphics.getHeight() - 30);
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
        // Update viewports/cameras for new screen size
        // Keep player in bounds after resize
    }

    @Override
    public void dispose() {
        font.dispose();
        entityManager.dispose();

        // movement texture
        shapeRenderer.dispose();
        zones.clear();

        // abstract movement manager physics
        physicsWorld.dispose();

        Gdx.app.log("GameScene", "Scene disposed - All managers and resources cleaned up");
    }
}
