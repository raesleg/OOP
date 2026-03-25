package io.github.raesleg.game.state;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.raesleg.engine.entity.TextureObject;

/**
 * DashboardUI — HUD with dashboard.png for speed, star.png for wanted,
 * and an optional police-distance progress bar for Level 2.
 * <p>
 * <b>SRP Composition:</b> Delegates score popup lifecycle to
 * {@link ScorePopupManager}. This class is responsible only for
 * HUD layout and rendering.
 */
public class DashboardUI implements IDashboardObserver, Disposable {

    private static final int MAX_WANTED_STARS = 5;
    private static final float LABEL_SCALE = 2f;

    /* ── Scene2D ── */
    private final Stage stage;
    private final BitmapFont font;
    private final BitmapFont speedFont;

    /* ── Text labels ── */
    private final Label scoreLabel;
    private final Label progressLabel;

    // start finish flags
    private final Texture finishIcon;
    private final Texture startIcon;

    /* ── Textures ── */
    private final Texture dashboardTex;
    private final Texture starTex;
    private final Texture carTex;
    private final Texture policeTex;
    private final Texture pixelTex;

    /* ── Cached state ── */
    private int currentScore;
    private float currentProgress;
    private int currentRulesBroken;
    private int currentSpeed;

    /* ── Incremental score display ── */
    private float displayScore;
    private int targetScore;
    private static final float SCORE_LERP_SPEED = 200f;

    /* ── Score popups (SRP — delegated to ScorePopupManager) ── */
    private final ScorePopupManager popupManager;

    /* ── HUD rendering (SRP — delegated to HudRenderer) ── */
    private final HudRenderer hudRenderer;

    /* ── Fuel bar ── */
    private float currentFuel;
    private final Texture chargeTex;

    /* ── Police distance mode (Level 2) ── */
    private boolean policeDistanceMode;
    private float policeDistance; // 0.0 = caught, 1.0 = far away

    public DashboardUI(Viewport uiViewport) {
        font = new BitmapFont();
        font.getData().setScale(LABEL_SCALE);
        speedFont = new BitmapFont();
        speedFont.getData().setScale(2.5f);

        Label.LabelStyle style = new Label.LabelStyle(font, Color.WHITE);
        scoreLabel = new Label("", style);
        progressLabel = new Label("", style);

        currentScore = 0;
        currentProgress = 0f;
        currentRulesBroken = 0;
        currentSpeed = 0;
        policeDistanceMode = false;
        policeDistance = 1f;
        displayScore = 0f;
        targetScore = 0;
        currentFuel = 1f;

        // Load textures
        dashboardTex = TextureObject.getOrLoadTexture("dashboard.png");
        starTex = TextureObject.getOrLoadTexture("star.png");
        carTex = TextureObject.getOrLoadTexture("car.png");
        policeTex = TextureObject.getOrLoadTexture("policecar_noflash.png");
        finishIcon = TextureObject.getOrLoadTexture("finish_flag.png");
        startIcon = TextureObject.getOrLoadTexture("start_flag.png");
        chargeTex = TextureObject.getOrLoadTexture("charge.png");

        // 1x1 white pixel for drawing lines/bars
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixelTex = new Texture(pm);
        pm.dispose();

        // Score popup manager (SRP extraction)
        popupManager = new ScorePopupManager();

        // HUD renderer (SRP extraction)
        hudRenderer = new HudRenderer(
                dashboardTex, starTex, carTex, policeTex, pixelTex,
                finishIcon, startIcon, chargeTex, font, speedFont);

        // Build Scene2D layout (score + progress text on top)
        stage = new Stage(uiViewport);
        Table root = new Table();
        root.setFillParent(true);
        root.pad(10f);

        Table topRow = new Table();
        topRow.add(scoreLabel).left().expandX();
        topRow.add(progressLabel).center().expandX();
        // Wanted area is handled by custom draw (star.png images)
        topRow.add().right().expandX().width(200f);
        root.add(topRow).expandX().fillX().top();
        root.row();
        root.add().expand().fill();

        stage.addActor(root);

        refreshAllLabels();
    }

