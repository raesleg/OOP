package io.github.raesleg.OOP;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

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

    // Example entities (these would come from EntityManager in a full
    // implementation)
    private float playerX, playerY;
    private float playerSpeed;

    /* Constructor */
    public GameScene() {
        super();
        this.isPaused = false;
        this.gameTime = 0f;

        // Initial player position
        this.playerX = 400f;
        this.playerY = 300f;
        this.playerSpeed = 200f;
    }

    /* Scene Lifecycle Methods */

    @Override
    public void show() {
        // Initialize rendering resources
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.setColor(Color.WHITE);

        // SCENE SOVEREIGNTY: Create and own all managers for this scene
        entityManager = new EntityManager();
        movementManager = new MovementManager();
        // collisionManager = new ConcreteCollisionManager(); // When implemented
        ioManager = new IOManager();

        // Initialize entities through EntityManager
        // Example: entityManager.addEntity(new Player(playerX, playerY, playerSpeed));

        Gdx.app.log("GameScene", "Scene shown - Game started. Press ESC to pause.");
    }

    @Override
    public void handleInput() {
        // Input Focus Rule: This only runs when GameScene is the top scene

        // ESC -> Push pause scene (this scene stays in memory)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            sceneManager.push(new PauseScene());
            return; // Don't process other inputs when transitioning
        }

        // Game input handling (WASD movement example)
        // In a full implementation, IOManager would track pressed keys
        // and the Scene would act on that information
        float moveX = 0f;
        float moveY = 0f;

        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            moveY = 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            moveY = -1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            moveX = -1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            moveX = 1f;
        }

        // Normalize diagonal movement
        if (moveX != 0f && moveY != 0f) {
            float factor = 0.7071f; // 1/sqrt(2)
            moveX *= factor;
            moveY *= factor;
        }

        // Apply movement (deltaTime handled in update)
        float dt = Gdx.graphics.getDeltaTime();
        playerX += moveX * playerSpeed * dt;
        playerY += moveY * playerSpeed * dt;

        // Keep player in bounds
        playerX = Math.max(20f, Math.min(playerX, Gdx.graphics.getWidth() - 20f));
        playerY = Math.max(20f, Math.min(playerY, Gdx.graphics.getHeight() - 20f));
    }

    @Override
    public void update(float deltaTime) {
        if (isPaused) {
            return; // Don't update game logic when paused
        }

        // Handle input first
        handleInput();

        // Update game time
        gameTime += deltaTime;

        // Update managers in correct order (Dependency Injection pattern)
        // entityManager.update(deltaTime);
        // movementManager.update(entityManager.getSnapshot(), deltaTime);
        // collisionManager.collisionResolver(entityManager);
    }

    @Override
    public void render(SpriteBatch batch) {
        // Clear screen with gameplay background color
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Render entities using ShapeRenderer
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw player
        shapeRenderer.setColor(Color.CYAN);
        shapeRenderer.circle(playerX, playerY, 20f);

        // Draw some example obstacles/entities
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.circle(200f, 200f, 15f);
        shapeRenderer.circle(600f, 400f, 15f);
        shapeRenderer.circle(300f, 500f, 15f);

        shapeRenderer.end();

        // Render UI text
        batch.begin();
        font.draw(batch, "Game Time: " + String.format("%.1f", gameTime) + "s", 10, Gdx.graphics.getHeight() - 10);
        font.draw(batch, "Use WASD/Arrows to move | ESC to pause", 10, Gdx.graphics.getHeight() - 30);
        font.draw(batch, "Player: (" + (int) playerX + ", " + (int) playerY + ")", 10, Gdx.graphics.getHeight() - 50);
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
        playerX = Math.min(playerX, width - 20f);
        playerY = Math.min(playerY, height - 20f);
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

        if (movementManager != null) {
            movementManager = null;
        }

        if (collisionManager != null) {
            collisionManager = null;
        }

        if (ioManager != null) {
            ioManager = null;
        }

        Gdx.app.log("GameScene", "Scene disposed - All managers and resources cleaned up");
    }
}
