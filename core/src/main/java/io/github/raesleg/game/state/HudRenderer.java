package io.github.raesleg.game.state;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * HudRenderer — Encapsulates the raw SpriteBatch draw calls for every
 * HUD element (dashboard speed, wanted stars, progress bar, police bar, fuel
 * bar).
 * <p>
 * Extracted from {@link DashboardUI} to satisfy SRP: DashboardUI owns state
 * observation and layout; this class owns pixel-level rendering.
 */
public class HudRenderer {

    /* ── Layout constants ── */
    private static final float DASHBOARD_W = 280f;
    private static final float DASHBOARD_H = 140f;
    private static final float STAR_SIZE = 28f;
    private static final float ICON_SIZE = 28f;
    private static final float BAR_WIDTH = 300f;
    private static final int MAX_WANTED_STARS = 5;
    private static final float LABEL_SCALE = 2f;

    /* ── Shared textures (owned by DashboardUI, passed in) ── */
    private final Texture dashboardTex;
    private final Texture starTex;
    private final Texture carTex;
    private final Texture policeTex;
    private final Texture pixelTex;
    private final Texture finishIcon;
    private final Texture startIcon;
    private final Texture chargeTex;

    /* ── Fonts (owned by DashboardUI) ── */
    private final BitmapFont font;
    private final BitmapFont speedFont;

    public HudRenderer(
            Texture dashboardTex, Texture starTex, Texture carTex,
            Texture policeTex, Texture pixelTex, Texture finishIcon,
            Texture startIcon, Texture chargeTex,
            BitmapFont font, BitmapFont speedFont) {
        this.dashboardTex = dashboardTex;
        this.starTex = starTex;
        this.carTex = carTex;
        this.policeTex = policeTex;
        this.pixelTex = pixelTex;
        this.finishIcon = finishIcon;
        this.startIcon = startIcon;
        this.chargeTex = chargeTex;
        this.font = font;
        this.speedFont = speedFont;
    }

    public void drawDashboardSpeed(SpriteBatch batch, int currentSpeed) {
        float dx = 1280f - DASHBOARD_W - 10f;
        float dy = 5f;
        batch.draw(dashboardTex, dx, dy, DASHBOARD_W, DASHBOARD_H);

        String speedText = currentSpeed + "";
        speedFont.setColor(Color.WHITE);
        speedFont.draw(batch, speedText,
                dx + DASHBOARD_W * 0.40f,
                dy + DASHBOARD_H * 0.70f);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "KM/H",
                dx + DASHBOARD_W * 0.35f,
                dy + DASHBOARD_H * 0.45f);
        font.setColor(Color.WHITE);
    }

    public void drawWantedStars(SpriteBatch batch, int currentRulesBroken) {
        float startX = 1280f - 10f - (MAX_WANTED_STARS * (STAR_SIZE + 4f));
        float y = 720f - 14f - STAR_SIZE;

        font.draw(batch, "WANTED:", startX - 160f, y + STAR_SIZE - 2f);

        for (int i = 0; i < MAX_WANTED_STARS; i++) {
            float x = startX + i * (STAR_SIZE + 4f);
            if (i < currentRulesBroken) {
                batch.setColor(1f, 1f, 1f, 1f);
            } else {
                batch.setColor(1f, 1f, 1f, 0.2f);
            }
            batch.draw(starTex, x, y, STAR_SIZE, STAR_SIZE);
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public void drawPoliceDistanceBar(SpriteBatch batch, float policeDistance) {
        float barX = (1280f - BAR_WIDTH) / 2f;
        float barY = 720f - 30f;
        float lineH = 6f;

        batch.setColor(0.3f, 0.3f, 0.3f, 0.6f);
        batch.draw(pixelTex, barX, barY - lineH / 2f, BAR_WIDTH, lineH);

        float dangerWidth = (1f - policeDistance) * BAR_WIDTH;
        if (dangerWidth > 0) {
            batch.setColor(0.9f, 0.2f, 0.2f, 0.7f);
            batch.draw(pixelTex, barX, barY - lineH / 2f, dangerWidth, lineH);
        }
        batch.setColor(1f, 1f, 1f, 1f);

        float carX = barX + BAR_WIDTH - ICON_SIZE;
        float carY = barY - ICON_SIZE / 2f;
        batch.draw(carTex, carX, carY, ICON_SIZE, ICON_SIZE);

        float policeX = barX + (1f - policeDistance) * (BAR_WIDTH - ICON_SIZE * 2f);
        batch.draw(policeTex, policeX, carY, ICON_SIZE, ICON_SIZE);

        font.setColor(Color.LIGHT_GRAY);
        font.getData().setScale(1.2f);
        font.draw(batch, "POLICE", barX - 100f, barY + 8f);
        font.getData().setScale(LABEL_SCALE);
        font.setColor(Color.WHITE);
    }

    public void drawProgressBar(SpriteBatch batch, float currentProgress) {
        float barX = (1280f - BAR_WIDTH) / 2f;
        float barY = 720f - 22f;
        float lineH = 3f;

        batch.setColor(Color.WHITE);
        batch.draw(pixelTex, barX, barY - lineH / 2f, BAR_WIDTH, lineH);

        float sflagWidth = 50f;
        float sflagheight = 30f;
        float sflagX = barX - 50f;
        float sflagY = barY - sflagWidth / 2f + 10f;
        batch.draw(startIcon, sflagX, sflagY, sflagWidth, sflagheight);

        float fflagSize = 28f;
        float fflagX = barX + BAR_WIDTH + 6f;
        float fflagY = barY - fflagSize / 2f - 6f;
        batch.draw(finishIcon, fflagX, fflagY, fflagSize, fflagSize);

        float carW = 24f;
        float carH = 24f;
        float carX = barX + currentProgress * (BAR_WIDTH - carW);
        float carY = barY - carH / 2f;
        batch.setColor(Color.WHITE);
        batch.draw(carTex, carX, carY, carW, carH);
    }

    public void drawFuelBar(SpriteBatch batch, float currentFuel) {
        float barX = 20f;
        float barY = 30f;
        float barW = 200f;
        float barH = 18f;
        float iconSize = 32f;

        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(chargeTex, barX, barY - 6f, iconSize, iconSize);

        float fillX = barX + iconSize + 6f;

        batch.setColor(0.2f, 0.2f, 0.2f, 0.7f);
        batch.draw(pixelTex, fillX, barY, barW, barH);

        float r, g;
        if (currentFuel > 0.5f) {
            r = 1f - (currentFuel - 0.5f) * 2f;
            g = 1f;
        } else {
            r = 1f;
            g = currentFuel * 2f;
        }
        batch.setColor(r, g, 0.15f, 0.9f);
        batch.draw(pixelTex, fillX, barY, barW * currentFuel, barH);

        batch.setColor(1f, 1f, 1f, 1f);
        font.setColor(Color.WHITE);
        font.getData().setScale(1.4f);
        font.draw(batch, "FUEL", fillX, barY + barH + 22f);
        font.getData().setScale(LABEL_SCALE);
    }
}
