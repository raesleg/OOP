package io.github.raesleg.OOP;

import java.util.HashMap;
import java.util.Map;

public class SceneManager {

    /* Private Variables */
    private Map<String, Scene> scenes = new HashMap<>();
    private Scene currentScene;

    /* Public Functions */
    public void initialise(){};

    public void update(float deltaTime) {};

    public void addScene(String name, Scene scene) {};

    public void loadScene(String name) {};

    public Scene getCurrentScene() {return currentScene;};

    public void removeScene(String name) {};

}
