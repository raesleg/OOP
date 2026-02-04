package io.github.raesleg.OOP;

import com.badlogic.gdx.Input;

public final class Controls {
    private Controls() {}

    public record ControlState(float xAxis, float yAxis, boolean action) {
    }

    public interface ControlSource {
        ControlState get(float dt);
    }

    public static final class UserControlled implements ControlSource {
        private final IOManager io;

        public UserControlled(IOManager io) {
            this.io = io;
        }
        
        @Override
        public ControlState get(float dt) {
            float x = 0f, y = 0f;
            if (io.isKeyDown(Input.Keys.A) || io.isKeyDown(Input.Keys.LEFT)) x -= 1f;
            if (io.isKeyDown(Input.Keys.D) || io.isKeyDown(Input.Keys.RIGHT)) x += 1f;
            if (io.isKeyDown(Input.Keys.S) || io.isKeyDown(Input.Keys.DOWN)) y -= 1f;
            if (io.isKeyDown(Input.Keys.W) || io.isKeyDown(Input.Keys.UP)) y += 1f;

            // normalize so diagonal isn't faster
            float len = (float)Math.sqrt(x * x + y * y);
            if (len > 0.0001f) { x /= len; y /= len; }

            boolean action = io.isKeyDown(Input.Keys.SPACE);
            return new ControlState(x, y, action);
        }
    }

    public static final class AIControlled implements ControlSource {
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
