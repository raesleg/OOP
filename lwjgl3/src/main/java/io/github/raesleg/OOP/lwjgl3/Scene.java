package io.github.raesleg.OOP.lwjgl3;

import java.util.List;

public class Scene {

    /* Protected Variables */
    protected SceneManager sceneM;
    protected IOManager inputM;
    protected List<Entity> entities;
    protected boolean isLoaded;

    /* Public Functions */
    public Scene() {};

    public Scene(SceneManager sceneM) {};

    public void IO(IOManager inputM) {};

    public void load() {};

    public void unload() {};

    public void update(double deltaTime) {};

}
