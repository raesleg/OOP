package io.github.raesleg.engine.io;

import java.util.HashSet;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

public class KeyboardMouse implements InputDevice {

    // Track only the keys/buttons your game cares about (device-specific, OK)
    private static final int[] TRACK_KEYS = {
            Input.Keys.W, Input.Keys.A, Input.Keys.S, Input.Keys.D,
            Input.Keys.UP, Input.Keys.DOWN, Input.Keys.LEFT, Input.Keys.RIGHT,
            Input.Keys.SPACE,
            Input.Keys.ENTER, Input.Keys.NUMPAD_ENTER,
            Input.Keys.ESCAPE, Input.Keys.M
    };

    private static final int[] TRACK_MOUSE = {
            Input.Buttons.LEFT,
            Input.Buttons.RIGHT
    };

    private final Set<Integer> pressedKeys = new HashSet<>();
    private final Set<Integer> keysDown = new HashSet<>();

    private final Set<Integer> mouseDown = new HashSet<>();
    private final Set<Integer> pressedMouse = new HashSet<>();

    private float scroll = 0f;

    @Override
    public void update() {
        pressedKeys.clear();
        pressedMouse.clear();
        scroll = 0f;    

        // ----- Keys -----
        for (int key : TRACK_KEYS) {
            boolean downNow = Gdx.input.isKeyPressed(key);
            boolean wasDown = keysDown.contains(key);

            if (downNow) {
                if (!wasDown) {
                    pressedKeys.add(key); // just pressed this frame
                }
                keysDown.add(key);
            } else {
                keysDown.remove(key);
            }
        }

        // ----- Mouse buttons -----
        for (int btn : TRACK_MOUSE) {
            boolean downNow = Gdx.input.isButtonPressed(btn);
            boolean wasDown = mouseDown.contains(btn);

            if (downNow) {
                if (!wasDown) {
                    pressedMouse.add(btn);
                }
                mouseDown.add(btn);
            } else {
                mouseDown.remove(btn);
            }
        }
    }

    @Override
    public float getX() {
        float x = 0f;
        if (isKeyPressed(Input.Keys.A) || isKeyPressed(Input.Keys.LEFT)) x -= 1f;
        if (isKeyPressed(Input.Keys.D) || isKeyPressed(Input.Keys.RIGHT)) x += 1f;
        return x;
    }

    @Override
    public float getY() {
        float y = 0f;
        if (isKeyPressed(Input.Keys.S) || isKeyPressed(Input.Keys.DOWN)) y -= 1f;
        if (isKeyPressed(Input.Keys.W) || isKeyPressed(Input.Keys.UP)) y += 1f;
        return y;
    }

    @Override
    public boolean isAction() {
        // "Action" = space OR mouse click (you can change this rule anytime)
        return isKeyPressed(Input.Keys.SPACE)
                || isMouseButtonPressed(Input.Buttons.LEFT)
                || isMouseButtonPressed(Input.Buttons.RIGHT);
    }

    @Override
    public float getScroll() {
        return scroll;
    }

    @Override
    public boolean isKeyPressed(int key) {
        return keysDown.contains(key);
    }

    @Override
    public boolean isKeyJustPressed(int key) {
        return pressedKeys.contains(key);
    }

    @Override
    public boolean isMouseButtonPressed(int btn) {
        return mouseDown.contains(btn);
    }

    @Override
    public boolean isMouseButtonJustPressed(int btn) {
        return pressedMouse.contains(btn);
    }
}
