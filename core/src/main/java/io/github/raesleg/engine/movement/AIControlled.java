package io.github.raesleg.engine.movement;

import io.github.raesleg.engine.io.ControlSource;

public class AIControlled implements ControlSource {
    private float time = 0f;

    public AIControlled() {
    }

    @Override
    public boolean isUserControlled() {
        return false;
    }

    @Override
    public float getX(float deltaTime) {
        time += deltaTime;
        return (float) Math.sin(time * 1.0f); // left-right wave
    }

    @Override
    public float getY(float deltaTime) {
        return (float) Math.cos(time * 0.8f); // constant forward
    }

    @Override
    public boolean isAction(float deltaTime) {
        return ((int) (time * 2)) % 2 == 0;
    }
}
