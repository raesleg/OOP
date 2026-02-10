package io.github.raesleg.engine;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public abstract class TextureObject extends Entity{

    private Texture texture;
    private float width;
    private float height;

    public TextureObject(String filename, float x, float y, float width, float height) {
        super(x, y);
        texture = new Texture(filename);
        this.width = width;
        this.height = height;
    }

    public Texture getTexture() {
        return texture;
    }

    public float getWidth(){
        return width;
    }

    public float getHeight() {
        return height;
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (texture != null) {
            batch.draw(
                texture,
                getX() - width / 2f,
                getY() - height / 2f,
                width,
                height
            );
        }
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
