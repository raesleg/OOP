package io.github.raesleg.game.state;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * ScorePopupManager — Manages floating score change popups ("+50", "-100").
 * <p>
 * Extracted from DashboardUI to satisfy SRP: popup lifecycle and rendering
 * is a separate concern from the main HUD layout.
 * <p>
 * Each popup floats upward and fades out over its lifetime.
 */
public class ScorePopupManager implements Disposable {

    private static final float MAX_LIFETIME = 1.2f;
    private static final float RISE_SPEED = 60f;

    private final List<ScorePopup> popups = new ArrayList<>();
    private final BitmapFont popupFont;

    /** Internal data holder for a single floating popup. */
    private static class ScorePopup {
        float x, y;
        String text;
        Color color;
        float alpha;
        float lifetime;
    }

    public ScorePopupManager() {
        popupFont = new BitmapFont();
        popupFont.getData().setScale(2.5f);
    }

    /**
     * Spawns a new floating score popup.
     *
     * @param delta score change (positive = green, negative = red)
     */
    public void show(int delta) {
        ScorePopup p = new ScorePopup();
        p.x = 120f;
        p.y = 680f;
        p.text = (delta > 0 ? "+" : "") + delta;
        p.color = delta > 0 ? Color.GREEN : Color.RED;
        p.alpha = 1f;
        p.lifetime = MAX_LIFETIME;
        popups.add(p);
    }

    /**
     * Advances popup animations (rise + fade).
     *
     * @param deltaTime frame delta in seconds
     */
    public void update(float deltaTime) {
        Iterator<ScorePopup> it = popups.iterator();
        while (it.hasNext()) {
            ScorePopup p = it.next();
            p.lifetime -= deltaTime;
            p.y += RISE_SPEED * deltaTime;
            p.alpha = Math.max(0f, p.lifetime / MAX_LIFETIME);
            if (p.lifetime <= 0)
                it.remove();
        }
    }

    /**
     * Renders all active popups. Must be called inside a SpriteBatch begin/end
     * block.
     *
     * @param batch active SpriteBatch
     */
    public void render(SpriteBatch batch) {
        for (ScorePopup p : popups) {
            popupFont.setColor(p.color.r, p.color.g, p.color.b, p.alpha);
            popupFont.draw(batch, p.text, p.x, p.y);
        }
        popupFont.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        popupFont.dispose();
    }
}
