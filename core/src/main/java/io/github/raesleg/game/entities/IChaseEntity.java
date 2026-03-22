package io.github.raesleg.game.entities;

/**
 * Abstraction for any entity that chases the player.
 * Level2Scene depends on this interface rather than a concrete class (DIP).
 */
public interface IChaseEntity {

    void updateChase(float deltaTime, float playerX, float playerY,
            float playerSpeed, float maxSpeed, float aggression);

    boolean hasCaughtPlayer();

    float getScreenY();
}
