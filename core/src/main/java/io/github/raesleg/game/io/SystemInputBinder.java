package io.github.raesleg.game.io;

import com.badlogic.gdx.Input;

import io.github.raesleg.engine.Constants;

/**
 * SystemInputBinder — Binds system-level and movement key shortcuts
 * without hard-coding them inside scene classes.
 * <p>
 * <b>OCP:</b> New bindings can be added here without modifying any scene.
 * <b>SRP:</b> Sole responsibility is mapping keys to actions.
 */
public final class SystemInputBinder {

    private SystemInputBinder() {
    }

    /**
     * Registers movement key bindings (WASD + arrows + space → engine constants).
     */
    public static void bindMovementKeys(Keyboard kb) {
        kb.bindAction(Input.Keys.A, Constants.LEFT);
        kb.bindAction(Input.Keys.LEFT, Constants.LEFT);
        kb.bindAction(Input.Keys.D, Constants.RIGHT);
        kb.bindAction(Input.Keys.RIGHT, Constants.RIGHT);
        kb.bindAction(Input.Keys.W, Constants.UP);
        kb.bindAction(Input.Keys.UP, Constants.UP);
        kb.bindAction(Input.Keys.S, Constants.DOWN);
        kb.bindAction(Input.Keys.DOWN, Constants.DOWN);
        kb.bindAction(Input.Keys.SPACE, Constants.ACTION);
    }

    /**
     * Registers system-wide key bindings (pause, mute).
     */
    public static void bindSystemKeys(Keyboard kb, Runnable onPause, Runnable onToggleMute) {
        kb.addBind(Input.Keys.ESCAPE, onPause, true);
        kb.addBind(Input.Keys.M, onToggleMute, true);
    }
}
