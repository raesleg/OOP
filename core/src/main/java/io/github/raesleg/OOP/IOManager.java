package io.github.raesleg.OOP;

import java.util.Set;

public class IOManager {

    /* Private Variables */
    private boolean left_click;
    private boolean right_click;
    private Set<Integer> pressedKeys;

    /* Public Functions */
    public void keyPressed(int key) {};
    
    public void keyReleased(int key) {};

    public void mousePressed(int btn) {};

    public void mouseReleased(int btn) {};

    //public boolean isKeyDown(int key) {return ??}

    public boolean isLeftClick() {return left_click;};

    public boolean isRightClick() {return right_click;};

    /* takes logic params according to UML (Player car) */
    public void handleInput() {};

    //public boolean isEscPressedOnce() {};

}
