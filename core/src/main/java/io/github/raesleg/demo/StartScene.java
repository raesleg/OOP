package io.github.raesleg.demo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.raesleg.engine.IOManager;
import io.github.raesleg.engine.Scene;

/**
 * StartScene - The initial menu scene (Stack Base).
 * 
 * This is the first scene shown when the game launches.
 * Acts as the main menu / title screen.
 * 
 * TRIGGER: Press ENTER -> Calls sceneManager.set(new GameScene())
 * 
 * SCENE SOVEREIGNTY: This scene owns its own managers.
 * Since this is a simple menu, it only needs IOManager for input.
 */
public class StartScene extends Scene {

    /* Private Variables */
    private BitmapFont font;
    private GlyphLayout layout;
    private String titleText;
    private String promptText;

    /* Constructor */
    public StartScene() {
        super();
        this.titleText = "GAME TITLE";
        this.promptText = "Press ENTER to Start";
    }

    /* Scene Lifecycle Methods */

    @Override
    public void show() {
        // Initialize scene-specific resources
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        layout = new GlyphLayout();

        ioManager = new IOManager();
        ioManager.update();

        Gdx.app.log("StartScene", "Scene shown - Press ENTER to start the game");
    }

    @Override
    public void handleInput() {
        // Input Focus Rule: This only runs when StartScene is the top scene
        if (ioManager.isKeyJustPressed(Input.Keys.ENTER)) {
            // Transition to GameScene using set() - clears stack and starts fresh
            sceneManager.set(new GameScene());
        }
    }

    @Override
    public void update(float deltaTime) {
        ioManager.update();
        handleInput();
    }

    @Override
    public void render(SpriteBatch batch) {
        // Clear screen with a dark color
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Get screen dimensions
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        batch.begin();

        // Draw title centered
        font.getData().setScale(3f);
        layout.setText(font, titleText);
        float titleX = (screenWidth - layout.width) / 2;
        float titleY = screenHeight * 0.6f + layout.height / 2;
        font.draw(batch, titleText, titleX, titleY);

        // Draw prompt centered below title
        font.getData().setScale(1.5f);
        layout.setText(font, promptText);
        float promptX = (screenWidth - layout.width) / 2;
        float promptY = screenHeight * 0.4f + layout.height / 2;
        font.draw(batch, promptText, promptX, promptY);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        // Update any viewports or cameras here if needed
    }

    @Override
    public void dispose() {
        font.dispose();
        ioManager = null;

        Gdx.app.log("StartScene", "Scene disposed");
    }
}
