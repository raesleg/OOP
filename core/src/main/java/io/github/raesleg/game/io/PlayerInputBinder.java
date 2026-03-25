package io.github.raesleg.game.io;

import com.badlogic.gdx.Input;

import io.github.raesleg.engine.Constants;

/**
 * PlayerInputBinder — Binds keyboard keys to engine-level actions for the
 * player car.
 * <p>
 * Extracted from {@link io.github.raesleg.game.scene.BaseGameScene} to satisfy
 * SRP: the scene should not know individual key mappings.
 */
public final class PlayerInputBinder {

    private PlayerInputBinder() {
    }

    /**
     * Binds the standard WASD + arrow-key movement controls and the SPACE action
     * key.
     *
     * @param kb the keyboard input device to bind actions on
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
}
