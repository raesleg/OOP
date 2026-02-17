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
        return input.isKeyJustPressed(Input.Keys.ENTER)
                || input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER);
    }

    @Override
    public boolean isPause(float deltaTime) {
        return input.isKeyJustPressed(Input.Keys.ESCAPE);
    }

    @Override
    public boolean isUp(float deltaTime) {
        return input.isKeyJustPressed(Input.Keys.W)
            || input.isKeyJustPressed(Input.Keys.UP);
    }

    @Override
    public boolean isDown(float deltaTime) {
        return input.isKeyJustPressed(Input.Keys.S)
            || input.isKeyJustPressed(Input.Keys.DOWN);
    }

    @Override
    public boolean isMute(float deltaTime) {
        return input.isKeyJustPressed(Input.Keys.M);
    }
}
