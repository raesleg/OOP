package io.github.raesleg.engine;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public abstract class TextureObject extends Entity{

    private Texture texture;

    public TextureObject(String filename, float x, float y, float w, float h) {
        super(x, y, w, h);
        texture = new Texture(filename);
    }

    public Texture getTexture() {
        return texture;
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (texture != null) {
            batch.draw(
                texture,
                getX() - getW() / 2f,
                getY() - getH() / 2f,
                getW(),
                getH()
            );
        }
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
