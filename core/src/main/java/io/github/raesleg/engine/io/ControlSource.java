package io.github.raesleg.engine.io;

public interface ControlSource {
    float getX(float deltaTime);
    float getY(float deltaTime);
    boolean isAction(float deltaTime);

    default boolean isConfirm(float deltaTime) { return false; };
    default boolean isPause(float deltaTime) { return false; };
    default boolean isUp(float deltaTime) { return false; };
    default boolean isDown(float deltaTime) { return false; }; 
    default boolean isMute(float deltaTime) { return false; };
}