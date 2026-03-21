package io.github.raesleg.engine.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.HashMap;
import java.util.Map;

public abstract class TextureObject extends Entity {

    /* ── Shared texture cache (Flyweight) ── */
    private static Map<String, Texture> textureCache = new HashMap<>();

    private Texture texture;

    public TextureObject(String filename, float x, float y, float w, float h) {
        super(x, y, w, h);
        this.texture = textureCache.computeIfAbsent(filename, Texture::new);
    }

    public Texture getTexture() {
        return texture;
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (texture != null) {
            batch.draw(texture, getX(), getY(), getW(), getH());
        }
    }

    /**
     * Individual dispose is now a no-op — the shared texture is owned by
     * the cache. Call {@link #disposeAllTextures()} at application shutdown.
     */
    @Override
    public void dispose() {
        // no-op: texture is shared via the cache
    }

    /**
     * Disposes <b>every</b> cached texture and clears the cache.
     * Call once from the top-level dispose
     * (e.g.&nbsp;{@code GameMaster.dispose()}).
     */
    public static void disposeAllTextures() {
        for (Texture t : textureCache.values()) {
            t.dispose();
        }
        textureCache.clear();
    }
}
