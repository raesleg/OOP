package io.github.raesleg.engine.movement;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import java.util.HashSet;
import java.util.Set;

public class IOManager {
    private boolean left_click;
    private boolean right_click;
    private Set<Integer> pressedKeys = new HashSet<>();
    private Set<Integer> keysDown = new HashSet<>();

    public void update() {
        pressedKeys.clear();

        trackKey(Input.Keys.W);
        trackKey(Input.Keys.A);
        trackKey(Input.Keys.S);
        trackKey(Input.Keys.D);
        trackKey(Input.Keys.UP);
        trackKey(Input.Keys.LEFT);
        trackKey(Input.Keys.DOWN);
        trackKey(Input.Keys.RIGHT);
        trackKey(Input.Keys.SPACE);
        trackKey(Input.Keys.ESCAPE);
        trackKey(Input.Keys.ENTER);
        trackKey(Input.Keys.NUMPAD_ENTER);

        // mouse buttons
        left_click = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        right_click = Gdx.input.isButtonPressed(Input.Buttons.RIGHT);
    }

    private void trackKey(int key) {
        boolean pressedNow = Gdx.input.isKeyPressed(key);

        if (pressedNow) {
            if (!keysDown.contains(key)) {
                pressedKeys.add(key); 
            }
            keysDown.add(key);
        } else {
            keysDown.remove(key);
        }
    }

    public boolean isKeyDown(int key) { return keysDown.contains(key);}
    
    public boolean isKeyJustPressed(int key) { return pressedKeys.contains(key);};

    public boolean isLeftClick() {return left_click;};

    public boolean isRightClick() {return right_click;};
}
