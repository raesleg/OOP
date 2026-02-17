package io.github.raesleg.engine.io;

public interface InputDevice {
    void update();
    float getX();
    float getY();
    boolean isAction();
    float getScroll();

    boolean isKeyPressed(int key);
    boolean isKeyJustPressed(int key);

    boolean isMouseButtonPressed(int btn);
    boolean isMouseButtonJustPressed(int btn);
}
