package io.github.raesleg.game.scene;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.game.GameConstants;
import io.github.raesleg.game.entities.vehicles.PlayerCar;
import io.github.raesleg.game.io.Keyboard;

/**
 * PlayerController — SRP extraction for Level 2 vertical player movement.
 * <p>
 * Owns the per-frame UP/DOWN key → Y position update and physics body
 * synchronisation that was previously inlined inside
 * {@link Level2Scene#updateGame(float)}.
 * <p>
 * <b>SRP:</b> Sole responsibility is translating input into bounded
 * vertical position changes for the player car.
 */
public final class PlayerController {

    private final Keyboard keyboard;
    private final PlayerCar playerCar;

    public PlayerController(Keyboard keyboard, PlayerCar playerCar) {
        this.keyboard = keyboard;
        this.playerCar = playerCar;
    }

    /**
     * Reads UP/DOWN input, clamps Y within road bounds, and syncs the
     * physics body to the new visual position.
     */
    public void update(float deltaTime) {
        float currentY = playerCar.getY();

        if (keyboard.isHeld(Constants.UP)) {
            currentY += GameConstants.L2_PLAYER_VERTICAL_SPEED * deltaTime;
        }
        if (keyboard.isHeld(Constants.DOWN)) {
            currentY -= GameConstants.L2_PLAYER_VERTICAL_SPEED * deltaTime;
        }

        currentY = Math.max(GameConstants.PLAYER_MIN_Y,
                Math.min(GameConstants.PLAYER_MAX_Y, currentY));
        playerCar.setY(currentY);

        /* Sync physics body to match visual position */
        var body = playerCar.getPhysicsBody();
        if (body != null) {
            body.setPosition(body.getPosition().x,
                    (currentY + playerCar.getH() / 2f) / Constants.PPM);
        }
    }
}
