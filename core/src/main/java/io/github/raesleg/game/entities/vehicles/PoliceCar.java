package io.github.raesleg.game.entities.vehicles;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.TextureObject;
import io.github.raesleg.engine.physics.PhysicsBody;

/**
 * PoliceCar — A chase entity that spawns when the player accumulates
 * too many rule violations (WANTED stars).
 * <p>
 * The police car approaches from behind the player and gradually closes
 * the gap. The approach speed increases with the player's violation count
 * (via aggression factor). The player can maintain distance by driving at
 * high speed.
 * <p>
 * Game Over occurs when the police car reaches the player's Y position.
 * <p>
 * <b>Design Pattern:</b> Flyweight (TextureObject texture cache),
 * Strategy (chase behaviour is data-driven via aggression parameter).
 * <p>
 * <b>Engine/Game Boundary:</b> Extends engine's TextureObject.
 * Lives entirely in the game layer.
 */
public class PoliceCar extends TextureObject {

    private static final float BASE_APPROACH_SPEED = 70f;
    private static final float AGGRESSION_BONUS = 130f;

    /* ── Siren flash animation ── */
    private static final float FLASH_INTERVAL = 0.15f;
    private static final String[] FLASH_FRAMES = {
            "policecar_noflash.png",
            "policecar_leftflash.png",
            "policecar_noflash.png",
            "policecar_rightflash.png"
    };
    private static Texture[] flashTextures;
    private float flashTimer;
    private int flashIndex;

    private final PhysicsBody body;
    private float screenY;
    private boolean caught;

    /**
     * Creates a police car that will chase from below the screen.
     *
     * @param body kinematic PhysicsBody for collision detection
     */
    public PoliceCar(PhysicsBody body) {
        super("policecar_noflash.png", 0, 0, 80f, 140f);
        this.body = body;
        this.screenY = -50f;
        this.caught = false;
        this.flashTimer = 0f;
        this.flashIndex = 0;

        // Load flash frame textures once (shared across all instances)
        if (flashTextures == null) {
            flashTextures = new Texture[FLASH_FRAMES.length];
            for (int i = 0; i < FLASH_FRAMES.length; i++) {
                flashTextures[i] = new Texture(FLASH_FRAMES[i]);
            }
        }

        if (body != null) {
            body.setUserData(this);
        }
    }

    /** Lerp factor for horizontal tracking (0 = instant, lower = smoother). */
    private static final float LANE_TRACK_SPEED = 4f;

    /**
     * Advances the police car toward the player every frame.
     *
     * @param deltaTime   frame delta
     * @param playerX     player car's screen X position (left edge)
     * @param playerY     player car's screen Y position
     * @param playerSpeed current player speed in KM/H
     * @param maxSpeed    maximum speed for this level
     * @param aggression  0..1 aggression factor from RuleManager
     */
    public void updateChase(float deltaTime, float playerX, float playerY,
            float playerSpeed, float maxSpeed,
            float aggression) {
        // Approach speed = base + aggression bonus
        float approachSpeed = BASE_APPROACH_SPEED + aggression * AGGRESSION_BONUS;

        // Player maintaining high speed slows the police approach
        // At max speed (speedRatio=1.0), speedFactor goes negative → police falls
        // behind
        float speedRatio = (maxSpeed > 0) ? playerSpeed / maxSpeed : 0f;
        float speedFactor = 1.0f - speedRatio * 1.15f;
        speedFactor = Math.max(-0.3f, speedFactor);

        screenY += approachSpeed * speedFactor * deltaTime;

        // Smoothly follow player's X position (lerp toward player lane)
        float targetX = playerX;
        float currentX = getX();
        float newX = currentX + (targetX - currentX) * LANE_TRACK_SPEED * deltaTime;
        setX(newX);
        setY(screenY);

        // Sync kinematic body
        if (body != null) {
            float bodyX = (getX() + getW() / 2f) / Constants.PPM;
            float bodyY = (screenY + getH() / 2f) / Constants.PPM;
            body.setPosition(bodyX, bodyY);
        }

        // Live check — recomputed each frame so knockback recovery
        // clears a false-positive "caught" within the same second.
        caught = (screenY + getH() >= playerY);

        // Advance siren flash animation
        flashTimer += deltaTime;
        if (flashTimer >= FLASH_INTERVAL) {
            flashTimer -= FLASH_INTERVAL;
            flashIndex = (flashIndex + 1) % FLASH_FRAMES.length;
        }
    }

    /** True while the police car overlaps the player vertically. */
    public boolean hasCaughtPlayer() {
        return caught;
    }

    /** Current screen-Y position (pixels). Used for distance-based siren volume. */
    public float getScreenY() {
        return screenY;
    }

    @Override
    public void draw(SpriteBatch batch) {
        Texture frame = (flashTextures != null) ? flashTextures[flashIndex] : getTexture();
        if (frame != null) {
            batch.draw(frame, getX(), getY(), getW(), getH());
        }
    }

    @Override
    public void dispose() {
        if (body != null) {
            body.destroy();
        }
    }
}
