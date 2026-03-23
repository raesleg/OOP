package io.github.raesleg.game.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import io.github.raesleg.engine.system.IGameSystem;

/**
 * PoliceLightSystem — Renders a red/blue oscillating glow at the bottom
 * screen edge when the police car is nearby.
 * <p>
 * Intensity scales with normalised police distance (0 = caught, 1 = far).
 * When far away the effect is invisible; as the police close in the
 * flashing becomes brighter and faster.
 * <p>
 * <b>SRP:</b> Purely visual — no game-state mutation.
 */
public final class PoliceLightSystem implements IGameSystem {

    private static final float BASE_FLASH_FREQ = 3f;
    private static final float MAX_FLASH_FREQ = 10f;
    private static final float GLOW_HEIGHT = 60f;

    private float timer;
    private float normalisedDistance = 1f;

    /**
     * Called each frame with the normalised police distance (0..1).
     * 0 means the police has caught the player; 1 means max distance.
     */
    public void setNormalisedDistance(float d) {
        this.normalisedDistance = Math.max(0f, Math.min(1f, d));
    }

    @Override
    public void update(float deltaTime) {
        timer += deltaTime;
    }

    /**
     * Renders the red/blue glow bar at the bottom of the visible area.
     * Must be called with an un-begun ShapeRenderer; this method manages
     * begin/end internally.
     *
     * @param sr      un-begun ShapeRenderer
     * @param visMinX left edge of visible area (world coords)
     * @param visMinY bottom edge of visible area (world coords)
     * @param visMaxX right edge of visible area (world coords)
     */
    public void render(ShapeRenderer sr, float visMinX, float visMinY, float visMaxX) {
        float intensity = 1f - normalisedDistance;
        if (intensity < 0.05f)
            return;

        float freq = BASE_FLASH_FREQ + (MAX_FLASH_FREQ - BASE_FLASH_FREQ) * intensity;
        float phase = (float) Math.sin(timer * freq * Math.PI * 2f);

        float redAlpha = intensity * Math.max(0f, phase) * 0.45f;
        float blueAlpha = intensity * Math.max(0f, -phase) * 0.45f;

        float width = visMaxX - visMinX;
        float midX = visMinX + width / 2f;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Red glow on left half
        sr.setColor(new Color(1f, 0f, 0f, redAlpha));
        sr.rect(visMinX, visMinY, width / 2f, GLOW_HEIGHT);

        // Blue glow on right half
        sr.setColor(new Color(0f, 0f, 1f, blueAlpha));
        sr.rect(midX, visMinY, width / 2f, GLOW_HEIGHT);

        sr.end();
    }

    @Override
    public void dispose() {
        // No resources to release
    }
}
