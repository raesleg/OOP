package io.github.raesleg.engine.io;

public interface InputDevice {
    void handleInput();

    // bind to an action
    void addBind(int input, Runnable action, boolean isJustPressed);
    void removeBind(int input);
    
    void pushContext();

    void popContext();

    void resetToBase();
}
