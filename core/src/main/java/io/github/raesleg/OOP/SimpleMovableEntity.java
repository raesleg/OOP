// for testing of movement management
package io.github.raesleg.OOP;

public class SimpleMovableEntity extends Entity implements IMovable {
    private final Controls.ControlSource controls;

    public SimpleMovableEntity(float x, float y, float speed, Controls.ControlSource controls) {
        super(x, y, speed);
        this.controls = controls;
    }

    @Override
    public void move(float dt) {
        Controls.ControlState c = controls.get(dt);
        translate(c.xAxis() * getSpeed() * dt, c.yAxis() * getSpeed() * dt);
    }
}
