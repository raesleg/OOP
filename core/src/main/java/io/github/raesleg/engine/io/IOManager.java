package io.github.raesleg.engine.io;

public class IOManager {
    private InputDevice input;
    private SoundDevice output;

    public IOManager(InputDevice input, SoundDevice output) {
        this.input = input;
        this.output = output;
    }

    public InputDevice getInput() {
        return input;
    }

    public SoundDevice getSound() {
        return output;
    }

    public void update() {
        input.update();
    }

    public void dispose() {
        output.dispose();
        // input.dispose(); //maybe if input has dispose
    }
}
