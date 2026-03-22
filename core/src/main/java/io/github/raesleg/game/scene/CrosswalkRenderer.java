package io.github.raesleg.game.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import io.github.raesleg.game.zone.CrosswalkZone;

import java.util.List;

/**
 * CrosswalkRenderer — Renders crosswalk zone overlays for Level 1.
 * <p>
 * Extracted from Level1Scene to satisfy SRP: crosswalk visual rendering
 * is one responsibility, independent of crosswalk encounter logic,
 * collision detection, or pedestrian management.
 * <p>
 * Requires an external list of {@link CrosswalkZone} objects supplied
 * each frame by the owning scene (Dependency Injection via method).
 */
public final class CrosswalkRenderer {

    /**
     * Renders all visible crosswalk zones using alpha-blended filled shapes.
     *
     * @param sr    a ShapeRenderer (not currently in begin/end)
     * @param zones the live crosswalk zone list from the encounter system
     */
    public void render(ShapeRenderer sr, List<CrosswalkZone> zones) {
        if (zones == null || zones.isEmpty())
            return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (CrosswalkZone zone : zones) {
            zone.draw(sr);
        }
        sr.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}
