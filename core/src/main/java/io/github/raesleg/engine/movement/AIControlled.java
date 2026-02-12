package io.github.raesleg.engine.movement;

public class AIControlled implements IControllable {
    private float t = 0f;

    @Override
    public ControlState get(float dt) {
        t += dt;

        float x = 0f;
        float y = (float) Math.sin(t);

        return new ControlState(x, y, false);
    }
}

