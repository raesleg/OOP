package io.github.raesleg.game.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/* Engine Imports */
import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.collision.CollisionManager;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.event.EventBus;
import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.movement.MovementManager;
import io.github.raesleg.engine.movement.UserControlled;
import io.github.raesleg.engine.physics.PhysicsWorld;
import io.github.raesleg.engine.scene.Scene;

/* Game Imports */
import io.github.raesleg.game.GameConstants;
import io.github.raesleg.game.collision.GameCollisionHandler;
import io.github.raesleg.game.entities.misc.ExplosionOverlay;
import io.github.raesleg.game.entities.misc.Particle;
import io.github.raesleg.game.entities.misc.Trees;
import io.github.raesleg.game.factory.BoundaryFactory;
import io.github.raesleg.game.factory.PlayerFactory;
import io.github.raesleg.game.entities.vehicles.PlayerCar;
import io.github.raesleg.game.event.FuelDepletedEvent;
import io.github.raesleg.game.event.PickupCollectedEvent;
import io.github.raesleg.game.event.ScoreChangedEvent;
import io.github.raesleg.game.io.Keyboard;
import io.github.raesleg.game.rules.RuleManager;
import io.github.raesleg.game.state.AudioController;
import io.github.raesleg.game.state.DashboardUI;
import io.github.raesleg.game.state.FuelController;
import io.github.raesleg.game.state.FuelSystem;
import io.github.raesleg.game.state.SpeedScrollController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * BaseGameScene — Abstract template for all gameplay levels.
 * <p>
 * Uses the <b>Template Method</b> pattern to define a strict contract for
 * level scenes. Common infrastructure (physics, managers, player car, HUD,
 * input, audio) is initialised here. Subclasses customise behaviour through
 * well-defined hooks.
 * <p>
 * <b>SRP Composition:</b> Delegates speed/scroll to
 * {@link SpeedScrollController},
 * fuel lifecycle to {@link FuelController}, and audio management to
 * {@link AudioController}. Cross-system communication uses the
 * {@link EventBus}.
 * <p>
 * <b>Scene Sovereignty:</b> Each level owns its own EntityManager,
 * MovementManager, CollisionManager, PhysicsWorld, and EventBus.
 */
public abstract class BaseGameScene extends Scene {

    /* ── Composed systems (SRP) ── */
    private SpeedScrollController speedScroll;
    private FuelController fuelController;
    private AudioController audioController;
    private EventBus eventBus;

    /* ── Common state ── */
    private DashboardUI dashboard;
    private boolean isPaused;
    private float gameTime;
    private int score;
    private float scoreAccumulator;
    private int scoreBonus;
    private int rulesBroken;
    private int crashCount;
    private boolean instantFail;
    private String instantFailReason;

    /*
     * ── Shared rule/command infrastructure (DIP — created here, injected into
     * subclasses) ──
     */
    private RuleManager ruleManager;
    private io.github.raesleg.engine.io.CommandHistory commandHistory;

    /* ── Physics ── */
    private PhysicsWorld world;

    /* ── Collision ── */
    private GameCollisionHandler collisionHandler;

    /* ── Player ── */
    private PlayerCar playerCar;

    /* ── Scenery ── */
    private Trees trees;

    /* ── Rendering ── */
    private ShapeRenderer shapeRenderer;
    private RoadRenderer roadRenderer;

    /* ── Visible camera bounds (updated each frame in render()) ── */
    private float visMinX, visMinY, visMaxX, visMaxY;

    /* ── Audio (direct reference for subclass access) ── */
    private SoundDevice sound;

    /* ── Explosion game-over delay ── */
    private boolean gameOverPending;
    private float gameOverTimer;
    private LevelResult pendingResult;

    /* ── Level-end conditions (OCP — evaluated in registration order) ── */
    private final List<ILevelEndCondition> endConditions = new ArrayList<>();

    /* ── World dimensions (metres) ── */
    private final float worldW = VIRTUAL_WIDTH / Constants.PPM;
    private final float worldH = VIRTUAL_HEIGHT / Constants.PPM;

    /* ── Constructor ── */

