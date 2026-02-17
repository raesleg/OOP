package io.github.raesleg.engine.io;

public interface ControlSource {
    float getX(float deltaTime);
    float getY(float deltaTime);
    boolean isAction(float deltaTime);
}