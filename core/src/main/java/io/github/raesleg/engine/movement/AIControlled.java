package io.github.raesleg.engine.movement;

import io.github.raesleg.engine.io.ControlSource;

public class AIControlled implements ControlSource {
    private float time = 0f;

    public AIControlled() {}

    @Override
    public float getX(float deltaTime) {
        time += deltaTime;

        // Example AI: left-right wave
        return (float) Math.sin(time * 1.0f);
    }

    @Override
    public float getY(float deltaTime) {
        // Example AI: constant forward / or up-down wave
        return (float) Math.cos(time * 0.8f);
    }

    @Override
    public boolean isAction(float deltaTime) {
        // Example AI: action every ~1 second pulse
        return ((int)(time * 2)) % 2 == 0;
    }

    // @Override
    // public ControlState get(float dt) {
    //     t += dt;

    //     float x = 0f;
    //     float y = (float) Math.sin(t);

    //     return new ControlState(x, y, false);
    // }
}

