package io.github.raesleg.game.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.scene.Scene;
import io.github.raesleg.game.io.Keyboard;

/**
 * LevelSelectScene — Level selection menu.
 * <p>
 * Presents Scene2D buttons for each available level. Selecting a level
 * instantiates the corresponding concrete {@link BaseGameScene} subclass
 * and transitions via {@code SceneManager.set()}.
 * <p>
 * A "Back" button returns to {@link StartScene}.
 */
public class LevelSelectScene extends Scene {

    private Stage stage;
    private BitmapFont titleFont;
    private BitmapFont buttonFont;
    private BitmapFont subFont;
    private Texture pixelTexture;
    private SoundDevice sound;

    public LevelSelectScene() {
        super();
    }

    @Override
    public void show() {
        sound = getIOManager().getSound();

        /* Fonts */
        titleFont = new BitmapFont();
        titleFont.getData().setScale(4f);

        buttonFont = new BitmapFont();
        buttonFont.getData().setScale(2f);

        /* 1×1 white pixel for button backgrounds */
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixelTexture = new Texture(pm);
        pm.dispose();

        TextureRegionDrawable white = new TextureRegionDrawable(new TextureRegion(pixelTexture));

        /* Button style */
        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.up = white.tint(new Color(0.25f, 0.25f, 0.35f, 1f));
        btnStyle.over = white.tint(new Color(0.35f, 0.35f, 0.5f, 1f));
        btnStyle.down = white.tint(new Color(0.15f, 0.15f, 0.25f, 1f));
        btnStyle.font = buttonFont;
        btnStyle.fontColor = Color.WHITE;
        btnStyle.overFontColor = Color.YELLOW;

        /* Back button style (smaller, dimmer) */
        TextButton.TextButtonStyle backStyle = new TextButton.TextButtonStyle();
        backStyle.up = white.tint(new Color(0.2f, 0.2f, 0.25f, 1f));
        backStyle.over = white.tint(new Color(0.3f, 0.3f, 0.4f, 1f));
        backStyle.down = white.tint(new Color(0.12f, 0.12f, 0.18f, 1f));
        backStyle.font = buttonFont;
        backStyle.fontColor = Color.LIGHT_GRAY;
        backStyle.overFontColor = Color.WHITE;

        /* Title label style */
        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, Color.WHITE);

        /* Subtitle label style */
        subFont = new BitmapFont();
        subFont.getData().setScale(1.5f);
        Label.LabelStyle subStyle = new Label.LabelStyle(subFont, Color.LIGHT_GRAY);

        /* Stage + Table layout */
        stage = new Stage(getUiViewport());

        Table root = new Table();
        root.setFillParent(true);

        /* Title */
        Label title = new Label("SELECT LEVEL", titleStyle);

        /* Level 1 button */
        TextButton lvl1Btn = new TextButton("Level 1 — Sunny Road", btnStyle);
        lvl1Btn.pad(15f, 40f, 15f, 40f);
        lvl1Btn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectLevel1();
            }
        });

        Label lvl1Desc = new Label("Normal traffic, no police", subStyle);

        /* Level 2 button */
        TextButton lvl2Btn = new TextButton("Level 2 — Rain Expressway", btnStyle);
        lvl2Btn.pad(15f, 40f, 15f, 40f);
        lvl2Btn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectLevel2();
            }
        });

        Label lvl2Desc = new Label("Wet road, police chase at 3 rule breaks", subStyle);

        /* Back button */
        TextButton backBtn = new TextButton("Back", backStyle);
        backBtn.pad(10f, 30f, 10f, 30f);
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                goBack();
            }
        });

        /* Assemble layout */
        root.add(title).padBottom(60f);
        root.row();
        root.add(lvl1Btn).padBottom(8f);
        root.row();
        root.add(lvl1Desc).padBottom(30f);
        root.row();
        root.add(lvl2Btn).padBottom(8f);
        root.row();
        root.add(lvl2Desc).padBottom(50f);
        root.row();
        root.add(backBtn);

        stage.addActor(root);
        Gdx.input.setInputProcessor(stage);

        /* Keyboard shortcuts */
        Keyboard kb = getIOManager().getInputs(Keyboard.class);
        if (kb != null) {
            kb.addBind(Input.Keys.NUM_1, this::selectLevel1, true);
            kb.addBind(Input.Keys.NUM_2, this::selectLevel2, true);
            kb.addBind(Input.Keys.ESCAPE, this::goBack, true);
            kb.addBind(Input.Keys.BACKSPACE, this::goBack, true);
        }

        Gdx.app.log("LevelSelectScene", "Level select shown — pick a level or press 1/2");
    }

    @Override
    public void update(float deltaTime) {
        stage.act(deltaTime);
    }

    @Override
    public void render(SpriteBatch batch) {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

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
        buttonFont.dispose();
        subFont.dispose();
        pixelTexture.dispose();
        Gdx.app.log("LevelSelectScene", "Scene disposed");
    }

    /* ── Transition helpers ── */

    private void selectLevel1() {
        sound.playSound("selected", 1.0f);
        getSceneManager().set(new Level1Scene());
    }

    private void selectLevel2() {
        sound.playSound("selected", 1.0f);
        getSceneManager().set(new Level2Scene());
    }

    private void goBack() {
        sound.playSound("menu", 1.0f);
        getSceneManager().set(new StartScene());
    }
}
