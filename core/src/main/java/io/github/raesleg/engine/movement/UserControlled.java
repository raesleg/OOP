package io.github.raesleg.engine.movement;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.io.ActionInput;
import io.github.raesleg.engine.io.ControlSource;

public class UserControlled implements ControlSource {
    private ActionInput input;

    public UserControlled(ActionInput input) {
        this.input = input;
    }

    @Override
    public boolean isUserControlled() {
        return true;
    }

    @Override
    public float getX(float deltaTime) {
        float x = 0f;
        if (input.isHeld(Constants.LEFT))  x -= 1f;
        if (input.isHeld(Constants.RIGHT)) x += 1f;
        return x;
    }

    @Override
    public float getY(float deltaTime) {
        float y = 0f;
        if (input.isHeld(Constants.UP))   y += 1f;
        if (input.isHeld(Constants.DOWN)) y -= 1f;
        return y;
    }

    @Override
    public boolean isAction(float deltaTime) {
        return input.justPressed(Constants.ACTION);
    }
}