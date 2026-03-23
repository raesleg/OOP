package io.github.raesleg.game.entities;

/**
 * Abstraction for any entity that chases the player.
 * Level2Scene depends on this interface rather than a concrete class (DIP).
 */
public interface IChaseEntity {

    void updateChase(float deltaTime, float playerX, float playerY,
            int starCount, int maxStars, float playerSpeed, float maxSpeed);

    boolean hasCaughtPlayer();

    float getScreenY();
}
