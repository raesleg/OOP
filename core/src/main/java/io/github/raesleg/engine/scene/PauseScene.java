package io.github.raesleg.engine.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

import io.github.raesleg.engine.sound.SoundManager;
/**
 * PauseScene - The pause menu overlay.
 * 
 * This scene overlays on top of GameScene (which remains in memory).
 * The scene is marked as TRANSPARENT so GameScene is rendered behind it.
 * 
 * Renders in SCREEN SPACE (not world space) so the overlay covers the entire
 * window regardless of viewport letterboxing or screen resolution.
 * 
 * TRIGGERS:
 * - Press ESC or "Resume" -> Calls sceneManager.pop() (Returns to GameScene)
 * - Press "Exit" -> Calls sceneManager.set(new StartScene()) (Returns to main
 * menu)
 * 
 * IOManager is injected by SceneManager (never created here).
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

    /* Screen-space projection — updated on resize */
    private Matrix4 screenProjection;

    // Sound manager for menu navigation and selection sounds
    private SoundManager soundManager;

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

        // Initialize screen-space projection matrix
        screenProjection = new Matrix4().setToOrtho2D(0, 0,
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Initialize sound manager and load sounds
        soundManager = new SoundManager();
        soundManager.addSound("menu", "uiMenu_sound.wav"); // Add menu navigation sound
        soundManager.addSound("selected", "uiSelected_sound.wav"); // Add selection sound

        Gdx.app.log("PauseScene", "Pause menu shown - ESC/Enter to resume, navigate with W/S or Up/Down");
    }

    @Override
    public void handleInput() {
        // Input Focus Rule: Only PauseScene receives input when it's on top

        // ESC -> Resume game (pop this scene)
        if (ioManager.isPauseRequested()) {

            // Play sound when ESC is pressed to resume
            soundManager.playSound("selected"); 

            sceneManager.pop();
            return;
        }

        // Navigate menu with W/S or Up/Down arrows
        else if (ioManager.isUpJustPressed()) {
            selectedOption--;
            if (selectedOption < 0) {
                selectedOption = menuOptions.length - 1;
            }
            soundManager.playSound("menu"); // Play menu navigation sound
        }

        else if (ioManager.isDownJustPressed()) {
            selectedOption++;
            if (selectedOption >= menuOptions.length) {
                selectedOption = 0;
            }
            soundManager.playSound("menu"); // Play menu navigation sound
        }

        // ENTER -> Select current option
        else if (ioManager.isConfirmRequested()) {
            soundManager.playSound("selected"); // Play selection sound
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
        handleInput();
    }

    @Override
    public void render(SpriteBatch batch) {
        /*
         * Rendering strategy (Liskov-safe — uses the base-class uiViewport):
         * 1. Screen-space overlay → covers the ENTIRE window (incl. letterbox bars)
         * 2. UI-viewport box+text → all layout in 1280×720 virtual coords;
         * FitViewport scales automatically so the menu always fits any window.
         */
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        // ── 1. Full-screen darkening overlay (screen-space) ──
        // Reset GL viewport to the FULL window so the overlay is not clipped
        // to the FitViewport's letterboxed region left over from GameScene.
        Gdx.gl.glViewport(0, 0, screenWidth, screenHeight);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        screenProjection.setToOrtho2D(0, 0, screenWidth, screenHeight);
        shapeRenderer.setProjectionMatrix(screenProjection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(overlayColor);
        shapeRenderer.rect(0, 0, screenWidth, screenHeight);
        shapeRenderer.end();

        // ── 2. Switch to UI viewport (1280×720 virtual coords) ──
        uiViewport.apply();
        uiCamera.update();
        shapeRenderer.setProjectionMatrix(uiCamera.combined);

        // Menu box — proportional to virtual resolution, always centred
        float boxWidth = VIRTUAL_WIDTH * 0.5f; // 640
        float boxHeight = VIRTUAL_HEIGHT * 0.55f; // 396
        float boxX = (VIRTUAL_WIDTH - boxWidth) / 2f;
        float boxY = (VIRTUAL_HEIGHT - boxHeight) / 2f;

        // Filled background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0.2f, 0.2f, 0.3f, 0.95f));
        shapeRenderer.rect(boxX, boxY, boxWidth, boxHeight);
        shapeRenderer.end();

        // Border
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(boxX, boxY, boxWidth, boxHeight);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // ── 3. Text — all in virtual coords (no manual scaleFactor) ──
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        // Title
        font.getData().setScale(3f);
        font.setColor(Color.WHITE);
        layout.setText(font, "PAUSED");
        float titleX = (VIRTUAL_WIDTH - layout.width) / 2f;
        float titleY = boxY + boxHeight - 40f;
        font.draw(batch, "PAUSED", titleX, titleY);

        // Menu options
        font.getData().setScale(2f);
        float optionStartY = boxY + boxHeight - 140f;
        float optionSpacing = 60f;

        for (int i = 0; i < menuOptions.length; i++) {
            String text;
            if (i == selectedOption) {
                font.setColor(selectedColor);
                text = "> " + menuOptions[i] + " <";
            } else {
                font.setColor(unselectedColor);
                text = menuOptions[i];
            }
            layout.setText(font, text);
            float optionX = (VIRTUAL_WIDTH - layout.width) / 2f;
            font.draw(batch, text, optionX, optionStartY - i * optionSpacing);
        }

        // Instructions
        font.getData().setScale(1.2f);
        font.setColor(Color.GRAY);
        String instructions = "W/S to navigate  |  Enter to select  |  Esc to resume";
        layout.setText(font, instructions);
        float instrX = (VIRTUAL_WIDTH - layout.width) / 2f;
        float instrY = boxY + 40f;
        font.draw(batch, instructions, instrX, instrY);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        // screenProjection is updated each frame in render() to match current window
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        font.dispose();

        Gdx.app.log("PauseScene", "Pause scene disposed");
    }
}
