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

    /* ── Fuel bar ── */
    private float currentFuel;
    private final Texture chargeTex;

    /* ── Police distance mode (Level 2) ── */
    private boolean policeDistanceMode;
    private float policeDistance; // 0.0 = caught, 1.0 = far away

    /* ── Layout constants ── */
    private static final float DASHBOARD_W = 280f;
    private static final float DASHBOARD_H = 140f;
    private static final float STAR_SIZE = 28f;
    private static final float ICON_SIZE = 28f;
    private static final float BAR_WIDTH = 300f;

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

        // Score popup manager (SRP extraction)
        popupManager = new ScorePopupManager();

        // 1x1 white pixel for drawing lines/bars
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixelTex = new Texture(pm);
        pm.dispose();

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
        // Dashboard.png at bottom-right
        float dx = 1280f - DASHBOARD_W - 10f;
        float dy = 5f;
        batch.draw(dashboardTex, dx, dy, DASHBOARD_W, DASHBOARD_H);

        // Speed number in the yellow box area (right portion of dashboard)
        String speedText = currentSpeed + "";
        speedFont.setColor(Color.WHITE);
        speedFont.draw(batch, speedText,
                dx + DASHBOARD_W * 0.62f,
                dy + DASHBOARD_H * 0.58f);
        // "KM/H" label below speed
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "KM/H",
                dx + DASHBOARD_W * 0.60f,
                dy + DASHBOARD_H * 0.30f);
        font.setColor(Color.WHITE);
    }

    private void drawWantedStars(SpriteBatch batch) {
        // Position: top-right area
        float startX = 1280f - 10f - (MAX_WANTED_STARS * (STAR_SIZE + 4f));
        float y = 720f - 14f - STAR_SIZE;

        // "WANTED:" text
        font.draw(batch, "WANTED:", startX - 160f, y + STAR_SIZE - 2f);

        for (int i = 0; i < MAX_WANTED_STARS; i++) {
            float x = startX + i * (STAR_SIZE + 4f);
            if (i < currentRulesBroken) {
                // Filled star
                batch.setColor(1f, 1f, 1f, 1f);
            } else {
                // Empty slot — dim star
                batch.setColor(1f, 1f, 1f, 0.2f);
            }
            batch.draw(starTex, x, y, STAR_SIZE, STAR_SIZE);
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void drawPoliceDistanceBar(SpriteBatch batch) {
        // Centered at top, between score and wanted
        float barX = (1280f - BAR_WIDTH) / 2f;
        float barY = 720f - 30f;
        float lineH = 6f;

        // Draw bar track (dark background)
        batch.setColor(0.3f, 0.3f, 0.3f, 0.6f);
        batch.draw(pixelTex, barX, barY - lineH / 2f, BAR_WIDTH, lineH);

        // Draw colored fill: green (safe) on the right, red (danger) on the left
        // policeDistance: 1 = far (safe), 0 = caught (danger)
        // Fill from left to (1 - policeDistance) to show danger zone
        float dangerWidth = (1f - policeDistance) * BAR_WIDTH;
        if (dangerWidth > 0) {
            batch.setColor(0.9f, 0.2f, 0.2f, 0.7f);
            batch.draw(pixelTex, barX, barY - lineH / 2f, dangerWidth, lineH);
        }
        batch.setColor(1f, 1f, 1f, 1f);

        // Car icon on the right (player — fixed position)
        float carX = barX + BAR_WIDTH - ICON_SIZE;
        float carY = barY - ICON_SIZE / 2f;
        batch.draw(carTex, carX, carY, ICON_SIZE, ICON_SIZE);

        // Police icon position based on distance (left = far, right = close)
        float policeX = barX + (1f - policeDistance) * (BAR_WIDTH - ICON_SIZE * 2f);
        batch.draw(policeTex, policeX, carY, ICON_SIZE, ICON_SIZE);

        // Labels
        font.setColor(Color.LIGHT_GRAY);
        font.getData().setScale(1.2f);
        font.draw(batch, "POLICE", barX - 100f, barY + 8f);
        font.getData().setScale(LABEL_SCALE);
        font.setColor(Color.WHITE);
    }

    /**
     * Graphical progress bar — white line with car icon moving right towards GOAL.
     */
    private void drawProgressBar(SpriteBatch batch) {
        float barX = (1280f - BAR_WIDTH) / 2f;
        float barY = 720f - 22f;
        float lineH = 3f;

        // White line
        batch.setColor(Color.WHITE);
        batch.draw(pixelTex, barX, barY - lineH / 2f, BAR_WIDTH, lineH);

        // "S" label at start
        float sflagWidth = 50f;
        float sflagheight = 30f;
        float sflagX = barX - 50f;
        float sflagY = barY - sflagWidth / 2f + 10f;

        batch.draw(startIcon, sflagX, sflagY, sflagWidth, sflagheight);

        // "Finish_flag.png" picture at finish
        float fflagSize = 28f;
        float fflagX = barX + BAR_WIDTH + 6f;
        float fflagY = barY - fflagSize / 2f - 6f;

        batch.draw(finishIcon, fflagX, fflagY, fflagSize, fflagSize);

        // Car icon moving right along the bar
        float carW = 24f;
        float carH = 24f;
        float carX = barX + currentProgress * (BAR_WIDTH - carW);
        float carY = barY - carH / 2f;
        batch.setColor(Color.WHITE);
        batch.draw(carTex, carX, carY, carW, carH);
    }

    private void drawFuelBar(SpriteBatch batch) {
        float barX = 20f;
        float barY = 30f;
        float barW = 200f;
        float barH = 18f;
        float iconSize = 32f;

        // Charge icon to the left of the bar
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(chargeTex, barX, barY - 6f, iconSize, iconSize);

        float fillX = barX + iconSize + 6f;

        // Background track
        batch.setColor(0.2f, 0.2f, 0.2f, 0.7f);
        batch.draw(pixelTex, fillX, barY, barW, barH);

        // Fill — color shifts green → yellow → red
        float r, g;
        if (currentFuel > 0.5f) {
            r = 1f - (currentFuel - 0.5f) * 2f;
            g = 1f;
        } else {
            r = 1f;
            g = currentFuel * 2f;
        }
        batch.setColor(r, g, 0.15f, 0.9f);
        batch.draw(pixelTex, fillX, barY, barW * currentFuel, barH);

        // "FUEL" label
        batch.setColor(1f, 1f, 1f, 1f);
        font.setColor(Color.WHITE);
        font.getData().setScale(1.4f);
        font.draw(batch, "FUEL", fillX, barY + barH + 22f);
        font.getData().setScale(LABEL_SCALE);
    }

    private void refreshAllLabels() {
        onScoreUpdated(currentScore);
        onProgressUpdated(currentProgress);
        onRuleBroken(currentRulesBroken);
        onSpeedChanged(currentSpeed);
    }
}
