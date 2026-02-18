package io.github.raesleg.engine.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for every entity that renders a sprite.
 * <p>
 * Textures are loaded through a <b>static cache</b> (Flyweight pattern) so
 * that the same image file is read from disk and uploaded to the GPU only
 * once, no matter how many instances reference it. Without this cache,
 * creating 12&nbsp;{@code ExplosionParticle} objects would trigger 12
 * redundant disk reads &mdash; a major frame-time spike.
 */
public abstract class TextureObject extends Entity {

    /* ── Shared texture cache (Flyweight) ── */
    private static final Map<String, Texture> textureCache = new HashMap<>();

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
            batch.draw(
                    texture,
                    getX() - getW() / 2f,
                    getY() - getH() / 2f,
                    getW(),
                    getH());
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
