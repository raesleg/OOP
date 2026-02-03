package io.github.raesleg.OOP;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public abstract class Texture extends Entity{

    /* Private Variables */
    private Texture texture;

    /* Public Functions */
    public void Tex(){};

    public void Tex(String filename, float x, float y, float speed) {};

    public Texture getTexture() {return texture;};

    public void movement(){};

    public void draw(SpriteBatch batch){};

    public void dispose() {};
}
