package io.github.raesleg.engine.movement;

import com.badlogic.gdx.Input;

public class ControlState {

    private float xAxis;
    private float yAxis;
    private boolean action;

    public ControlState(float xAxis, float yAxis, boolean action) {
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.action = action;
    }

    public float getxAxis() { return xAxis; }
    public float getyAxis() { return yAxis; }
    public boolean getAction() { return action; }

    public interface ControlSource {
        ControlState get(float dt);
    }

    public static class UserControlled implements ControlSource {
        private IOManager io;

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

    public static class AIControlled implements ControlSource {
        private float t = 0f;

        @Override
        public ControlState get(float dt) {
            t += dt;

            float x = 0f; 
            float y = (float) Math.sin(t); 

            return new ControlState(x, y, false);
        }
    }
}
