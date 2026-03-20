package io.github.raesleg.game.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.collision.CollisionManager;
import io.github.raesleg.engine.entity.EntityManager;
import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.movement.MovementManager;
import io.github.raesleg.engine.movement.UserControlled;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.engine.physics.PhysicsWorld;
import io.github.raesleg.engine.scene.Scene;
import io.github.raesleg.game.collision.GameCollisionHandler;
import io.github.raesleg.game.entities.misc.Trees;
import io.github.raesleg.game.entities.vehicles.PlayerCar;
import io.github.raesleg.game.io.Keyboard;
import io.github.raesleg.game.movement.PlayerMovement;
import io.github.raesleg.game.movement.PlayerMovementModel;
import io.github.raesleg.game.state.DashboardUI;

/**
 * BaseGameScene — Abstract template for all gameplay levels.
 * <p>
 * Uses the <b>Template Method</b> pattern to define a strict contract for
 * level scenes. Common infrastructure (physics, managers, player car, HUD,
 * input, audio) is initialised here. Subclasses customise behaviour through
 * well-defined hooks:
 * <ul>
 * <li>{@link #getLevelLength()}, {@link #getMaxSpeed()},
 * {@link #getAcceleration()}, {@link #getBrakeRate()},
 * {@link #getBgmPath()} — level configuration</li>
 * <li>{@link #initLevelData()} — add level-specific entities, zones,
 * sounds</li>
 * <li>{@link #updateGame(float)} — per-frame level-specific logic</li>
 * <li>{@link #renderLevelEffects(ShapeRenderer, SpriteBatch)} — visual
 * overlays</li>
 * <li>{@link #disposeLevelData()} — clean up level-specific resources</li>
 * </ul>
 * <p>
 * <b>Scene Sovereignty:</b> Each level owns its own EntityManager,
 * MovementManager, CollisionManager, and PhysicsWorld. Only the shared
 * IOManager is injected by SceneManager.
 */
public abstract class BaseGameScene extends Scene {
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

    /** The number of NPC crashes that triggers an explosion game over. */
    private static final int CRASH_EXPLOSION_THRESHOLD = 3;

    /* ── Speed & scroll ── */
    private float simulatedSpeed;
    private float scrollOffset;
    private static final float PASSIVE_DECEL = 18f;

    /* ── Physics ── */
    private PhysicsWorld world;

    /* ── Collision ── */
    private GameCollisionHandler collisionHandler;

    /* ── Player ── */
    // private MovableEntity playerCar;
    private PlayerCar playerCar;

    // trees
    private Trees trees;

    /* ── Rendering ── */
    private ShapeRenderer shapeRenderer;
    private RoadRenderer roadRenderer;

    /* ── Audio ── */
    private SoundDevice sound;
    private Music bgm;
    private static final float BGM_BASE_VOLUME = 0.2f;

    /* ── Explosion game-over delay ── */
    private boolean gameOverPending;
    private float gameOverTimer;
    private LevelResult pendingResult;
    private static final float EXPLOSION_DELAY = 1.5f;

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
        this.scrollOffset = 0f;
        this.simulatedSpeed = 0f;
        this.gameOverPending = false;
        this.gameOverTimer = 0f;
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * Abstract hooks — subclasses MUST implement
     * ══════════════════════════════════════════════════════════════
     */

    /** Total scroll distance (pixels) to reach the finish line. */
    protected abstract float getLevelLength();

    /** Maximum achievable speed in KM/H. */
    protected abstract float getMaxSpeed();

    /** Speed increase rate (KM/H per second) while accelerating. */
    protected abstract float getAcceleration();

    /** Speed decrease rate (KM/H per second) while braking. */
    protected abstract float getBrakeRate();

    /** Asset path for the level's background music (null = no BGM). */
    protected abstract String getBgmPath();

    /**
     * Called after all common managers, player car, and input are ready.
     * Add level-specific entities, motion zones, and sounds here.
     */
    protected abstract void initLevelData();

    /**
     * Called every frame (when not paused) after common update logic.
     * Implement level-specific rules (police chase, traffic spawning, etc.).
     */
    protected abstract void updateGame(float deltaTime);

    /**
     * Template Method hook — returns true when the level's specific
     * lose condition is met. Called by {@link #checkLevelEnd()} every frame.
     * <p>
     * Level 1 returns true when the WANTED threshold is reached.
     * Level 2 returns true when the police car catches the player.
     * A level with no lose condition simply returns false.
     */
    protected abstract boolean isGameOver();

    /** Human-readable name shown on the results screen. */
    protected abstract String getLevelName();

    /**
     * Factory Method hook — creates a fresh instance of
     * this level for the "Retry" action on the results screen.
     */
    protected abstract BaseGameScene createRetryScene();

