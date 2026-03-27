package io.github.raesleg.game.scene;

import java.util.List;

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
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.scene.Scene;

import io.github.raesleg.game.io.Keyboard;

// Rules before actual gameplay
public final class RulesPopupScene extends Scene {

    private final String levelLabel;
    private final List<String> rules;

    private Stage stage;

    private Texture overlayTexture;
    private Texture outerPanelTexture;
    private Texture innerPanelTexture;
    private Texture redButtonUpTexture;
    private Texture redButtonDownTexture;

    private BitmapFont titleFont;
    private BitmapFont bodyFont;
    private BitmapFont buttonFont;

    private ImageButton continueBtn;

    private static final float OUTER_W = 760f;
    private static final float OUTER_H = 430f;

    private static final float INNER_W = 620f;
    private static final float INNER_H = 300f;

    private static final float TITLE_BANNER_W = 320f;
    private static final float TITLE_BANNER_H = 68f;

    private static final float CONTINUE_W = 250f;
    private static final float CONTINUE_H = 70f;

    public RulesPopupScene(String levelLabel, List<String> rules) {
        super();
        this.levelLabel = levelLabel;
        this.rules = rules;
        setTransparent(true);
    }

    @Override
    public void show() {
        stage = new Stage(getUiViewport());

        overlayTexture = makeSolidTexture(new Color(1f, 1f, 1f, 1f));

        outerPanelTexture = makePanelTexture(
                900, 520,
                new Color(0.88f, 0.84f, 0.71f, 0.96f),
                new Color(0.55f, 0.42f, 0.20f, 1f)
        );

        innerPanelTexture = makePanelTexture(
                900, 520,
                new Color(0.96f, 0.94f, 0.86f, 0.98f),
                new Color(0.48f, 0.45f, 0.40f, 1f)
        );

        redButtonUpTexture = new Texture("menu/ButtonLargeRed.png");
        redButtonDownTexture = new Texture("menu/ButtonLargeRedActive.png");

        titleFont = makeFont("fonts/BubblegumSans-Regular.ttf", 30, Color.WHITE, 2);
        bodyFont = makeFont("fonts/BubblegumSans-Regular.ttf", 20, new Color(0.12f, 0.10f, 0.08f, 1f), 0);
        buttonFont = makeFont("fonts/BubblegumSans-Regular.ttf", 28, Color.WHITE, 2);

        ImageButton.ImageButtonStyle redStyle = new ImageButton.ImageButtonStyle();
        redStyle.imageUp = new TextureRegionDrawable(new TextureRegion(redButtonUpTexture));
        redStyle.imageOver = new TextureRegionDrawable(new TextureRegion(redButtonDownTexture));
        redStyle.imageDown = new TextureRegionDrawable(new TextureRegion(redButtonDownTexture));

        continueBtn = new ImageButton(redStyle);
        continueBtn.setSize(CONTINUE_W, CONTINUE_H);

        float worldW = getUiViewport().getWorldWidth();
        continueBtn.setPosition(worldW / 2f - CONTINUE_W / 2f, 120f);

        continueBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closePopup();
            }
        });

        stage.addActor(continueBtn);
        Gdx.input.setInputProcessor(stage);
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

    private Texture makeSolidTexture(Color color) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.fill();
        Texture texture = new Texture(pm);
        pm.dispose();
        return texture;
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

    @Override
    public void update(float deltaTime) {
        stage.act(deltaTime);

        Keyboard kb = getIOManager().getInputs(Keyboard.class);
        if (kb != null && kb.justPressed(Constants.ACTION)) {
            closePopup();
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            closePopup();
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        Gdx.gl.glEnable(GL20.GL_BLEND);

        getUiViewport().apply();
        getUiCamera().update();

        float worldW = getUiViewport().getWorldWidth();
        float worldH = getUiViewport().getWorldHeight();

        float outerX = worldW / 2f - OUTER_W / 2f;
        float outerY = worldH / 2f - OUTER_H / 2f + 20f;

        float innerX = worldW / 2f - INNER_W / 2f;
        float innerY = outerY + 55f;

        float bannerX = worldW / 2f - TITLE_BANNER_W / 2f;
        float bannerY = outerY + OUTER_H - 58f;

        batch.setProjectionMatrix(getUiCamera().combined);
        batch.begin();

        // dim background
        batch.setColor(0f, 0f, 0f, 0.42f);
        batch.draw(overlayTexture, 0, 0, worldW, worldH);
        batch.setColor(Color.WHITE);

        // outer and inner boxes
        batch.draw(outerPanelTexture, outerX, outerY, OUTER_W, OUTER_H);
        batch.draw(innerPanelTexture, innerX, innerY, INNER_W, INNER_H);

        // title banner
        batch.draw(redButtonUpTexture, bannerX, bannerY, TITLE_BANNER_W, TITLE_BANNER_H);
        drawTextCenteredInRect(batch, titleFont, levelLabel + " RULES",
                bannerX, bannerY, TITLE_BANNER_W, TITLE_BANNER_H, 0f, 1.5f);

        // main rule lines — dynamically rendered from injected rules list
        float ruleY = innerY + 260f;
        float ruleSpacing = 35f;
        for (String rule : rules) {
            drawCentered(batch, bodyFont, rule, worldW / 2f, ruleY);
            ruleY -= ruleSpacing;
        }

        batch.end();

        stage.draw();

        batch.begin();
        drawButtonText(batch, buttonFont, "CONTINUE", continueBtn, 0f, 1.5f);
        batch.end();
    }

    private void drawCentered(SpriteBatch batch, BitmapFont font, String text, float centerX, float y) {
        GlyphLayout layout = new GlyphLayout(font, text);
        font.draw(batch, layout, centerX - layout.width / 2f, y);
    }

    private void drawTextCenteredInRect(SpriteBatch batch, BitmapFont font, String text,
                                        float x, float y, float w, float h,
                                        float offsetX, float offsetY) {
        GlyphLayout layout = new GlyphLayout(font, text);
        float textX = x + (w - layout.width) / 2f + offsetX;
        float textY = y + (h + layout.height) / 2f + offsetY;
        font.draw(batch, layout, textX, textY);
    }

    private void drawButtonText(SpriteBatch batch, BitmapFont font, String text,
                                ImageButton btn, float offsetX, float offsetY) {
        GlyphLayout layout = new GlyphLayout(font, text);
        float x = btn.getX() + (btn.getWidth() - layout.width) / 2f + offsetX;
        float y = btn.getY() + (btn.getHeight() + layout.height) / 2f + offsetY;
        font.draw(batch, layout, x, y);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        overlayTexture.dispose();
        outerPanelTexture.dispose();
        innerPanelTexture.dispose();
        redButtonUpTexture.dispose();
        redButtonDownTexture.dispose();
        titleFont.dispose();
        bodyFont.dispose();
        buttonFont.dispose();
    }

    private void closePopup() {
        getSceneManager().pop();
    }
}