package io.github.raesleg.engine.io;

import java.util.ArrayList;
import java.util.List;

public class IOManager {

    private final List<InputDevice> inputs = new ArrayList<>();
    private final SoundDevice output;

    public IOManager(SoundDevice output) {
        this.output = output;
    }

    public void addInput(InputDevice device) {
        inputs.add(device);
    }

    public <T extends InputDevice> T getInputs(Class<T> type) {
        for (InputDevice d : inputs) {
            if (type.isInstance(d)) return type.cast(d);
        }
        return null;
    }

    public SoundDevice getSound() {
        return output;
    }

    public void update() {
        for (InputDevice d : inputs) {
            d.handleInput();
        }
    }

    public void dispose() {
        output.dispose();
    }

    public void pushInputContext() {
        for (InputDevice d : inputs) d.pushContext();
    }

    public void popInputContext() {
        for (InputDevice d : inputs) d.popContext();
    }

    public void resetInputContexts() {
        for (InputDevice d : inputs) d.resetToBase();
    }
}