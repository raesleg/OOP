package io.github.raesleg.demo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import io.github.raesleg.engine.IOManager;
import io.github.raesleg.engine.Scene;

/**
 * PauseScene - The pause menu overlay.
 * 
 * This scene overlays on top of GameScene (which remains in memory).
 * The scene is marked as TRANSPARENT so GameScene is rendered behind it.
 * 
 * TRIGGERS:
 * - Press ESC or "Resume" -> Calls sceneManager.pop() (Returns to GameScene)
 * - Press "Exit" -> Calls sceneManager.set(new StartScene()) (Returns to main
 * menu)
 * 
 * SCENE SOVEREIGNTY: This scene owns its own IOManager for input handling.
 * Since it's just a menu overlay, it doesn't need EntityManager or physics
 * managers.
 */
public class PauseScene extends Scene {

    /* Private Variables */
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private GlyphLayout layout;

    // Menu options
    private String[] menuOptions;
    private int selectedOption;

    // Overlay styling
    private Color overlayColor;
    private Color selectedColor;
    private Color unselectedColor;

    /* Constructor */
    public PauseScene() {
        super();
        // Mark as transparent so GameScene renders behind this overlay
        setTransparent(true);

        this.menuOptions = new String[] { "Resume", "Exit to Main Menu" };
        this.selectedOption = 0;

        // Styling
        this.overlayColor = new Color(0f, 0f, 0f, 0.7f); // Semi-transparent black
        this.selectedColor = Color.YELLOW;
        this.unselectedColor = Color.WHITE;
    }

    /* Scene Lifecycle Methods */

    @Override
    public void show() {
        // Initialize rendering resources
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        layout = new GlyphLayout();

        ioManager = new IOManager();
        ioManager.update();

        Gdx.app.log("PauseScene", "Pause menu shown - ESC/Enter to resume, navigate with W/S or Up/Down");
    }

    @Override
    public void handleInput() {
        // Input Focus Rule: Only PauseScene receives input when it's on top

        // ESC -> Resume game (pop this scene)
        if (ioManager.isKeyJustPressed(Input.Keys.ESCAPE)) {
            sceneManager.pop();
            return;
        }

        // Navigate menu with W/S or Up/Down arrows
        if (ioManager.isKeyJustPressed(Input.Keys.W) || ioManager.isKeyJustPressed(Input.Keys.UP)) {
            selectedOption--;
            if (selectedOption < 0) {
                selectedOption = menuOptions.length - 1;
            }
        }

        if (ioManager.isKeyJustPressed(Input.Keys.S) || ioManager.isKeyJustPressed(Input.Keys.DOWN)) {
            selectedOption++;
            if (selectedOption >= menuOptions.length) {
                selectedOption = 0;
            }
        }

        // ENTER -> Select current option
        if (ioManager.isKeyJustPressed(Input.Keys.ENTER)) {
            executeSelectedOption();
        }
    }

    /**
     * Executes the currently selected menu option.
     */
    private void executeSelectedOption() {
        switch (selectedOption) {
            case 0: // Resume
                // Pop this scene to return to the paused GameScene
                sceneManager.pop();
                break;

            case 1: // Exit to Main Menu
                // Set new StartScene - clears entire stack including paused GameScene
                sceneManager.set(new StartScene());
                break;

            default:
                break;
        }
    }

    @Override
    public void update(float deltaTime) {
        ioManager.update();
        handleInput();
    }

    @Override
    public void render(SpriteBatch batch) {
        // NOTE: GameScene is rendered first because this scene is transparent
        // We only draw the overlay on top

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        // Enable blending for transparency
        Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
        Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);

        // Draw semi-transparent overlay
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(overlayColor);
        shapeRenderer.rect(0, 0, screenWidth, screenHeight);
        shapeRenderer.end();

        // Draw pause menu box
        float boxWidth = 400f;
        float boxHeight = 250f;
        float boxX = (screenWidth - boxWidth) / 2;
        float boxY = (screenHeight - boxHeight) / 2;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0.2f, 0.2f, 0.3f, 0.95f));
        shapeRenderer.rect(boxX, boxY, boxWidth, boxHeight);
        shapeRenderer.end();

        // Draw border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(boxX, boxY, boxWidth, boxHeight);
        shapeRenderer.end();

        Gdx.gl.glDisable(Gdx.gl.GL_BLEND);

        // Draw text
        batch.begin();

        // Draw "PAUSED" title
        font.getData().setScale(2.5f);
        font.setColor(Color.WHITE);
        layout.setText(font, "PAUSED");
        float titleX = (screenWidth - layout.width) / 2;
        float titleY = boxY + boxHeight - 30f;
        font.draw(batch, "PAUSED", titleX, titleY);

        // Draw menu options
        font.getData().setScale(1.5f);
        float optionY = boxY + boxHeight - 100f;
        float optionSpacing = 50f;

        for (int i = 0; i < menuOptions.length; i++) {
            // Highlight selected option
            if (i == selectedOption) {
                font.setColor(selectedColor);
                String optionText = "> " + menuOptions[i] + " <";
                layout.setText(font, optionText);
                float optionX = (screenWidth - layout.width) / 2;
                font.draw(batch, optionText, optionX, optionY - i * optionSpacing);
            } else {
                font.setColor(unselectedColor);
                layout.setText(font, menuOptions[i]);
                float optionX = (screenWidth - layout.width) / 2;
                font.draw(batch, menuOptions[i], optionX, optionY - i * optionSpacing);
            }
        }

        // Draw instructions
        font.getData().setScale(1f);
        font.setColor(Color.GRAY);
        String instructions = "W/S or Up/Down to navigate | ENTER to select | ESC to resume";
        layout.setText(font, instructions);
        float instrX = (screenWidth - layout.width) / 2;
        float instrY = boxY + 30f;
        font.draw(batch, instructions, instrX, instrY);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        // Overlay adjusts automatically based on screen size
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        font.dispose();
        ioManager = null;

        Gdx.app.log("PauseScene", "Pause scene disposed");
    }
}
