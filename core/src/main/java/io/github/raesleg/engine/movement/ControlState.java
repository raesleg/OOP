package io.github.raesleg.engine.movement;

public class ControlState {

    private float xAxis;
    private float yAxis;
    private boolean action;

    public ControlState(float xAxis, float yAxis, boolean action) {
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.action = action;
    }

    public float getxAxis() {
        return xAxis;
    }

    public float getyAxis() {
        return yAxis;
    }

    public boolean getAction() {
        return action;
    }

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
            if (io.isLeftHeld())
                x -= 1f;
            if (io.isRightHeld())
                x += 1f;
            if (io.isDownHeld())
                y -= 1f;
            if (io.isUpHeld())
                y += 1f;

            // normalize so diagonal isn't faster
            float len = (float) Math.sqrt(x * x + y * y);
            if (len > 0.0001f) {
                x /= len;
                y /= len;
            }

            boolean action = io.isActionHeld();
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
