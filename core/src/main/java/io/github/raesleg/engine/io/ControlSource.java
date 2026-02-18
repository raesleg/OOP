package io.github.raesleg.engine.io;

public interface ControlSource {
    float getX(float deltaTime);

    float getY(float deltaTime);

    boolean isAction(float deltaTime);

    /**
     * Returns whether this control source represents a human player.
     * AI controllers override this to return false.
     */
    default boolean isPlayerControlled() {
        return true;
    }
}