    public void setPoliceDistanceMode(boolean enabled) {
        this.policeDistanceMode = enabled;
    }

    public void onPoliceDistanceUpdated(float normalizedDistance) {
        this.policeDistance = Math.max(0f, Math.min(1f, normalizedDistance));
    }

    /* ── IDashboardObserver ── */

    @Override
    public void onScoreUpdated(int score) {
        targetScore = score;
    }

    @Override
    public void onProgressUpdated(float percentage) {
        currentProgress = Math.max(0f, Math.min(1f, percentage));
        progressLabel.setText(""); // drawn graphically
    }

    @Override
    public void onRuleBroken(int totalBroken) {
        currentRulesBroken = Math.max(0, Math.min(totalBroken, MAX_WANTED_STARS));
        // Stars drawn in custom draw, no label needed
    }

    @Override
    public void onSpeedChanged(int speed) {
        currentSpeed = speed;
        // Speed drawn on dashboard.png, no label needed
    }

    @Override
    public void onFuelUpdated(float percentage) {
        currentFuel = Math.max(0f, Math.min(1f, percentage));
    }

    /* ── Lifecycle ── */

    public void act(float deltaTime) {
        // Incremental score lerp
        if (displayScore < targetScore) {
            displayScore = Math.min(targetScore, displayScore + SCORE_LERP_SPEED * deltaTime);
        } else if (displayScore > targetScore) {
            displayScore = Math.max(targetScore, displayScore - SCORE_LERP_SPEED * deltaTime);
        }
        currentScore = (int) displayScore;
        scoreLabel.setText("SCORE: " + currentScore);

        // Delegate popup animation to ScorePopupManager
        popupManager.update(deltaTime);

        stage.act(deltaTime);
    }

    public void draw() {
        // Draw Scene2D labels (score, progress text when not in police mode)
        stage.draw();

        // Draw custom textures on the stage's batch
        SpriteBatch batch = (SpriteBatch) stage.getBatch();
        stage.getViewport().apply();
        batch.setProjectionMatrix(stage.getCamera().combined);
        batch.begin();

        drawDashboardSpeed(batch);
        drawWantedStars(batch);
        if (policeDistanceMode) {
            drawPoliceDistanceBar(batch);
        } else {
            drawProgressBar(batch);
        }
        drawFuelBar(batch);
        popupManager.render(batch);

        batch.end();
    }

    /** Spawns a floating score popup (e.g. "+50" or "-100"). */
    public void showScorePopup(int delta) {
        popupManager.show(delta);
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        font.dispose();
        speedFont.dispose();
        popupManager.dispose();
        // Only dispose locally-created textures. Shared Flyweight textures
        // (dashboardTex, starTex, carTex, etc.) are owned by TextureObject's
        // static cache and disposed at app shutdown via disposeAllTextures().
        pixelTex.dispose();
    }

    /* ── Custom drawing ── */

    private void drawDashboardSpeed(SpriteBatch batch) {
        hudRenderer.drawDashboardSpeed(batch, currentSpeed);
    }

    private void drawWantedStars(SpriteBatch batch) {
        hudRenderer.drawWantedStars(batch, currentRulesBroken);
    }

    private void drawPoliceDistanceBar(SpriteBatch batch) {
        hudRenderer.drawPoliceDistanceBar(batch, policeDistance);
    }

    /**
     * Graphical progress bar — white line with car icon moving right towards GOAL.
     */
    private void drawProgressBar(SpriteBatch batch) {
        hudRenderer.drawProgressBar(batch, currentProgress);
    }

    private void drawFuelBar(SpriteBatch batch) {
        hudRenderer.drawFuelBar(batch, currentFuel);
    }

    private void refreshAllLabels() {
        onScoreUpdated(currentScore);
        onProgressUpdated(currentProgress);
        onRuleBroken(currentRulesBroken);
        onSpeedChanged(currentSpeed);
    }
}
