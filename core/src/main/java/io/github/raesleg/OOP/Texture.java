package io.github.raesleg.OOP;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public abstract class Texture extends Entity{

    /* Private Variables */
    private com.badlogic.gdx.graphics.Texture texture;

    /* Public Functions */
    public void Tex() {
        texture = null;
    };

    public void Tex(String filename, float x, float y, float speed) {
        setX(x);
        setY(y);
        setSpeed(speed);
        texture = new com.badlogic.gdx.graphics.Texture(filename);
    };

    public com.badlogic.gdx.graphics.Texture getTexture() {
        return texture;
    };

    public void movement() { };

    public void draw(SpriteBatch batch){
        if (texture != null) {
            batch.draw(texture, getX(), getY());
        }
    };

    public void dispose() {
        if (texture != null) {
            texture.dispose();
        }
    };
}
