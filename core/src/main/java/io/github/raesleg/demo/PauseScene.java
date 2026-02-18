package io.github.raesleg.demo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;

import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.scene.Scene;

/*
 * PauseScene - The pause menu overlay. 
 * - Press ESC or "Resume" -> Calls sceneManager.pop() (Returns to GameScene)
 * - Press "Exit" -> Calls sceneManager.set(new StartScene()) (Returns to main
 * menu)
*/

public class PauseScene extends Scene {

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

    // Screen-space projection — updated on resize
    private Matrix4 screenProjection;

    private SoundDevice sound;

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

    @Override
    public void show() {
        // Initialize rendering resources
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        layout = new GlyphLayout();

        // Initialize screen-space projection matrix
        screenProjection = new Matrix4().setToOrtho2D(0, 0,
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Retrieve shared sound device (UI sounds registered centrally)
        sound = getIOManager().getSound();

        // Input Bindings
        Keyboard kb = getIOManager().getInputs(Keyboard.class);
        if (kb != null) {
            // Navigate Menu
            kb.addBind(Input.Keys.ESCAPE, this::resumeGame, true);
            kb.addBind(Input.Keys.W, this::moveUp, true);
            kb.addBind(Input.Keys.UP, this::moveUp, true);
            kb.addBind(Input.Keys.S, this::moveDown, true);
            kb.addBind(Input.Keys.DOWN, this::moveDown, true);
            kb.addBind(Input.Keys.ENTER, this::confirm, true);
            kb.addBind(Input.Keys.NUMPAD_ENTER, this::confirm, true);
            kb.addBind(Input.Keys.M, this::toggleMute, true);
        }

        Gdx.app.log("PauseScene", "Pause menu shown - ESC/Enter to resume, navigate with W/S or Up/Down");
    }

    /* Executes the currently selected menu option */
    private void executeSelectedOption() {

        switch (selectedOption) {
            case 0: // Resume
                // Pop this scene to return to the paused GameScene
                getSceneManager().pop();
                break;

            case 1: // Exit to Main Menu
                // Set new StartScene - clears entire stack including paused GameScene
                getSceneManager().set(new StartScene());
                break;

            default:
                break;
        }
    }

    @Override
    public void update(float deltaTime) {
    }

    /*
     * Rendering strategy (Liskov-safe — uses the base-class uiViewport):
     * 1. Screen-space overlay → covers the ENTIRE window (incl. letterbox bars)
     * 2. UI-viewport box+text → all layout in 1280×720 virtual coords;
     * FitViewport scales automatically so the menu always fits any window.
     */
    @Override
    public void render(SpriteBatch batch) {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

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
        getUiViewport().apply();
        getUiCamera().update();
        shapeRenderer.setProjectionMatrix(getUiCamera().combined);

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
        batch.setProjectionMatrix(getUiCamera().combined);
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

    /* Private method for key bindings */
    private void resumeGame() {
        sound.playSound("selected", 1.0f);
        getSceneManager().pop();
    }

    private void moveUp() {
        selectedOption--;
        if (selectedOption < 0)
            selectedOption = menuOptions.length - 1;
        sound.playSound("menu", 1.0f);
    }

    private void moveDown() {
        selectedOption++;
        if (selectedOption >= menuOptions.length)
            selectedOption = 0;
        sound.playSound("menu", 1.0f);
    }

    private void confirm() {
        sound.playSound("selected", 1.0f);
        executeSelectedOption();
    }

    private void toggleMute() {
        sound.toggleMute();
    }

}
