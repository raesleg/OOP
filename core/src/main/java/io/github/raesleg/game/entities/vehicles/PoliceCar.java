package io.github.raesleg.game.entities.vehicles;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.TextureObject;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.game.GameConstants;
import io.github.raesleg.game.entities.IChaseEntity;
import io.github.raesleg.game.movement.PoliceMovement;

/**
 * PoliceCar — A chase entity that spawns when the player accumulates
 * too many rule violations (WANTED stars).
 * 
 * Chase algorithm is delegated to PoliceMovement
 * Implements IChaseEntity so Level2Scene depends on the
 * abstraction rather than this concrete class (DIP).
 * 
 * <b>Design Pattern:</b> Flyweight (TextureObject texture cache),
 * Strategy (chase behaviour delegated to PoliceMovement).
 */
public class PoliceCar extends TextureObject implements IChaseEntity {

    /* ── Siren flash animation ── */
    private static final String[] FLASH_FRAMES = {
            "policecar_noflash.png",
            "policecar_leftflash.png",
            "policecar_noflash.png",
            "policecar_rightflash.png"
    };
    private float flashTimer;
    private int flashIndex;

    private final PhysicsBody body;
    private final PoliceMovement movement;
    private float screenY;
    private boolean caught;

    /**
     * Creates a police car that will chase from below the screen.
     *
     * @param body kinematic PhysicsBody for collision detection
     * @param initialX initial X position in pixels
     * @param initialY initial Y position in pixels
     */
    public PoliceCar(PhysicsBody body, float initialX, float initialY) {
        super("policecar_noflash.png", initialX, initialY, 80f, 140f);
        this.body = body;
        this.screenY = initialY;
        this.caught = false;
        this.flashTimer = 0f;
        this.flashIndex = 0;
        this.movement = new PoliceMovement(screenY);

        if (body != null) {
            body.setUserData(this);
        }
    }

    @Override
    public void updateChase(float deltaTime, float playerX, float playerY,
            int starCount, int maxStars, float playerSpeed, float maxSpeed) {
        // Delegate chase algorithm to PoliceMovement
        screenY = movement.advance(deltaTime, playerY, starCount, maxStars,
                playerSpeed, maxSpeed);

        float newX = movement.lerpX(getX(), playerX, deltaTime);
        setX(newX);
        setY(screenY);

        // Sync physics body for collision detection
        if (body != null) {
            float bodyX = (getX() + getW() / 2f) / Constants.PPM;
            float bodyY = (screenY + getH() / 2f) / Constants.PPM;
            body.setPosition(bodyX, bodyY);
            // body.setPosition(
            //         (newX + getW() / 2f) / Constants.PPM,
            //         (screenY + getH() / 2f) / Constants.PPM);
        }

        caught = movement.hasCaught(screenY, getH(), playerY);

        flashTimer += deltaTime;
        if (flashTimer >= GameConstants.POLICE_FLASH_INTERVAL) {
            flashTimer -= GameConstants.POLICE_FLASH_INTERVAL;
            flashIndex = (flashIndex + 1) % FLASH_FRAMES.length;
        }
    }

    @Override
    public boolean hasCaughtPlayer() {
        return caught;
    }

    @Override
    public float getScreenY() {
        return screenY;
    }

    @Override
    public void draw(SpriteBatch batch) {
        Texture frame = TextureObject.getOrLoadTexture(FLASH_FRAMES[flashIndex]);
        if (frame != null) {
            batch.draw(frame, getX(), getY(), getW(), getH());
        }
    }

    @Override
    public void dispose() {
        if (body != null) body.destroy();
    }
}