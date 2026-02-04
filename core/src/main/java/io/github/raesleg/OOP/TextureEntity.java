package io.github.raesleg.OOP;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public abstract class TextureEntity extends Entity{

    /* Private Variables */
    private Texture texture;

    /* Public Functions */
    public TextureEntity() {
        super();
        this.texture = null;
    }

    public TextureEntity(String filename, float x, float y, float speed) {
        super(x, y, speed);
        texture = new Texture(filename);
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
