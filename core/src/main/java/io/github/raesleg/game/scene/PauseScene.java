package io.github.raesleg.game.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.scene.Scene;
import io.github.raesleg.game.io.Keyboard;

/**
 * PauseScene — Transparent overlay pause menu.
 * <p>
 * Uses Scene2D {@link Stage} and {@link Table} for layout, replacing
 * the previous raw ShapeRenderer / GL implementation.
 * Rendered on top of the frozen game scene via {@code setTransparent(true)}.
 * <p>
 * Controls: W/S navigate, A/D volume, Enter select, ESC resume, M mute.
 */
public class PauseScene extends Scene {

    private Stage stage;
    private BitmapFont titleFont;
    private BitmapFont optionFont;
    private BitmapFont instrFont;
    private Texture pixelTexture;

    private SoundDevice sound;

    private final String[] menuOptions = { "Resume", "Volume", "Exit to Level Select" };
    private final Label[] optionLabels = new Label[3];
    private int selectedOption;

    public PauseScene() {
        super();
        setTransparent(true);
        this.selectedOption = 0;
    }

    @Override
    public void show() {
        sound = getIOManager().getSound();

        /* Fonts — separate instances per scale to avoid Scene2D re-measure issues */
        titleFont = new BitmapFont();
        titleFont.getData().setScale(3f);

        optionFont = new BitmapFont();
        optionFont.getData().setScale(2f);

        instrFont = new BitmapFont();
        instrFont.getData().setScale(1.2f);

        /* 1×1 white pixel for tinting backgrounds */
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixelTexture = new Texture(pm);
        pm.dispose();

        TextureRegionDrawable white = new TextureRegionDrawable(new TextureRegion(pixelTexture));

        /* Stage — uses base-class UI viewport (1280×720 virtual coords) */
        stage = new Stage(getUiViewport());

        /* Overlay — semi-transparent black covers entire viewport */
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setBackground(white.tint(new Color(0f, 0f, 0f, 0.7f)));

        /* Inner panel — dark background with padding */
        Table panel = new Table();
        panel.setBackground(white.tint(new Color(0.2f, 0.2f, 0.3f, 0.95f)));
        panel.pad(40f, 60f, 40f, 60f);

        /* Title */
        Label title = new Label("PAUSED", new Label.LabelStyle(titleFont, Color.WHITE));
        panel.add(title).padBottom(50f);
        panel.row();

        /* Menu option labels — text + colour updated dynamically */
        for (int i = 0; i < menuOptions.length; i++) {
            optionLabels[i] = new Label("", new Label.LabelStyle(optionFont, Color.WHITE));
            panel.add(optionLabels[i]).padBottom(20f);
            panel.row();
        }

        /* Instructions */
        Label instructions = new Label(
                "W/S to navigate  |  A/D to adjust volume  |  Enter to select  |  Esc to resume",
                new Label.LabelStyle(instrFont, Color.GRAY));
        panel.add(instructions).padTop(30f);

        overlay.add(panel);
        stage.addActor(overlay);

        /* Input bindings */
        Keyboard kb = getIOManager().getInputs(Keyboard.class);
        if (kb != null) {
            kb.addBind(Input.Keys.ESCAPE, this::resumeGame, true);
            kb.addBind(Input.Keys.W, this::moveUp, true);
            kb.addBind(Input.Keys.UP, this::moveUp, true);
            kb.addBind(Input.Keys.S, this::moveDown, true);
            kb.addBind(Input.Keys.DOWN, this::moveDown, true);
            kb.addBind(Input.Keys.A, this::volumeDown, true);
            kb.addBind(Input.Keys.LEFT, this::volumeDown, true);
            kb.addBind(Input.Keys.D, this::volumeUp, true);
            kb.addBind(Input.Keys.RIGHT, this::volumeUp, true);
            kb.addBind(Input.Keys.ENTER, this::confirm, true);
            kb.addBind(Input.Keys.NUMPAD_ENTER, this::confirm, true);
            kb.addBind(Input.Keys.M, this::toggleMute, true);
        }

        refreshLabels();
        Gdx.app.log("PauseScene", "Pause menu shown - ESC/Enter to resume, navigate with W/S or Up/Down");
    }

    /** Updates label text and colours based on current selection and volume. */
    private void refreshLabels() {
        for (int i = 0; i < menuOptions.length; i++) {
            String text = menuOptions[i];

            if (i == 1) {
                int volPercent = Math.round(sound.getMasterVolume() * 100f);
                text = "< Volume: " + volPercent + "% >";
            }

            if (i == selectedOption) {
                optionLabels[i].setText("> " + text + " <");
                optionLabels[i].setColor(Color.YELLOW);
            } else {
                optionLabels[i].setText(text);
                optionLabels[i].setColor(Color.WHITE);
            }
        }
    }

    @Override
    public void update(float deltaTime) {
        stage.act(deltaTime);
    }

    @Override
    public void render(SpriteBatch batch) {
        getUiViewport().apply();
        getUiCamera().update();
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        titleFont.dispose();
        optionFont.dispose();
        instrFont.dispose();
        pixelTexture.dispose();
        Gdx.app.log("PauseScene", "Pause scene disposed");
    }

    /* ── Menu actions ── */

    private void resumeGame() {
        sound.playSound("selected", 1.0f);
        getSceneManager().pop();
    }

    private void moveUp() {
        selectedOption--;
        if (selectedOption < 0)
            selectedOption = menuOptions.length - 1;
        sound.playSound("menu", 1.0f);
        refreshLabels();
    }

    private void moveDown() {
        selectedOption++;
        if (selectedOption >= menuOptions.length)
            selectedOption = 0;
        sound.playSound("menu", 1.0f);
        refreshLabels();
    }

    private void confirm() {
        sound.playSound("selected", 1.0f);
        switch (selectedOption) {
            case 0:
                getSceneManager().pop();
                break;
            case 1:
                break; // Volume — A/D adjusts, Enter does nothing
            case 2:
                getSceneManager().set(new LevelSelectScene());
                break;
            default:
                break;
        }
    }

    private void volumeUp() {
        float vol = Math.min(1f, sound.getMasterVolume() + 0.1f);
        sound.setMasterVolume(vol);
        sound.playSound("menu", 1.0f);
        refreshLabels();
    }

    private void volumeDown() {
        float vol = Math.max(0f, sound.getMasterVolume() - 0.1f);
        sound.setMasterVolume(vol);
        sound.playSound("menu", 1.0f);
        refreshLabels();
    }

    private void toggleMute() {
        sound.toggleMute();
        refreshLabels();
    }
}