    /**
     * Human-readable reason for the level-specific lose condition (e.g. "Police
     * caught you").
     */
    protected abstract String getGameOverReason();

    /*
     * ══════════════════════════════════════════════════════════════
     * Optional hooks — subclasses MAY override
     * ══════════════════════════════════════════════════════════════
     */

    /**
     * Called during render between the road and the entity layer.
     * Override to draw level-specific visual effects (rain, fog, etc.).
     */
    protected void renderLevelEffects(ShapeRenderer sr, SpriteBatch batch) {
        // no-op by default
    }

    /**
     * Called at the start of {@link #dispose()} before common resources
     * are cleaned up. Override to dispose level-specific resources.
     */
    protected void disposeLevelData() {
        // no-op by default
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
        roadRenderer = new RoadRenderer(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        dashboard = new DashboardUI(getUiViewport());

        /* Physics world: zero gravity (top-down) */
        world = new PhysicsWorld(new Vector2(0, 0));

        sound = getIOManager().getSound();

        /* Managers (Scene Sovereignty) */
        setEntityManager(new EntityManager());
        setMovementManager(new MovementManager(world, getEntityManager()));
        collisionHandler = new GameCollisionHandler(getEntityManager(), sound);
        setCollisionManager(new CollisionManager(world, collisionHandler));

        /* Road boundary walls (metres) */
        float t = 0.5f;
        float roadLeftM = RoadRenderer.ROAD_LEFT / Constants.PPM;
        float roadRightM = RoadRenderer.ROAD_RIGHT / Constants.PPM;

        world.createBody(BodyDef.BodyType.StaticBody,
                roadLeftM - t / 2f, worldH / 2f, t / 2f, worldH / 2f, 0, 0.4f, false, null);
        world.createBody(BodyDef.BodyType.StaticBody,
                roadRightM + t / 2f, worldH / 2f, t / 2f, worldH / 2f, 0, 0.4f, false, null);
        world.createBody(BodyDef.BodyType.StaticBody,
                worldW / 2f, -t / 2f, worldW / 2f, t / 2f, 0, 0.4f, false, null);
        world.createBody(BodyDef.BodyType.StaticBody,
                worldW / 2f, worldH + t / 2f, worldW / 2f, t / 2f, 0, 0.4f, false, null);

        /* Player car — centre lane, near bottom */
        float carPixelX = RoadRenderer.ROAD_LEFT + RoadRenderer.ROAD_WIDTH / 2f;
        float carPixelY = 100f;
        float carW = 100f; // 64f
        float carH = 185f; // 64f

        PhysicsBody carBody = world.createBody(
                BodyDef.BodyType.DynamicBody,
                carPixelX / Constants.PPM,
                (carPixelY + carH / 2f) / Constants.PPM,
                (carW / Constants.PPM) / 2f,
                (carH / Constants.PPM) / 2f,
                1f, 0.3f, false, null);
        //carBody.setBullet(true);

        /* Input bindings */
        Keyboard kb = getIOManager().getInputs(Keyboard.class);
        UserControlled user = new UserControlled(kb);

        /* Testing Purposes for Movement */
        playerCar = new PlayerCar(
                "car.png",
                carPixelX - carW / 2f, carPixelY,
                carW, carH,
                user, 
                new PlayerMovement(),
                new PlayerMovementModel(),
                carBody);
                
        getEntityManager().addEntity(playerCar);

        /* Input bindings */
        kb.bindAction(Input.Keys.A, Constants.LEFT);
        kb.bindAction(Input.Keys.LEFT, Constants.LEFT);
        kb.bindAction(Input.Keys.D, Constants.RIGHT);
        kb.bindAction(Input.Keys.RIGHT, Constants.RIGHT);

        kb.bindAction(Input.Keys.W, Constants.UP);
        kb.bindAction(Input.Keys.UP, Constants.UP);
        kb.bindAction(Input.Keys.S, Constants.DOWN);
        kb.bindAction(Input.Keys.DOWN, Constants.DOWN);

        // kb.bindAction(Input.Keys.W, "ACCEL");
        // kb.bindAction(Input.Keys.UP, "ACCEL");
        // kb.bindAction(Input.Keys.S, "BRAKE");
        // kb.bindAction(Input.Keys.DOWN, "BRAKE");

        kb.bindAction(Input.Keys.SPACE, Constants.ACTION);
        kb.addBind(Input.Keys.ESCAPE, this::openPause, true);
        kb.addBind(Input.Keys.M, this::toggleMute, true);

        // tree assets being made
        trees = new Trees(8, getEntityManager());

        /* Background music (null path = no BGM) */
        String bgmPath = getBgmPath();
        if (bgmPath != null) {
            bgm = Gdx.audio.newMusic(Gdx.files.internal(bgmPath));
            bgm.setLooping(true);
            syncBgmVolume();
            bgm.play();
        }

        /* Common scene sounds */
        sound.addSound("move", "car_sound.wav");
        sound.addSound("explosion", "collide_sound.wav");
        sound.addSound("explosion_big", "explosionsound.wav");
        sound.addSound("reward", "rewardsound.mp3");
        sound.addSound("negative", "negativesound.mp3");

        /* Level-specific setup (Template Method hook) */
        initLevelData();
    }

    @Override
    public final void update(float deltaTime) {
        if (isPaused) {
            sound.stopSound("move");
            return;
        }

        /* Explosion delay — keep rendering entities but freeze gameplay */
        if (gameOverPending) {
            getEntityManager().update(deltaTime);
            gameOverTimer -= deltaTime;
            if (gameOverTimer <= 0) {
                getSceneManager().set(
                        new ResultsScene(pendingResult, () -> createRetryScene()));
            }
            return;
        }

        gameTime += deltaTime;

        getEntityManager().update(deltaTime);
        getMovementManager().update(deltaTime);

        /* Speed control via action bindings */
        Keyboard kb = getIOManager().getInputs(Keyboard.class);

        if (kb.isHeld(Constants.UP)) {
            simulatedSpeed = Math.min(getMaxSpeed(), simulatedSpeed + getAcceleration() * deltaTime);
        } else if (kb.isHeld(Constants.DOWN)) {
            simulatedSpeed = Math.max(0f, simulatedSpeed - getBrakeRate() * deltaTime);
        } else {
            simulatedSpeed = Math.max(0f, simulatedSpeed - PASSIVE_DECEL * deltaTime);
        }

        float scrollSpeed = getScrollSpeedPixelsPerSecond();
        scrollOffset -= scrollSpeed * deltaTime;

        /* Dashboard updates — score only increases when moving toward goal */
        if (simulatedSpeed > 0.5f) {
            scoreAccumulator += deltaTime * 10f;
        }
        score = (int) scoreAccumulator + scoreBonus;

        // tree update to simulate scenery movement
        trees.update(simulatedSpeed, deltaTime);

        /* Dashboard updates */
        score = (int) (gameTime * 10f);
        float progress = Math.min(1f, (-scrollOffset) / getLevelLength());

        dashboard.onScoreUpdated(score);
        dashboard.onSpeedChanged(Math.round(simulatedSpeed));
        dashboard.onProgressUpdated(progress);
        dashboard.onRuleBroken(rulesBroken);
        dashboard.act(deltaTime);

        updateMoveLoop(playerCar.isMoving());

        /* Sync BGM volume with master volume (may have changed in pause menu) */
        syncBgmVolume();

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
        getCamera().update();
        shapeRenderer.setProjectionMatrix(getCamera().combined);
        batch.setProjectionMatrix(getCamera().combined);

        /* Road background */
        roadRenderer.draw(shapeRenderer, scrollOffset);

        /* Level-specific visual effects (Template Method hook) */
        renderLevelEffects(shapeRenderer, batch);

        /* Entities */
        batch.begin();
        getEntityManager().render(batch);
        batch.end();

        /* HUD overlay */
        dashboard.draw();
    }

    @Override
    public void pause() {
        isPaused = true;
        stopMoveLoop();
        if (bgm != null)
            bgm.pause();
        Gdx.app.log(getClass().getSimpleName(), "Scene paused");
    }

    @Override
    public void resume() {
        isPaused = false;
        if (bgm != null) {
            syncBgmVolume();
            bgm.play();
        }
        Gdx.app.log(getClass().getSimpleName(), "Scene resumed");
    }

    @Override
    public final void dispose() {
        sound.stopAllSounds();
        disposeLevelData();
        dashboard.dispose();
        shapeRenderer.dispose();
        getEntityManager().dispose();
        world.dispose();
        if (bgm != null) {
            bgm.stop();
            bgm.dispose();
        }
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

    // protected MovableEntity getPlayerCar() {
    // return playerCar;
    // }
    protected PlayerCar getPlayerCar() {
        return playerCar;
    }

    protected DashboardUI getDashboard() {
        return dashboard;
    }

    protected SoundDevice getSound() {
        return sound;
    }

    protected RoadRenderer getRoadRenderer() {
        return roadRenderer;
    }

    protected ShapeRenderer getShapeRenderer() {
        return shapeRenderer;
    }

    protected float getScrollOffset() {
        return scrollOffset;
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

    /**
     * Adds (or subtracts) a score delta. Dashboard shows the change
     * incrementally and displays a floating popup.
     */
    protected void addScore(int delta) {
        this.scoreBonus += delta;
        dashboard.showScorePopup(delta);
    }

    protected float getGameTime() {
        return gameTime;
    }

    protected abstract float getMaxScrollPixelsPerSecond();

    protected float getScrollSpeedPixelsPerSecond() {
        float t = simulatedSpeed / getMaxSpeed();
        t = Math.max(0f, Math.min(1f, t));
        return t * getMaxScrollPixelsPerSecond();
    }

    protected float getSimulatedSpeedKmh() {
        return simulatedSpeed;
    }

    /**
     * Template Method — fixed algorithm step that checks both win and lose
     * conditions every frame. Calls the abstract hook {@link #isGameOver()}
     * for level-specific lose logic.
     * <p>
     * The level is <b>won</b> when scroll progress reaches 100%.
     * The level is <b>lost</b> when:
     * <ul>
     * <li>{@code rulesBroken >= GAME_OVER_THRESHOLD} (too many violations)</li>
     * <li>{@code instantFail} is set (e.g. pedestrian hit)</li>
     * <li>{@link #isGameOver()} returns true (level-specific condition)</li>
     * </ul>
     * On trigger, transitions to {@link ResultsScene} with a
     * {@link LevelResult} snapshot.
     */
    protected final void checkLevelEnd() {
        float progress = Math.min(1f, (-scrollOffset) / getLevelLength());

        if (progress >= 1.0f) {
            Gdx.app.log(getClass().getSimpleName(),
                    "Level complete! Score: " + score);
            LevelResult win = new LevelResult(
                    score, gameTime, rulesBroken, getLevelName(), true, "");
            getSceneManager().set(
                    new ResultsScene(win, () -> createRetryScene()));
            return;
        }

        if (crashCount >= CRASH_EXPLOSION_THRESHOLD) {
            Gdx.app.log(getClass().getSimpleName(),
                    "3rd crash! Triggering explosion...");
            triggerExplosionGameOver("Crashed into too many vehicles");
            return;
        }

        if (instantFail) {
            Gdx.app.log(getClass().getSimpleName(),
                    "Instant fail! Reason: " + instantFailReason);
            sound.playSound("negative", 1.0f);
            LevelResult lose = new LevelResult(
                    score, gameTime, rulesBroken, getLevelName(), false, instantFailReason);
            getSceneManager().set(
                    new ResultsScene(lose, () -> createRetryScene()));
            return;
        }

        if (isGameOver()) {
            String reason = getGameOverReason();
            Gdx.app.log(getClass().getSimpleName(),
                    "Game Over! Reason: " + reason);
            sound.playSound("negative", 1.0f);
            LevelResult lose = new LevelResult(
                    score, gameTime, rulesBroken, getLevelName(), false, reason);
            getSceneManager().set(
                    new ResultsScene(lose, () -> createRetryScene()));
        }
    }

    /** Spawns a large explode.png at the player position and delays transition. */
    private void triggerExplosionGameOver(String lossReason) {
        gameOverPending = true;
        gameOverTimer = EXPLOSION_DELAY;
        pendingResult = new LevelResult(
                score, gameTime, rulesBroken, getLevelName(), false, lossReason);

        // Spawn visual explosion at player position
        float px = playerCar.getX() + playerCar.getW() / 2f;
        float py = playerCar.getY() + playerCar.getH() / 2f;
        io.github.raesleg.game.entities.vehicles.npc.world.effects.ExplosionParticle
                .spawnExplosion(getEntityManager(),
                        new com.badlogic.gdx.math.Vector2(px / Constants.PPM, py / Constants.PPM), 50f);

        // Large explode.png overlay
        getEntityManager().addEntity(
                new io.github.raesleg.game.entities.ExplosionOverlay(
                        "explode.png", px - 100f, py - 100f, 200f, 200f, EXPLOSION_DELAY));

        sound.playSound("explosion_big", 1.0f);
        stopMoveLoop();
        if (bgm != null)
            bgm.setVolume(0.05f);
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * Private helpers
     * ══════════════════════════════════════════════════════════════
     */

    private void stopMoveLoop() {
        sound.stopSound("move");
    }

    private void updateMoveLoop(boolean moving) {
        if (sound.isMuted() || !moving) {
            stopMoveLoop();
            return;
        }
        if (!sound.isLooping("move")) {
            sound.loopSound("move");
        }
    }

    private void openPause() {
        getSceneManager().push(new PauseScene());
    }

    private void toggleMute() {
        sound.toggleMute();
        syncBgmVolume();
        updateMoveLoop(playerCar.isMoving());
    }

    /** Keeps the BGM Music object in sync with mute state and master volume. */
    private void syncBgmVolume() {
        if (bgm != null) {
            if (sound.isMuted()) {
                bgm.setVolume(0f);
            } else {
                bgm.setVolume(BGM_BASE_VOLUME * sound.getMasterVolume());
            }
        }
    }
}
