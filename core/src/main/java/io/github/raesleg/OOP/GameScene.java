package io.github.raesleg.OOP;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.Texture;

/**
 * GameScene - The main gameplay scene.
 * 
 * This scene contains the actual game logic, entities, and gameplay mechanics.
 * 
 * TRIGGER: Press ESC -> Calls sceneManager.push(new PauseScene())
 * CRUCIAL: When ESC is pressed, this scene is NOT destroyed.
 * It stays in memory in the background (paused state).
 * 
 * SCENE SOVEREIGNTY PRINCIPLE:
 * This scene owns and manages:
 * - EntityManager (manages game entities)
 * - MovementManager (handles position updates)
 * - CollisionManager (handles physics)
 * - IOManager (handles gameplay input)
 * 
 * All managers are disposed when this scene is disposed.
 */
public class GameScene extends Scene {

    /* Private Variables */
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    // Game state
    private boolean isPaused;
    private float gameTime;

    // Example entities (these would come from EntityManager)
    private Texture bucketTexture;
    private Texture dropletTexture;

    // movable entities implementing box2d physics engine
    private PhysicsWorld physicsWorld;
    private MovableEntity bucket;
    private MovableEntity droplet;

    /* Constructor */
    public GameScene() {
        super();
        this.isPaused = false;
        this.gameTime = 0f;
    }

    /* Scene Lifecycle Methods */
    @Override
    public void show() {
        // Initialize rendering resources
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.setColor(Color.WHITE);

        // game scene draws textures, optinally could make a separate irenderable method if entity gets overpopulated
        bucketTexture = new Texture("bucket.png");  
        dropletTexture = new Texture("droplet.png");

        // out of bound walls
        physicsWorld = new PhysicsWorld(new Vector2(0, 0)); 
        physicsWorld.createBoundsPixels(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 100f);

        // SCENE SOVEREIGNTY: Create and own all managers for this scene
        entityManager = new EntityManager();
        movementManager = new MovementManager(physicsWorld);
        // collisionManager = new ConcreteCollisionManager(); // When implemented
        ioManager = new IOManager();

        // Initialize entities through EntityManager
        // Example: entityManager.addEntity(new Player(playerX, playerY, playerSpeed));
        bucket = new MovableEntity(physicsWorld, 200, 200, new Controls.UserControlled(ioManager));
        droplet = new MovableEntity(physicsWorld, 500, 300, new Controls.AIControlled());
        entityManager.addEntity(bucket);
        entityManager.addEntity(droplet);

        Gdx.app.log("GameScene", "Scene shown - Game started. Press ESC to pause.");
    }

    @Override
    public void handleInput() {
        // Input Focus Rule: This only runs when GameScene is the top scene
        // ESC -> Push pause scene (this scene stays in memory)
        if (ioManager.isKeyJustPressed(Input.Keys.ESCAPE)) {
            sceneManager.push(new PauseScene());
            return; // Don't process other inputs when transitioning
        }
    }

    @Override
    public void update(float deltaTime) {
        if (isPaused) {
            return; // Don't update game logic when paused
        }

        ioManager.update();
        handleInput();

        // Update game time
        gameTime += deltaTime;

        // Update managers in correct order (Dependency Injection pattern)
        // entityManager.update(deltaTime);
        // movementManager.update(entityManager.getSnapshot(), deltaTime);
        // collisionManager.collisionResolver(entityManager);
        entityManager.update(deltaTime);
        movementManager.update(entityManager, deltaTime);

    }

    @Override
    public void render(SpriteBatch batch) {
        // Clear screen with gameplay background color
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
            // drawing userControlled
            batch.draw(
                bucketTexture,
                bucket.getX() - bucketTexture.getWidth() / 2f,
                bucket.getY() - bucketTexture.getHeight() / 2f
            );

            // drawing AIcontrolled
            batch.draw(
                dropletTexture,
                droplet.getX() - dropletTexture.getWidth() / 2f,
                droplet.getY() - dropletTexture.getHeight() / 2f
            );
        font.draw(batch, "Game Time: " + String.format("%.1f", gameTime) + "s", 10, Gdx.graphics.getHeight() - 10);
        font.draw(batch, "Use WASD/Arrows to move | ESC to pause", 10, Gdx.graphics.getHeight() - 30);
        // font.draw(batch, "Player: (" + (int)  + ", " + (int) playerY + ")", 10, Gdx.graphics.getHeight() - 50);
        batch.end();

        // drawing entities hitbox
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line); 
            shapeRenderer.setColor(Color.RED);
            float hitboxRadius = 20f; 
            shapeRenderer.circle(droplet.getX(), droplet.getY(), hitboxRadius);
        shapeRenderer.end();
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
        // playerX = Math.min(playerX, width - 20f);
        // playerY = Math.min(playerY, height - 20f);
    }

    @Override
    public void dispose() {
        // CRUCIAL: Dispose all resources owned by this scene
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
            shapeRenderer = null;
        }

        if (font != null) {
            font.dispose();
            font = null;
        }

        // SCENE SOVEREIGNTY: Cascade dispose to all owned managers
        if (entityManager != null) {
            // entityManager.dispose(); // If EntityManager has dispose method
            entityManager = null;
        }

        // abstract movement manager physics
        if (physicsWorld != null) {
            physicsWorld.dispose();
            physicsWorld = null;
        }
        if (bucketTexture != null) {
            bucketTexture.dispose();
            bucketTexture = null;
        }
        if (dropletTexture != null) {
            dropletTexture.dispose();
            dropletTexture = null;
        }
        if (ioManager != null) {
            ioManager = null;
        }

        if (collisionManager != null) {
            collisionManager = null;
        }

        Gdx.app.log("GameScene", "Scene disposed - All managers and resources cleaned up");
    }
}
