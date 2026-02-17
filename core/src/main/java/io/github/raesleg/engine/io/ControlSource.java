package io.github.raesleg.engine.io;

public interface ControlSource {
    float getX(float deltaTime);
    float getY(float deltaTime);
    boolean isAction(float deltaTime);

    default boolean isConfirm(float deltaTime) { return false; };
    default boolean isPause(float deltaTime) { return false; };
    default boolean isUpJustPressed(float deltaTime) { return false; };
    default boolean isDownJustPressed(float deltaTime) { return false; }; 
    default boolean isMuteJustPressed(float deltaTime) { return false; };
}