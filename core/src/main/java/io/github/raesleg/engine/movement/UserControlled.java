package io.github.raesleg.engine.movement;

public  class UserControlled implements IControllable {
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
