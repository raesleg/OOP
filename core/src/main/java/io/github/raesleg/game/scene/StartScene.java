package io.github.raesleg.game.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.scene.Scene;

import io.github.raesleg.game.io.Keyboard;

public class StartScene extends Scene {

    private Stage stage;
    private SoundDevice sound;

    private Texture bgTexture;
    private Texture startUpTexture;
    private Texture startDownTexture;

    private BitmapFont buttonFont;
    private ImageButton startBtn;

    private static final float START_BUTTON_WIDTH = 320f;
    private static final float START_BUTTON_HEIGHT = 90f;
    private static final float START_BUTTON_Y = 60f;

    @Override
    public void show() {
        sound = getIOManager().getSound();
        stage = new Stage(getUiViewport());
        
        // Load textures
        bgTexture = new Texture("menu/start_bg.png");
        startUpTexture = new Texture("menu/ButtonLargeRed.png");
        startDownTexture = new Texture("menu/ButtonLargeRedActive.png");

        FreeTypeFontGenerator generator =
            new FreeTypeFontGenerator(Gdx.files.internal("fonts/BubblegumSans-Regular.ttf"));

        FreeTypeFontGenerator.FreeTypeFontParameter param =
            new FreeTypeFontGenerator.FreeTypeFontParameter();
        param.size = 52;
        param.borderWidth = 2;
        param.borderColor = Color.BLACK;
        param.color = Color.WHITE;

        buttonFont = generator.generateFont(param);
        generator.dispose();

        Image background = new Image(new TextureRegionDrawable(new TextureRegion(bgTexture)));
        background.setFillParent(true);
        stage.addActor(background);

        ImageButton.ImageButtonStyle startStyle = new ImageButton.ImageButtonStyle();
        startStyle.imageUp = new TextureRegionDrawable(new TextureRegion(startUpTexture));
        startStyle.imageOver = new TextureRegionDrawable(new TextureRegion(startDownTexture));
        startStyle.imageDown = new TextureRegionDrawable(new TextureRegion(startDownTexture));

        startBtn = new ImageButton(startStyle);
        startBtn.setTransform(true);
        startBtn.setSize(START_BUTTON_WIDTH, START_BUTTON_HEIGHT);
        startBtn.setPosition(
                getUiViewport().getWorldWidth() / 2f - START_BUTTON_WIDTH / 2f,
                START_BUTTON_Y
        );

        startBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                confirm();
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer,
                              com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                startBtn.setScale(1.03f);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer,
                             com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                startBtn.setScale(1f);
            }
        });

        stage.addActor(startBtn);
        Gdx.input.setInputProcessor(stage);

        Keyboard kb = getIOManager().getInputs(Keyboard.class);
        if (kb != null) {
            kb.addBind(Input.Keys.ENTER, this::confirm, true);
            kb.addBind(Input.Keys.NUMPAD_ENTER, this::confirm, true);
        }
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

        batch.setProjectionMatrix(getUiCamera().combined);
        batch.begin();

        String text = "START";
        GlyphLayout layout = new GlyphLayout(buttonFont, text);

        float textX = startBtn.getX() + (startBtn.getWidth() - layout.width) / 2f;
        float textY = startBtn.getY() + (startBtn.getHeight() + layout.height) / 1.8f;

        buttonFont.draw(batch, layout, textX, textY);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        bgTexture.dispose();
        startUpTexture.dispose();
        startDownTexture.dispose();
        buttonFont.dispose();
    }

    private void confirm() {
        sound.playSound("selected", 1.0f);
        getSceneManager().set(new LevelSelectScene());
    }
}