    protected BaseGameScene() {
        super();
        this.isPaused = false;
        this.gameTime = 0f;
        this.score = 0;
        this.scoreAccumulator = 0f;
        this.scoreBonus = 0;
        this.rulesBroken = 0;
        this.crashCount = 0;
        this.instantFail = false;
        this.instantFailReason = "";
        this.gameOverPending = false;
        this.gameOverTimer = 0f;
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * Abstract hooks — subclasses MUST implement
     * ══════════════════════════════════════════════════════════════
     */

    protected abstract float getLevelLength();

    protected abstract float getMaxSpeed();

    protected abstract float getAcceleration();

    protected abstract float getBrakeRate();

    protected abstract String getBgmPath();

    protected abstract void initLevelData();

    protected abstract void updateGame(float deltaTime);

    protected abstract boolean isGameOver();

    protected abstract String getLevelName();

    protected abstract BaseGameScene createRetryScene();

    protected abstract String getGameOverReason();

    protected abstract float getMaxScrollPixelsPerSecond();

    /*
     * ══════════════════════════════════════════════════════════════
     * Optional hooks — subclasses MAY override
     * ══════════════════════════════════════════════════════════════
     */

    protected void renderLevelEffects(ShapeRenderer sr, SpriteBatch batch) {
    }

    /** Camera zoom — subclasses may override for a wider/narrower view. */
    protected float getCameraZoom() {
        return GameConstants.CAMERA_ZOOM;
    }

    protected void disposeLevelData() {
    }

    protected List<String> getViolationLog() {
        return Collections.emptyList();
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * Viewport factory — ExtendViewport for gameplay
     * ══════════════════════════════════════════════════════════════
     */

    @Override
    protected Viewport createViewport(OrthographicCamera cam) {
        return new ExtendViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, cam);
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * Scene lifecycle — Template Method
     * ══════════════════════════════════════════════════════════════
     */

    @Override
    public final void show() {
        Gdx.input.setInputProcessor(null);

        shapeRenderer = new ShapeRenderer();
        roadRenderer = new RoadRenderer();
        dashboard = new DashboardUI(getUiViewport());

        /* EventBus — central nervous system for cross-system communication */
        eventBus = new EventBus();

        /* Physics world: zero gravity (top-down) */
        world = new PhysicsWorld(new Vector2(0, 0));

        sound = getIOManager().getSound();

        /* Shared rule/command infrastructure — injected into subclasses (DIP) */
        ruleManager = new RuleManager();
        commandHistory = new io.github.raesleg.engine.io.CommandHistory();

        /* Composed systems (SRP extraction) */
        speedScroll = new SpeedScrollController(
                getMaxSpeed(), getAcceleration(), getBrakeRate(), getMaxScrollPixelsPerSecond());

        fuelController = new FuelController(
                GameConstants.FUEL_DRAIN_RATE, GameConstants.FUEL_RECHARGE_AMOUNT, eventBus);

        audioController = new AudioController(sound);

        /* EventBus wiring — systems communicate via events */
        eventBus.subscribe(FuelDepletedEvent.class, e -> setInstantFail(true, "Ran out of fuel!"));
        eventBus.subscribe(ScoreChangedEvent.class, e -> addScore(e.getDelta()));
        eventBus.subscribe(PickupCollectedEvent.class, e -> handlePickup());

        /* Managers (Scene Sovereignty) */
        setEntityManager(new EntityManager());
        setMovementManager(new MovementManager(world, getEntityManager()));
        collisionHandler = new GameCollisionHandler(getEntityManager(), sound);
        setCollisionManager(new CollisionManager(world, collisionHandler));

        /* Road boundary walls — delegated to factory (SRP) */
        BoundaryFactory.createBoundaries(world, worldW, worldH);

        /* Player car — delegated to factory (SRP) */
        Keyboard kb = getIOManager().getInputs(Keyboard.class);
        UserControlled user = new UserControlled(kb);

        playerCar = PlayerFactory.create(world, getEntityManager(), user);

        kb.bindAction(Input.Keys.A, Constants.LEFT);
        kb.bindAction(Input.Keys.LEFT, Constants.LEFT);
        kb.bindAction(Input.Keys.D, Constants.RIGHT);
        kb.bindAction(Input.Keys.RIGHT, Constants.RIGHT);
        kb.bindAction(Input.Keys.W, Constants.UP);
        kb.bindAction(Input.Keys.UP, Constants.UP);
        kb.bindAction(Input.Keys.S, Constants.DOWN);
        kb.bindAction(Input.Keys.DOWN, Constants.DOWN);
        kb.bindAction(Input.Keys.SPACE, Constants.ACTION);
        kb.addBind(Input.Keys.ESCAPE, this::openPause, true);
        kb.addBind(Input.Keys.M, () -> audioController.toggleMute(), true);

        /* Wire keyboard to speed controller */
        speedScroll.setKeyboard(kb);

        /* Scenery trees */
        trees = new Trees(8, getEntityManager());

        /* BGM via AudioController */
        audioController.startBgm(getBgmPath());

        /* Common scene sounds */
        sound.addSound("drive", "car_sound.wav");
        sound.addSound("explosion", "crash_sound.wav");
        sound.addSound("accelerate", "accelerating.wav");
        sound.addSound("brake", "braking.wav");
        sound.addSound("explosion_big", "explosion.wav");
        sound.addSound("reward", "rewardsound.mp3");
        sound.addSound("negative", "negativesound.mp3");
        sound.addSound("gameover", "gameover_sound.wav");
        sound.addSound("win", "winning_sound.wav");

        /* Level-specific setup (Template Method hook) */
        initLevelData();

        /*
         * Register base level-end conditions (OCP).
         * NOTE: checkCrashExplosion is NOT registered here — subclasses
         * that want crash-explosion behaviour must register it explicitly
         * in initLevelData() via addEndCondition(this::checkCrashExplosion).
         * This allows Level 2 to opt out of explosions on crash.
         */
        addEndCondition(this::checkWinCondition);
        addEndCondition(this::checkInstantFail);
        addEndCondition(this::checkSubclassGameOver);
    }

    @Override
    public final void update(float deltaTime) {
        if (isPaused) {
            audioController.onPause();
            return;
        }

        /* Explosion delay — keep rendering entities but freeze gameplay */
        if (gameOverPending) {
            getEntityManager().update(deltaTime);
            gameOverTimer -= deltaTime;
            if (gameOverTimer <= 0f) {
                getSceneManager().set(
                        new ResultsScene(pendingResult, () -> createRetryScene()));
            }
            return;
        }

        gameTime += deltaTime;

        getEntityManager().update(deltaTime);
        getMovementManager().update(deltaTime);

        /* Speed/scroll — fully delegated to SpeedScrollController (SRP) */
        speedScroll.update(deltaTime);

        /* Dashboard updates — score only increases when moving toward goal */
        float simSpeed = speedScroll.getSimulatedSpeed();
        if (simSpeed > 0.5f) {
            scoreAccumulator += deltaTime * GameConstants.SCORE_RATE_PER_SECOND;
        }
        score = (int) scoreAccumulator + scoreBonus;

        trees.update(simSpeed, deltaTime);

        float scrollOffset = speedScroll.getScrollOffset();
        float progress = Math.min(1f, (-scrollOffset) / getLevelLength());

        dashboard.onScoreUpdated(score);
        dashboard.onSpeedChanged(Math.round(simSpeed));
        dashboard.onProgressUpdated(progress);
        dashboard.onRuleBroken(rulesBroken);
        dashboard.act(deltaTime);

        /* Delegate fuel to extracted system */
        fuelController.setCurrentSpeed(simSpeed);
        fuelController.update(deltaTime);
        dashboard.onFuelUpdated(fuelController.getFuel());

        /* Delegate audio to extracted system */
        Keyboard kb = getIOManager().getInputs(Keyboard.class);

        audioController.setPlayerMoving(playerCar.isMoving());
        audioController.setAccelerating(kb.isHeld(Constants.UP) || kb.isHeld(Constants.LEFT) || kb.isHeld(Constants.RIGHT));
        audioController.setBraking(kb.isHeld(Constants.DOWN));

        audioController.update(deltaTime);

        /* Level-specific update (Template Method hook) */
        updateGame(deltaTime);

        /* Template Method — check win/lose conditions */
        checkLevelEnd();
    }

    @Override
    public final void render(SpriteBatch batch) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        getViewport().apply();
        /* Apply camera zoom and look-ahead */
        getCamera().zoom = getCameraZoom();
        getCamera().position.set(
                VIRTUAL_WIDTH * 0.5f,
                VIRTUAL_HEIGHT * 0.5f + GameConstants.CAMERA_LOOK_AHEAD,
                0f);
        getCamera().update();

        /* Compute visible world-space bounds (accounts for zoom + ExtendViewport) */
        float halfVisW = (getViewport().getWorldWidth() / 2f) * getCameraZoom();
        float halfVisH = (getViewport().getWorldHeight() / 2f) * getCameraZoom();
        float camX = getCamera().position.x;
        float camY = getCamera().position.y;
        visMinX = camX - halfVisW;
        visMinY = camY - halfVisH;
        visMaxX = camX + halfVisW;
        visMaxY = camY + halfVisH;

        shapeRenderer.setProjectionMatrix(getCamera().combined);
        batch.setProjectionMatrix(getCamera().combined);

        roadRenderer.draw(shapeRenderer, speedScroll.getScrollOffset(),
                visMinX, visMinY, visMaxX, visMaxY);

        renderLevelEffects(shapeRenderer, batch);

        batch.begin();
        batch.setColor(1f, 1f, 1f, 1f);
        getEntityManager().render(batch);
        batch.end();

        dashboard.draw();
    }

    @Override
    public void pause() {
        isPaused = true;
        stopMoveLoop();
        Gdx.app.log(getClass().getSimpleName(), "Scene paused");
    }

    @Override
    public void resume() {
        isPaused = false;
        Gdx.app.log(getClass().getSimpleName(), "Scene resumed");
    }

    @Override
    public final void dispose() {
        audioController.dispose();
        disposeLevelData();
        speedScroll.dispose();
        fuelController.dispose();
        eventBus.clear();
        if (commandHistory != null)
            commandHistory.clear();
        dashboard.dispose();
        shapeRenderer.dispose();
        getEntityManager().dispose();
        world.dispose();
        Gdx.app.log(getClass().getSimpleName(), "Scene disposed — all resources cleaned up");
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * Protected accessors for subclasses
     * ══════════════════════════════════════════════════════════════
     */

    protected PhysicsWorld getWorld() {
        return world;
    }

    protected GameCollisionHandler getCollisionHandler() {
        return collisionHandler;
    }

    protected PlayerCar getPlayerCar() {
        return playerCar;
    }

    protected DashboardUI getDashboard() {
        return dashboard;
    }

    protected SoundDevice getSound() {
        return sound;
    }

    protected EventBus getEventBus() {
        return eventBus;
    }

    protected RuleManager getRuleManager() {
        return ruleManager;
    }

    protected io.github.raesleg.engine.io.CommandHistory getCommandHistory() {
        return commandHistory;
    }

    protected RoadRenderer getRoadRenderer() {
        return roadRenderer;
    }

    protected ShapeRenderer getShapeRenderer() {
        return shapeRenderer;
    }

    protected float getScrollOffset() {
        return speedScroll.getScrollOffset();
    }

    protected int getScore() {
        return score;
    }

    protected int getRulesBroken() {
        return rulesBroken;
    }

    protected void setRulesBroken(int n) {
        this.rulesBroken = n;
    }

    protected void setInstantFail(boolean flag) {
        this.instantFail = flag;
    }

    protected void setInstantFail(boolean flag, String reason) {
        this.instantFail = flag;
        this.instantFailReason = reason;
    }

    protected void incrementCrashCount() {
        this.crashCount++;
    }

    protected void addScore(int delta) {
        this.scoreBonus += delta;
        dashboard.showScorePopup(delta);
    }

    protected void handlePickup() {
        addScore(50);
        fuelController.getFuelSystem().recharge();
        sound.playSound("reward", 1.0f);
    }

    protected FuelSystem getFuelSystem() {
        return fuelController.getFuelSystem();
    }

    protected float getGameTime() {
        return gameTime;
    }

    protected float getScrollSpeedPixelsPerSecond() {
        return speedScroll.getScrollSpeedPixelsPerSecond();
    }

    protected float getNpcScrollSpeedPixelsPerSecond() {
        return speedScroll.getNpcScrollSpeedPixelsPerSecond();
    }

    protected float getSimulatedSpeedKmh() {
        return speedScroll.getSimulatedSpeed();
    }

    /**
     * Returns the speed/scroll controller so subclasses can apply speed
     * penalties (e.g. Level 2 crash halving).
     */
    protected SpeedScrollController getSpeedScrollController() {
        return speedScroll;
    }

    protected int getCrashCount() {
        return crashCount;
    }

    /** Left edge of the visible camera area (world coords, updated each frame). */
    protected float getVisMinX() {
        return visMinX;
    }

    /**
     * Bottom edge of the visible camera area (world coords, updated each frame).
     */
    protected float getVisMinY() {
        return visMinY;
    }

    /** Right edge of the visible camera area (world coords, updated each frame). */
    protected float getVisMaxX() {
        return visMaxX;
    }

    /** Top edge of the visible camera area (world coords, updated each frame). */
    protected float getVisMaxY() {
        return visMaxY;
    }

    /**
     * Registers a level-end condition (OCP extension point).
     * Subclasses may call this in {@link #initLevelData()} to add
     * custom conditions without modifying this base class.
     */
    protected void addEndCondition(ILevelEndCondition condition) {
        endConditions.add(condition);
    }

    /**
     * Template Method — iterates registered end conditions every frame.
     * Stops at the first condition that fires.
     */
    protected final void checkLevelEnd() {
        if (gameOverPending)
            return;

        for (ILevelEndCondition condition : endConditions) {
            if (condition.evaluate())
                return;
        }
    }

    /* ── Built-in end conditions (registered in show()) ── */

    protected boolean checkWinCondition() {
        float scrollOffset = speedScroll.getScrollOffset();
        float progress = Math.min(1f, (-scrollOffset) / getLevelLength());
        if (progress < 1.0f)
            return false;

        Gdx.app.log(getClass().getSimpleName(), "Level complete! Score: " + score);
        LevelResult win = new LevelResult(
                score, gameTime, rulesBroken, getLevelName(), true, "", getViolationLog());
        getSceneManager().set(new ResultsScene(win, () -> createRetryScene()));
        return true;
    }

    /**
     * Checks whether the crash count has reached the explosion threshold.
     * <p>
     * NOT registered by default — subclasses that want crash-explosion
     * behaviour must call {@code addEndCondition(this::checkCrashExplosion)}
     * in their {@link #initLevelData()} method.
     */
    protected boolean checkCrashExplosion() {
        if (crashCount < GameConstants.CRASH_EXPLOSION_THRESHOLD)
            return false;

        Gdx.app.log(getClass().getSimpleName(), "3rd crash! Triggering explosion...");
        triggerExplosionGameOver("Crashed into too many vehicles");
        return true;
    }

    protected boolean checkInstantFail() {
        if (!instantFail)
            return false;

        Gdx.app.log(getClass().getSimpleName(), "Instant fail! Reason: " + instantFailReason);
        sound.playSound("negative", 1.0f);
        LevelResult lose = new LevelResult(
                score, gameTime, rulesBroken, getLevelName(), false, instantFailReason, getViolationLog());
        getSceneManager().set(new ResultsScene(lose, () -> createRetryScene()));
        return true;
    }

    protected boolean checkSubclassGameOver() {
        if (!isGameOver())
            return false;

        String reason = getGameOverReason();
        Gdx.app.log(getClass().getSimpleName(), "Game Over! Reason: " + reason);
        sound.playSound("negative", 1.0f);
        LevelResult lose = new LevelResult(
                score, gameTime, rulesBroken, getLevelName(), false, reason, getViolationLog());
        getSceneManager().set(new ResultsScene(lose, () -> createRetryScene()));
        return true;
    }

    /**
     * Triggers an explosion at the player's position and schedules a delayed
     * transition to the results screen. Entity spawning is delegated to
     * {@link ExplosionSystem} (SRP).
     */
    protected void triggerExplosionGameOver(String lossReason) {
        gameOverPending = true;
        gameOverTimer = GameConstants.EXPLOSION_DELAY;
        pendingResult = new LevelResult(
                score, gameTime, rulesBroken, getLevelName(), false, lossReason, getViolationLog());

        float px = playerCar.getX() + playerCar.getW() / 2f;
        float py = playerCar.getY() + playerCar.getH() / 2f;
        Particle.spawnExplosion(getEntityManager(),
                new com.badlogic.gdx.math.Vector2(px / Constants.PPM, py / Constants.PPM), 50f);

        // Large explode.png overlay
        getEntityManager().addEntity(new ExplosionOverlay(
                "explode.png", px - 100f, py - 100f, 200f, 200f, GameConstants.EXPLOSION_DELAY));

        sound.playSound("explosion_big", 0.5f);
        stopMoveLoop();
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * Private helpers
     * ══════════════════════════════════════════════════════════════
     */

    private void stopMoveLoop() {
        sound.stopSound("drive");
    }

    private void updateMoveLoop(boolean moving) {
        if (sound.isMuted() || !moving) {
            stopMoveLoop();
            return;
        }
        if (!sound.isLooping("drive")) {
            sound.loopSound("drive");
        }
    }

    private void openPause() {
        getSceneManager().push(new PauseScene());
    }
}
