package io.github.raesleg.demo;

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

/**
 * StartScene — Main menu / title screen.
 * <p>
 * Uses Scene2D {@link Stage} with a {@link Table} layout containing the
 * game title and a "Start Game" {@link TextButton}. Pressing the button
 * (or ENTER) transitions to {@link LevelSelectScene} via
 * {@code SceneManager.set()}.
 */
public class StartScene extends Scene {

    private Stage stage;
    private BitmapFont titleFont;
    private BitmapFont buttonFont;
    private Texture pixelTexture;
    private SoundDevice sound;

    public StartScene() {
        super();
    }

    @Override
    public void show() {
        sound = getIOManager().getSound();

        /* Fonts */
        titleFont = new BitmapFont();
        titleFont.getData().setScale(5f);

        buttonFont = new BitmapFont();
        buttonFont.getData().setScale(2.5f);

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

        /* Title label style */
        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, Color.WHITE);

        /* Stage + Table layout */
        stage = new Stage(getUiViewport());

        Table root = new Table();
        root.setFillParent(true);

        Label title = new Label("GAME TITLE", titleStyle);

        TextButton startBtn = new TextButton("Start Game", btnStyle);
        startBtn.pad(15f, 40f, 15f, 40f);
        startBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                confirm();
            }
        });

        root.add(title).padBottom(80f);
        root.row();
        root.add(startBtn);

        stage.addActor(root);
        Gdx.input.setInputProcessor(stage);

        /* Keyboard shortcut: ENTER also starts */
        Keyboard kb = getIOManager().getInputs(Keyboard.class);
        if (kb != null) {
            kb.addBind(Input.Keys.ENTER, this::confirm, true);
            kb.addBind(Input.Keys.NUMPAD_ENTER, this::confirm, true);
        }

        Gdx.app.log("StartScene", "Scene shown — click 'Start Game' or press ENTER");
    }

    @Override
    public void update(float deltaTime) {
        stage.act(deltaTime);
    }

    @Override
    public void render(SpriteBatch batch) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f);
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
        pixelTexture.dispose();
        Gdx.app.log("StartScene", "Scene disposed");
    }

    private void confirm() {
        sound.playSound("selected", 1.0f);
        getSceneManager().set(new LevelSelectScene());
    }
}
