package io.github.raesleg.game.state;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * DashboardUI — Concrete text-based HUD for the lane-dodger game.
 * <p>
 * Implements {@link IDashboardObserver} so it reacts to game-state changes
 * pushed by the scene (Observer pattern). Owns a private Scene2D
 * {@link Stage} rendered on the UI viewport so all labels are pixel-stable
 * and independent of the world camera.
 * <p>
 * <b>Layout (1280 × 720 virtual coords):</b>
 * 
 * <pre>
 * +---------------------------------------------------------------+
 * | SCORE: 0       [S] ----------C---------- [F]   WANTED: [ ]    |
 * |                                                                |
 * |                        (gameplay area)                         |
 * |                                                                |
 * |                                                SPEED: 0 KM/H  |
 * +---------------------------------------------------------------+
 * </pre>
 * <p>
 * <b>Architectural notes (context.txt compliance):</b>
 * <ul>
 * <li>Lives in the {@code demo} package — pure game-layer UI, no engine imports
 * beyond
 * what Scene2D requires (LibGDX is a framework dependency, not engine
 * code).</li>
 * <li>Does <b>not</b> import any {@code engine.*} class — fully decoupled from
 * the
 * engine render loop.</li>
 * <li>The owning Scene ({@code GameScene}) calls {@link #act(float)},
 * {@link #draw()}, {@link #resize(int, int)}, and {@link #dispose()} at the
 * appropriate lifecycle points (Scene Sovereignty).</li>
 * </ul>
 */
public class DashboardUI implements IDashboardObserver, Disposable {

    /* ── Constants ── */
    private static final int PROGRESS_BAR_WIDTH = 20; // number of dash characters
    private static final int MAX_WANTED_STARS = 3;
    private static final float LABEL_SCALE = 2f;

    /* ── Scene2D ── */
    private final Stage stage;
    private final BitmapFont font;

    /* ── Labels ── */
    private final Label scoreLabel;
    private final Label progressLabel;
    private final Label wantedLabel;
    private final Label speedLabel;

    /* ── Cached state (avoids rebuilding strings every frame) ── */
    private int currentScore;
    private float currentProgress; // 0.0 – 1.0
    private int currentRulesBroken;
    private int currentSpeed;

    /**
     * Creates the HUD. The caller provides the <b>UI viewport</b>
     * (typically {@code Scene.getUiViewport()}) so the Stage shares the
     * same pixel-stable projection as the rest of the HUD layer.
     *
     * @param uiViewport the FitViewport used for UI rendering
     */
    public DashboardUI(Viewport uiViewport) {
        font = new BitmapFont();
        font.getData().setScale(LABEL_SCALE);

        Label.LabelStyle style = new Label.LabelStyle(font, Color.WHITE);

        scoreLabel = new Label("", style);
        progressLabel = new Label("", style);
        wantedLabel = new Label("", style);
        speedLabel = new Label("", style);

        // Apply initial state
        currentScore = 0;
        currentProgress = 0f;
        currentRulesBroken = 0;
        currentSpeed = 0;
        refreshAllLabels();

        // Build layout
        stage = new Stage(uiViewport);

        Table root = new Table();
        root.setFillParent(true);
        root.pad(10f);

        // ── Top row: SCORE | progress bar | WANTED ──
        Table topRow = new Table();
        topRow.add(scoreLabel).left().expandX();
        topRow.add(progressLabel).center().expandX();
        topRow.add(wantedLabel).right().expandX();

        root.add(topRow).expandX().fillX().top();
        root.row();

        // ── Spacer (gameplay area) ──
        root.add().expand().fill();
        root.row();

        // ── Bottom row: speed on the right ──
        root.add(speedLabel).right().bottom().expandX();

        stage.addActor(root);
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * IDashboardObserver callbacks (Observer pattern)
     * ══════════════════════════════════════════════════════════════
     */

    @Override
    public void onScoreUpdated(int score) {
        currentScore = score;
        scoreLabel.setText("SCORE: " + currentScore);
    }

    @Override
    public void onProgressUpdated(float percentage) {
        currentProgress = Math.max(0f, Math.min(1f, percentage));
        progressLabel.setText(buildProgressBar(currentProgress));
    }

    @Override
    public void onRuleBroken(int totalBroken) {
        currentRulesBroken = Math.max(0, Math.min(totalBroken, MAX_WANTED_STARS));
        wantedLabel.setText(buildWantedString(currentRulesBroken));
    }

    @Override
    public void onSpeedChanged(int speed) {
        currentSpeed = speed;
        speedLabel.setText("SPEED: " + currentSpeed + " KM/H");
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * Lifecycle — called by the owning Scene
     * ══════════════════════════════════════════════════════════════
     */

    /** Advance Scene2D actions/animations (call from Scene.update). */
    public void act(float deltaTime) {
        stage.act(deltaTime);
    }

    /**
     * Render all HUD labels (call from Scene.render, after applying the UI
     * viewport).
     */
    public void draw() {
        stage.draw();
    }

    /** Forward resize events so the Stage viewport stays in sync. */
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        font.dispose();
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * Helper methods
     * ══════════════════════════════════════════════════════════════
     */

    /**
     * Builds the text-based progress bar:
     * {@code [S] ----------C---------- [F]}
     *
     * @param percentage 0.0 (start) to 1.0 (finish)
     * @return the formatted progress string
     */
    private String buildProgressBar(float percentage) {
        int carIndex = Math.round(percentage * PROGRESS_BAR_WIDTH);
        carIndex = Math.max(0, Math.min(carIndex, PROGRESS_BAR_WIDTH));

        StringBuilder sb = new StringBuilder(PROGRESS_BAR_WIDTH + 10);
        sb.append("[S] ");
        for (int i = 0; i <= PROGRESS_BAR_WIDTH; i++) {
            sb.append(i == carIndex ? 'C' : '-');
        }
        sb.append(" [F]");
        return sb.toString();
    }

    /**
     * Builds the wanted-level display:
     * <ul>
     * <li>0 broken → {@code WANTED: [ ]}</li>
     * <li>1 broken → {@code WANTED: [X]}</li>
     * <li>2 broken → {@code WANTED: [X] [X]}</li>
     * <li>3 broken → {@code WANTED: [X] [X] [X]}</li>
     * </ul>
     */
    private String buildWantedString(int rulesBroken) {
        if (rulesBroken <= 0) {
            return "WANTED: [ ]";
        }
        StringBuilder sb = new StringBuilder("WANTED: ");
        for (int i = 0; i < rulesBroken; i++) {
            if (i > 0)
                sb.append(' ');
            sb.append("[X]");
        }
        return sb.toString();
    }

    /** Sets every label to match the current cached state. */
    private void refreshAllLabels() {
        onScoreUpdated(currentScore);
        onProgressUpdated(currentProgress);
        onRuleBroken(currentRulesBroken);
        onSpeedChanged(currentSpeed);
    }
}
