package io.github.raesleg.game.zone;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.physics.box2d.BodyDef;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.entity.IExpirable;
import io.github.raesleg.engine.physics.PhysicsWorld;
import io.github.raesleg.game.movement.SurfaceEffect;

/**
 * RoadHazard — Generic scrollable MotionZone for surface hazards.
 * Replaces the old Puddle class. Both puddles and oil spills are
 * RoadHazard instances with different SurfaceEffect and textures —
 * no duplicated zone logic needed (Open/Closed Principle).
 *
 * Usage:
 *   new RoadHazard(world, x, y, w, h, SurfaceEffect.PUDDLE,   "puddle.png")
 *   new RoadHazard(world, x, y, w, h, SurfaceEffect.MUD, "mud.png")
 */
public class RoadHazard extends MotionZone implements IExpirable {

    private static Texture puddleTexture;
    private static Texture oilTexture;

    private final float relativeY;
    private final String texturePath;
    private Texture texture;
    private boolean expired;

    public RoadHazard(PhysicsWorld world, float centreXPx, float relativeY,
            float wPx, float hPx, SurfaceEffect effect, String texturePath) {
        super(centreXPx - wPx / 2f, 0, wPx, hPx,
                effect,
                new Color(0.3f, 0.5f, 0.9f, 0.35f),
                world.createBody(
                        BodyDef.BodyType.KinematicBody,
                        centreXPx / Constants.PPM,
                        relativeY / Constants.PPM,
                        (wPx / Constants.PPM) / 2f,
                        (hPx / Constants.PPM) / 2f,
                        0f, 0f, true, null));
        this.relativeY = relativeY;
        this.texturePath = texturePath;
        this.expired = false;
        loadTexture();
    }

    private void loadTexture() {
        // Cache per texture path to avoid reloading same texture repeatedly
        if (texturePath.equals("puddle.png")) {
            if (puddleTexture == null) puddleTexture = new Texture(texturePath);
            texture = puddleTexture;
        } else if (texturePath.equals("oilspill.png")) {
            if (oilTexture == null) oilTexture = new Texture(texturePath);
            texture = oilTexture;
        } else {
            texture = new Texture(texturePath);
        }
    }

    public void updatePosition(float scrollOffset) {
        float screenY = relativeY + scrollOffset;
        setY(screenY);
        getBody().setPosition(
                (getX() + getW() / 2f) / Constants.PPM,
                (screenY + getH() / 2f) / Constants.PPM);
        if (screenY < -getH() * 3f) {
            expired = true;
        }
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    public void markExpired() {
        expired = true;
    }

    /** Called by spawner for correct z-order (drawn before entity pass). */
    public void drawHazard(SpriteBatch batch) {
        if (texture != null) {
            batch.draw(texture, getX(), getY(), getW(), getH());
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        // no-op — drawn explicitly by spawner for z-ordering
    }

    @Override
    public void draw(ShapeRenderer sr) {
        // no-op
    }

    @Override
    public void dispose() {
        getBody().destroy();
        // Note: shared textures (puddle/oil) are intentionally not disposed here
        // as other instances may still reference them
    }
}