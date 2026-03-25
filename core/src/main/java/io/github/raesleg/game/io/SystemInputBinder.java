package io.github.raesleg.game.io;

import com.badlogic.gdx.Input;

/**
 * SystemInputBinder — Binds system-level key shortcuts (pause, mute)
 * to the keyboard without hard-coding them inside scene classes.
 * <p>
 * Follows the same pattern as {@link PlayerInputBinder} (OCP):
 * new system bindings can be added here without modifying any scene.
 * <p>
 * <b>SRP:</b> Sole responsibility is mapping system keys to actions.
 */
public final class SystemInputBinder {

    private SystemInputBinder() {
    }

    /**
     * Registers system-wide key bindings on the given keyboard.
     *
     * @param kb           the keyboard to bind keys on
     * @param onPause      action invoked when ESCAPE is pressed
     * @param onToggleMute action invoked when M is pressed
     */
    public static void bindSystemKeys(Keyboard kb, Runnable onPause, Runnable onToggleMute) {
        kb.addBind(Input.Keys.ESCAPE, onPause, true);
        kb.addBind(Input.Keys.M, onToggleMute, true);
    }
}
