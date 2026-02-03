package io.github.raesleg.OOP;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

public final class Controls {
    private Controls() {}

    public record ControlState(float xAxis, float yAxis, boolean action) {
    }

    public interface ControlSource {
        ControlState get(float dt);
    }

    public static final class userControlled implements ControlSource {
        @Override
        public ControlState get(float dt) {
            float x = 0f, y = 0f;
            if (Gdx.input.isKeyPressed(Input.Keys.A)) x -= 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.D)) x += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.S)) y -= 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.W)) y += 1f;

            // normalize so diagonal isn't faster
            float len = (float)Math.sqrt(x * x + y * y);
            if (len > 0.0001f) { x /= len; y /= len; }

            boolean action = Gdx.input.isKeyPressed(Input.Keys.SPACE);
            return new ControlState(x, y, action);
        }
    }

    public static final class AiControlled implements ControlSource {
        private float t = 0f;

        @Override
        public ControlState get(float dt) {
            t += dt;
            float x = (float)Math.cos(t * 0.9f);
            float y = (float)Math.sin(t * 0.7f);
            return new ControlState(x, y, false);
        }
    }
}
