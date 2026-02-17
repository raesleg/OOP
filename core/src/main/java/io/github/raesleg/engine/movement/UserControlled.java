package io.github.raesleg.engine.movement;

import com.badlogic.gdx.Input;

import io.github.raesleg.engine.io.ControlSource;
import io.github.raesleg.engine.io.InputDevice;

public class UserControlled implements ControlSource {
    private InputDevice input;

    public UserControlled(InputDevice input) {
            this.input = input;
    }

    @Override
    public float getX(float deltaTime) {
        return input.getX();
    }

    @Override
    public float getY(float deltaTime) {
        return input.getY();
    }

    @Override
    public boolean isAction(float dt) {
        return input.isKeyPressed(Input.Keys.SPACE)
            || input.isMouseButtonPressed(Input.Buttons.LEFT);
    }

    @Override
    public boolean isConfirm(float deltaTime) {
        // ENTER should be "just pressed" (one-shot)
        return input.isKeyJustPressed(Input.Keys.ENTER)
                || input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER);
    }

    @Override
    public boolean isPause(float deltaTime) {
        // ESC should be "just pressed" (one-shot)
        return input.isKeyJustPressed(Input.Keys.ESCAPE);
    }

    @Override
    public boolean isUpJustPressed(float dt) {
        return input.isKeyJustPressed(Input.Keys.W)
            || input.isKeyJustPressed(Input.Keys.UP);
    }

    @Override
    public boolean isDownJustPressed(float dt) {
        return input.isKeyJustPressed(Input.Keys.S)
            || input.isKeyJustPressed(Input.Keys.DOWN);
    }

    @Override
    public boolean isMuteJustPressed(float dt) {
        return input.isKeyJustPressed(Input.Keys.M);
    }

    // @Override
    // public ControlState get(float dt) {
    //     float x = 0f, y = 0f;
    //     if (io.isLeftHeld())
    //         x -= 1f;
    //     if (io.isRightHeld())
    //         x += 1f;
    //     if (io.isDownHeld())
    //         y -= 1f;
    //     if (io.isUpHeld())
    //         y += 1f;

    //     // normalize so diagonal isn't faster
    //     float len = (float) Math.sqrt(x * x + y * y);
    //     if (len > 0.0001f) {
    //         x /= len;
    //         y /= len;
    //     }

    //     boolean action = io.isActionHeld();
    //     return new ControlState(x, y, action);
    // }
}
