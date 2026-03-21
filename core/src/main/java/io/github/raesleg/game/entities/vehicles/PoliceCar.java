package io.github.raesleg.game.entities.vehicles;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.TextureObject;
import io.github.raesleg.engine.physics.PhysicsBody;

import io.github.raesleg.game.movement.PoliceMovement;

public class PoliceCar extends TextureObject {

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

    private final PoliceMovement movement = new PoliceMovement();

    public PoliceCar(PhysicsBody body, float startY) {
        super("policecar_noflash.png", 0, startY, 80f, 140f);
        this.body   = body;
        this.screenY = startY;
        this.caught = false;

        if (body != null) body.setUserData(this);

        if (flashTextures == null) {
            flashTextures = new Texture[FLASH_FRAMES.length];
            for (int i = 0; i < FLASH_FRAMES.length; i++) {
                flashTextures[i] = new Texture(FLASH_FRAMES[i]);
            }
        }
    }

    /**
     * Called every frame by Level2Scene.
     * Delegates movement to PoliceMovement, then syncs sprite.
     */
    public void updateChase(float deltaTime, float playerX, float playerY,
            float playerSpeed, float maxSpeed,
            float rulesBroken, float scrollSpeedPixelsPerSecond) {

        screenY = movement.update(
                deltaTime, playerX, screenY,
                rulesBroken, scrollSpeedPixelsPerSecond);

        float newX = getX() + (playerX - getX()) * 3.5f * deltaTime;
        setX(newX);
        setY(screenY);

        // Sync physics body for collision detection
        if (body != null) {
            body.setPosition(
                    (newX + getW() / 2f) / Constants.PPM,
                    (screenY + getH() / 2f) / Constants.PPM);
        }

        caught = (screenY + getH() >= playerY);

        flashTimer += deltaTime;
        if (flashTimer >= FLASH_INTERVAL) {
            flashTimer -= FLASH_INTERVAL;
            flashIndex = (flashIndex + 1) % FLASH_FRAMES.length;
        }
    }

    public boolean hasCaughtPlayer() { return caught; }
    public float getScreenY()        { return screenY; }

    @Override
    public void draw(SpriteBatch batch) {
        Texture frame = (flashTextures != null) ? flashTextures[flashIndex] : getTexture();
        if (frame != null) batch.draw(frame, getX(), getY(), getW(), getH());
    }

    @Override
    public void dispose() {
        if (body != null) body.destroy();
    }
}