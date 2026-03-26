package io.github.raesleg.game.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
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

public class LevelSelectScene extends Scene {

    private Stage stage;
    private SoundDevice sound;

    private Texture bgTexture;
    private Texture redButtonUpTexture;
    private Texture redButtonDownTexture;
    private Texture panelTexture;

    private BitmapFont titleFont;
    private BitmapFont levelFont;
    private BitmapFont subtitleFont;
    private BitmapFont descFont;
    private BitmapFont backFont;

    private ImageButton level1Card;
    private ImageButton level2Card;
    private ImageButton backBtn;

    private static final float CARD_W = 360f;
    private static final float CARD_H = 215f;
    private static final float CARD_Y = 80f;
    private static final float CARD_GAP = 44f;

    private static final float BANNER_W = 270f;
    private static final float BANNER_H = 62f;

    private static final float BACK_W = 210f;
    private static final float BACK_H = 72f;
    private static final float BACK_Y = 18f;

    @Override
    public void show() {
        sound = getIOManager().getSound();
        stage = new Stage(getUiViewport());

        bgTexture = new Texture("menu/start_bg.png");
        redButtonUpTexture = new Texture("menu/ButtonLargeRed.png");
        redButtonDownTexture = new Texture("menu/ButtonLargeRedActive.png");

        panelTexture = makePanelTexture(
                600, 340,
                new Color(1f, 0.97f, 0.87f, 0.92f),
                new Color(0.70f, 0.55f, 0.28f, 1f)
        );

        titleFont = makeFont("fonts/BubblegumSans-Regular.ttf", 58, Color.WHITE, 3);
        levelFont = makeFont("fonts/BubblegumSans-Regular.ttf", 30, Color.WHITE, 2);
        subtitleFont = makeFont("fonts/BubblegumSans-Regular.ttf", 26, new Color(0.12f, 0.12f, 0.12f, 1f), 0);
        descFont = makeFont("fonts/BubblegumSans-Regular.ttf", 18, new Color(0.10f, 0.10f, 0.10f, 1f), 0);
        backFont = makeFont("fonts/BubblegumSans-Regular.ttf", 34, Color.WHITE, 2);

        Image background = new Image(new TextureRegionDrawable(new TextureRegion(bgTexture)));
        background.setFillParent(true);
        stage.addActor(background);

        ImageButton.ImageButtonStyle cardStyle = new ImageButton.ImageButtonStyle();
        cardStyle.imageUp = new TextureRegionDrawable(new TextureRegion(panelTexture));
        cardStyle.imageOver = new TextureRegionDrawable(new TextureRegion(panelTexture));
        cardStyle.imageDown = new TextureRegionDrawable(new TextureRegion(panelTexture));

        ImageButton.ImageButtonStyle redStyle = new ImageButton.ImageButtonStyle();
        redStyle.imageUp = new TextureRegionDrawable(new TextureRegion(redButtonUpTexture));
        redStyle.imageOver = new TextureRegionDrawable(new TextureRegion(redButtonDownTexture));
        redStyle.imageDown = new TextureRegionDrawable(new TextureRegion(redButtonDownTexture));

        level1Card = new ImageButton(cardStyle);
        level2Card = new ImageButton(cardStyle);
        backBtn = new ImageButton(redStyle);

        float worldW = getUiViewport().getWorldWidth();
        float centerX = worldW / 2f;

        float totalWidth = CARD_W * 2f + CARD_GAP;
        float leftX = centerX - totalWidth / 2f;
        float rightX = leftX + CARD_W + CARD_GAP;

        level1Card.setSize(CARD_W, CARD_H);
        level2Card.setSize(CARD_W, CARD_H);
        backBtn.setSize(BACK_W, BACK_H);

        level1Card.setPosition(leftX, CARD_Y / 0.95f);
        level2Card.setPosition(rightX, CARD_Y / 0.95f);
        backBtn.setPosition(centerX - BACK_W / 2f, BACK_Y);

        addHoverScale(level1Card, 1.02f);
        addHoverScale(level2Card, 1.02f);
        addHoverScale(backBtn, 1.03f);

        level1Card.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (sound != null) {
                    sound.playSound("selected", 1.0f);
                }
                getSceneManager().set(new Level1Scene());
            }
        });

        level2Card.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (sound != null) {
                    sound.playSound("selected", 1.0f);
                }
                getSceneManager().set(new Level2Scene());
            }
        });

        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                goBack();
            }
        });

        stage.addActor(level1Card);
        stage.addActor(level2Card);
        stage.addActor(backBtn);

        Gdx.input.setInputProcessor(stage);

        Keyboard kb = getIOManager().getInputs(Keyboard.class);
        if (kb != null) {
            kb.addBind(Input.Keys.ESCAPE, this::goBack, true);
            kb.addBind(Input.Keys.BACKSPACE, this::goBack, true);
        }
    }

    private BitmapFont makeFont(String path, int size, Color color, int borderWidth) {
        FreeTypeFontGenerator generator =
                new FreeTypeFontGenerator(Gdx.files.internal(path));

        FreeTypeFontGenerator.FreeTypeFontParameter param =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
        param.size = size;
        param.color = color;
        param.borderWidth = borderWidth;
        param.borderColor = Color.BLACK;

        BitmapFont font = generator.generateFont(param);
        generator.dispose();
        return font;
    }

    private Texture makePanelTexture(int width, int height, Color fill, Color border) {
        Pixmap pm = new Pixmap(width, height, Pixmap.Format.RGBA8888);

        pm.setColor(fill);
        pm.fill();

        pm.setColor(border);
        pm.drawRectangle(0, 0, width, height);
        pm.drawRectangle(1, 1, width - 2, height - 2);
        pm.drawRectangle(2, 2, width - 4, height - 4);

        Texture texture = new Texture(pm);
        pm.dispose();
        return texture;
    }

    private void addHoverScale(ImageButton btn, float scale) {
        btn.setTransform(true);
        btn.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer,
                              com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                btn.setScale(scale);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer,
                             com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                btn.setScale(1f);
            }
        });
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

        drawCentered(batch, titleFont, "LEVEL SELECT",
                getUiViewport().getWorldWidth() / 2f, 390f);

        drawLevelCard(batch, level1Card,
                "LEVEL 1",
                "Sunny Road",
                "Normal traffic, no police.");

        drawLevelCard(batch, level2Card,
                "LEVEL 2",
                "Rain Expressway",
                "Wet road, police chase.");

        drawButtonText(batch, backFont, "BACK", backBtn, 0f, 1.2f);

        batch.end();
    }

    private void drawLevelCard(SpriteBatch batch, ImageButton card,
                           String levelText, String subtitle, String desc) {

        float cardX = card.getX();
        float cardY = card.getY();
        float cardW = card.getWidth();
        float cardH = card.getHeight();

        float bannerX = cardX + (cardW - BANNER_W) / 2f;
        float bannerY = cardY + cardH - 78f;

        batch.draw(redButtonUpTexture, bannerX, bannerY, BANNER_W, BANNER_H);

        drawTextCenteredInRect(batch, levelFont, levelText,
                bannerX, bannerY, BANNER_W, BANNER_H, 0f, 1.5f);

        drawCentered(batch, subtitleFont, subtitle, cardX + cardW / 2f, cardY + 98f);

        drawWrappedCentered(batch, descFont, desc, cardX + 28f, cardY + 34f, cardW - 56f);
    }

    private void drawCentered(SpriteBatch batch, BitmapFont font, String text, float centerX, float y) {
        GlyphLayout layout = new GlyphLayout(font, text);
        font.draw(batch, layout, centerX - layout.width / 2f, y);
    }

    private void drawWrappedCentered(SpriteBatch batch, BitmapFont font,
                                     String text, float x, float y, float width) {
        GlyphLayout layout = new GlyphLayout();
        layout.setText(font, text, Color.BLACK, width, 1, true);
        font.draw(batch, layout, x, y + layout.height);
    }

    private void drawTextCenteredInRect(SpriteBatch batch, BitmapFont font, String text,
                                        float x, float y, float w, float h,
                                        float offsetX, float offsetY) {
        GlyphLayout layout = new GlyphLayout(font, text);
        float textX = x + (w - layout.width) / 2f + offsetX + 1f;
        float textY = y + (h + layout.height) / 2f + offsetY - 3f;
        font.draw(batch, layout, textX, textY);
    }

    private void drawButtonText(SpriteBatch batch, BitmapFont font, String text,
                            ImageButton btn, float offsetX, float offsetY) {
        GlyphLayout layout = new GlyphLayout(font, text);

        float x = btn.getX() + (btn.getWidth() - layout.width) / 2f + offsetX;
        float y = btn.getY() + (btn.getHeight() + layout.height) / 2f + offsetY;

        font.draw(batch, layout, x, y);
    }

    private void goBack() {
        if (sound != null) {
            sound.playSound("selected", 1.0f);
        }
        getSceneManager().set(new StartScene());
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
        redButtonUpTexture.dispose();
        redButtonDownTexture.dispose();
        panelTexture.dispose();
        titleFont.dispose();
        levelFont.dispose();
        subtitleFont.dispose();
        descFont.dispose();
        backFont.dispose();
    }
}