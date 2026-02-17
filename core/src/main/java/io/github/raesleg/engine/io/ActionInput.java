package io.github.raesleg.engine.io;

public interface ActionInput {
    boolean isHeld(String action);
    boolean justPressed(String action);
}
