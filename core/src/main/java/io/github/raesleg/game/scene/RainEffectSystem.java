package io.github.raesleg.game.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import io.github.raesleg.game.GameConstants;

/**
 * RainEffectSystem — Renders the Level 2 rain overlay (atmosphere,
 * wet-lens blur, vignette, and animated rain streaks).
 * <p>
 * Extracted from Level2Scene to satisfy SRP: weather rendering is
 * one visual responsibility, independent of game logic, scoring,
 * or entity management.
 * <p>
 * <b>Design Pattern:</b> Strategy — the scene selects which rendering
 * systems to compose; this system is only instantiated in Level 2.
 */
public final class RainEffectSystem {

    private final int dropCount;
    private final float[] dropX;
    private final float[] dropY;
    private final float[] dropLen;
    private final float[] dropSpd;
    private boolean dropsReady;
    private float rainTime;

    public RainEffectSystem() {
        this.dropCount = GameConstants.RAIN_DROP_COUNT;
        this.dropX = new float[dropCount];
        this.dropY = new float[dropCount];
        this.dropLen = new float[dropCount];
        this.dropSpd = new float[dropCount];
        this.dropsReady = false;
        this.rainTime = 0f;
    }

    /**
     * Renders the complete rain overlay (atmosphere + vignette + streaks).
     * Must be called between the road draw and the entity draw pass.
     */
    public void render(ShapeRenderer sr, SpriteBatch batch,
            float visMinX, float visMinY, float visMaxX, float visMaxY) {
        initDropsIfNeeded(visMinX, visMinY, visMaxX, visMaxY);

        float dt = Gdx.graphics.getDeltaTime();
        rainTime += dt;

        float visW = visMaxX - visMinX;
        float visH = visMaxY - visMinY;
        advanceDrops(dt, visMinX, visMinY, visW, visH);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        drawAtmosphere(sr, visMinX, visMinY, visW, visH);
        drawWetLensBlur(sr, visMinX, visMinY, visW, visH);
        drawVignette(sr, visMinX, visMinY, visW, visH);
        sr.end();

        sr.begin(ShapeRenderer.ShapeType.Line);
        drawRainStreaks(sr, visMinX, visMinY);
        sr.end();
        // Do NOT disable GL_BLEND — SpriteBatch requires it for texture alpha
    }

    /* ── Private helpers ── */

    private void initDropsIfNeeded(float visMinX, float visMinY, float visMaxX, float visMaxY) {
        if (dropsReady)
            return;
        float visW = visMaxX - visMinX;
        float visH = visMaxY - visMinY;
        for (int i = 0; i < dropCount; i++) {
            dropX[i] = MathUtils.random(visMinX, visMinX + visW);
            dropY[i] = MathUtils.random(visMinY, visMinY + visH);
            dropLen[i] = MathUtils.random(10f, 28f);
            dropSpd[i] = MathUtils.random(500f, 900f);
        }
        dropsReady = true;
    }

    private void advanceDrops(float dt, float visMinX, float visMinY, float visW, float visH) {
        for (int i = 0; i < dropCount; i++) {
            dropY[i] -= dropSpd[i] * dt;
            dropX[i] -= dropSpd[i] * 0.12f * dt;
            if (dropY[i] < visMinY - dropLen[i]) {
                dropY[i] = visMinY + visH + dropLen[i];
                dropX[i] = MathUtils.random(visMinX, visMinX + visW);
            }
            if (dropX[i] < visMinX - dropLen[i]) {
                dropX[i] = visMinX + visW + dropLen[i];
            }
        }
    }

    private void drawAtmosphere(ShapeRenderer sr, float visMinX, float visMinY, float visW, float visH) {
        sr.setColor(0.10f, 0.13f, 0.22f, 0.22f);
        sr.rect(visMinX, visMinY, visW, visH);
    }

    private void drawWetLensBlur(ShapeRenderer sr, float visMinX, float visMinY, float visW, float visH) {
        for (int pass = 0; pass < 5; pass++) {
            float ox = MathUtils.sin(pass * 1.3f) * 2.5f;
            float oy = MathUtils.cos(pass * 1.1f) * 2.5f;
            sr.setColor(0.12f, 0.18f, 0.28f, 0.045f);
            sr.rect(visMinX + ox, visMinY + oy, visW, visH);
        }
    }

    private void drawVignette(ShapeRenderer sr, float visMinX, float visMinY, float visW, float visH) {
        float vigAlpha = 0.40f + MathUtils.sin(rainTime * 1.6f) * 0.04f;

        float edgeW = 45f;
        float topH = 55f;
        float bottomH = 45f;

        sr.setColor(0.02f, 0.03f, 0.06f, vigAlpha * 0.7f);
        sr.rect(visMinX, visMinY, edgeW, visH);
        sr.rect(visMinX + visW - edgeW, visMinY, edgeW, visH);

        sr.setColor(0.02f, 0.03f, 0.06f, vigAlpha * 0.8f);
        sr.rect(visMinX, visMinY + visH - topH, visW, topH);
        sr.rect(visMinX, visMinY, visW, bottomH);
    }

    private void drawRainStreaks(ShapeRenderer sr, float visMinX, float visMinY) {
        for (int i = 0; i < dropCount; i++) {
            float alpha = MathUtils.random(0.28f, 0.60f);
            sr.setColor(0.72f, 0.84f, 1.0f, alpha);
            float endX = dropX[i] + dropLen[i] * 0.18f;
            float endY = dropY[i] + dropLen[i];
            sr.line(dropX[i], dropY[i], endX, endY);
            if (i % 5 == 0) {
                sr.setColor(0.88f, 0.94f, 1.0f, alpha * 0.45f);
                sr.line(dropX[i] + 1f, dropY[i], endX + 1f, endY);
            }
        }
    }
}
