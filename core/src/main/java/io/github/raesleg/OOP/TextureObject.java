package io.github.raesleg.OOP;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public abstract class TextureObject extends Entity{

    /* Private Variables */
    private Texture texture;

    /* Public Functions */
    public TextureObject() {
        super();
        this.texture = null;
    }

    public TextureObject(String filename, float x, float y, float speed) {
        super(x, y, speed);
        texture = new Texture(Gdx.files.internal(filename));
    }

    public Texture getTexture() {
        return texture;
}

    public void movement() { }

    @Override
    public void draw(SpriteBatch batch) {
        if (texture != null) {
            batch.draw(texture, getX(), getY());
        }
    }

    @Override
    public void dispose() {
        if (texture != null) {
            texture.dispose();
        }
    }
}
