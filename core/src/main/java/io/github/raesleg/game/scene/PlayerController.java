package io.github.raesleg.game.scene;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.game.GameConstants;
import io.github.raesleg.game.entities.vehicles.PlayerCar;
import io.github.raesleg.game.io.Keyboard;

/**
 * PlayerController — Handles vertical player movement input,
 * Y-coordinate clamping, and physics body synchronisation.
 * <p>
 * Extracted from Level2Scene to satisfy SRP: the scene no longer
 * owns input-to-position logic.
 */
public final class PlayerController {

    private final Keyboard keyboard;

    public PlayerController(Keyboard keyboard) {
        this.keyboard = keyboard;
    }

    /**
     * Polls UP/DOWN keys, moves the player car vertically within bounds,
     * and syncs the physics body to match the new visual position.
     */
    public void update(float deltaTime, PlayerCar player) {
        float currentY = player.getY();

        if (keyboard.isHeld(Constants.UP)) {
            currentY += GameConstants.L2_PLAYER_VERTICAL_SPEED * deltaTime;
        }
        if (keyboard.isHeld(Constants.DOWN)) {
            currentY -= GameConstants.L2_PLAYER_VERTICAL_SPEED * deltaTime;
        }

        currentY = Math.max(GameConstants.PLAYER_MIN_Y,
                Math.min(GameConstants.PLAYER_MAX_Y, currentY));
        player.setY(currentY);

        var playerBody = player.getPhysicsBody();
        if (playerBody != null) {
            playerBody.setPosition(playerBody.getPosition().x,
                    (currentY + player.getH() / 2f) / Constants.PPM);
        }
    }
}
