// Package: io.github.raesleg.game.scene
package io.github.raesleg.game.scene;

import java.util.function.Supplier;

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

import io.github.raesleg.engine.scene.Scene;
import io.github.raesleg.game.io.Keyboard;
import io.github.raesleg.engine.io.SoundDevice;

// ResultsScene — Win/Lose screen displayed after a level ends
public class ResultsScene extends Scene {

    private final LevelResult result;
    private final Supplier<Scene> retryFactory;

    private Stage stage;
    private BitmapFont titleFont;
    private BitmapFont bodyFont;
    private BitmapFont buttonFont;
    private BitmapFont smallFont;
    private Texture pixelTexture;

    private SoundDevice sound;

    public ResultsScene(LevelResult result, Supplier<Scene> retryFactory) {
        super();
        this.result = result;
        this.retryFactory = retryFactory;
    }

    @Override
    public void show() {
        sound = getIOManager().getSound();

        if (result.isCompleted()) {
            sound.playSound("win", 1.0f);
        } else {
            sound.playSound("gameover", 1.0f);
        }

        /* Fonts */
        titleFont = new BitmapFont();
        titleFont.getData().setScale(4f);

        bodyFont = new BitmapFont();
        bodyFont.getData().setScale(2f);

        buttonFont = new BitmapFont();
        buttonFont.getData().setScale(2.5f);

        /* 1x1 white pixel for button backgrounds */
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixelTexture = new Texture(pm);
        pm.dispose();

        TextureRegionDrawable white = new TextureRegionDrawable(
                new TextureRegion(pixelTexture));

        /* Button style */
        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.up = white.tint(new Color(0.25f, 0.25f, 0.35f, 1f));
        btnStyle.over = white.tint(new Color(0.35f, 0.35f, 0.5f, 1f));
        btnStyle.down = white.tint(new Color(0.15f, 0.15f, 0.25f, 1f));
        btnStyle.font = buttonFont;
        btnStyle.fontColor = Color.WHITE;
        btnStyle.overFontColor = Color.YELLOW;

        /* Label styles */
        Color titleColor = result.isCompleted() ? Color.GREEN : Color.RED;
        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, titleColor);
        Label.LabelStyle bodyStyle = new Label.LabelStyle(bodyFont, Color.WHITE);

        /* Stage + Table layout */
        stage = new Stage(getUiViewport());

        Table root = new Table();
        root.setFillParent(true);

        /* Title */
        String heading = result.isCompleted() ? "LEVEL COMPLETE!" : "GAME OVER";
        Label title = new Label(heading, titleStyle);
        root.add(title).padBottom(30f).colspan(2);
        root.row();

        /* Level name */
        Label levelLabel = new Label(result.getLevelName(), bodyStyle);
        root.add(levelLabel).padBottom(20f).colspan(2);
        root.row();

        /* Stats */
        root.add(new Label("Score: " + result.getScore(), bodyStyle))
                .padBottom(10f).colspan(2);
        root.row();
        int seconds = (int) result.getTime();
        root.add(new Label("Time: " + seconds + "s", bodyStyle))
                .padBottom(10f).colspan(2);
        root.row();
        root.add(new Label("Rules Broken: " + result.getRulesBroken(), bodyStyle))
                .padBottom(10f).colspan(2);
        root.row();

        /* Loss reason (only shown on game over) */
        if (!result.isCompleted() && !result.getLossReason().isEmpty()) {
            Label.LabelStyle reasonStyle = new Label.LabelStyle(bodyFont, Color.ORANGE);
            root.add(new Label("Reason: " + result.getLossReason(), reasonStyle))
                    .padBottom(10f).colspan(2);
            root.row();
        }

        /* Violation breakdown (only shown on game over when there are violations) */
        if (!result.isCompleted() && !result.getViolations().isEmpty()) {
            Label.LabelStyle violationHeaderStyle = new Label.LabelStyle(bodyFont, Color.YELLOW);
            root.add(new Label("Violations:", violationHeaderStyle))
                    .padBottom(6f).colspan(2);
            root.row();

            smallFont = new BitmapFont();
            smallFont.getData().setScale(1.6f);
            Label.LabelStyle violationStyle = new Label.LabelStyle(smallFont, Color.SALMON);

            for (String v : result.getViolations()) {
                root.add(new Label("- " + v, violationStyle))
                        .padBottom(4f).colspan(2);
                root.row();
            }
        }

        /* Spacer before buttons */
        root.add().padBottom(30f).colspan(2);
        root.row();

        /* Retry button */
        TextButton retryBtn = new TextButton("Retry", btnStyle);
        retryBtn.pad(15f, 40f, 15f, 40f);
        retryBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                sound.stopSound("gameover");
                sound.playSound("select", 1.0f);
                getSceneManager().set(retryFactory.get());
            }
        });

        /* Main Menu button */
        TextButton menuBtn = new TextButton("Level Select", btnStyle);
        menuBtn.pad(15f, 40f, 15f, 40f);
        menuBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                sound.stopSound("gameover");
                sound.playSound("select", 1.0f);
                getSceneManager().set(new LevelSelectScene());
            }
        });

        root.add(retryBtn).padRight(30f);
        root.add(menuBtn);

        stage.addActor(root);
        Gdx.input.setInputProcessor(stage);

        /* Keyboard shortcuts */
        Keyboard kb = getIOManager().getInputs(Keyboard.class);
        if (kb != null) {
            kb.addBind(Input.Keys.R, () -> getSceneManager().set(retryFactory.get()), true);
            kb.addBind(Input.Keys.ESCAPE, () -> getSceneManager().set(new LevelSelectScene()), true);
        }

        Gdx.app.log("ResultsScene", "Showing " + heading
                + " — " + result.getLevelName()
                + " | Score: " + result.getScore());
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
        bodyFont.dispose();
        buttonFont.dispose();
        if (smallFont != null)
            smallFont.dispose();
        pixelTexture.dispose();
        Gdx.app.log("ResultsScene", "Scene disposed");
    }
}
