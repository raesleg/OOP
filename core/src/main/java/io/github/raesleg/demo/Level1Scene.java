package io.github.raesleg.demo;

import com.badlogic.gdx.Gdx;

/**
 * Level1Scene — Sunny road (normal traffic, no police).
 * <p>
 * Extends {@link BaseGameScene} and provides Level 1 configuration.
 * Teammates inject traffic spawning, obstacle placement, and
 * additional game rules via the {@link #initLevelData()} and
 * {@link #updateGame(float)} hooks.
 *
 * <pre>
 * +---------------------------------------------------------------+
 * | SCORE: 0       [S] ----------C---------- [F]   WANTED: [ ]    |
 * |                                                                |
 * |                        (sunny road)                            |
 * |                                                                |
 * |                                                SPEED: 0 KM/H  |
 * +---------------------------------------------------------------+
 * </pre>
 */

public class Level1Scene extends BaseGameScene {
    
    /* ── Level parameters ── */
    private static final float LEVEL_LENGTH = 50000f;
    private static final float MAX_SPEED = 200f;
    private static final float ACCELERATION = 60f;
    private static final float BRAKE_RATE = 80f;
    
    /* ── NPC traffic configuration ── */
    private static final float NPC_SPAWN_INTERVAL = 2.0f;
    
    /* ── Level-specific components ── */
    private NPCCarSpawner npcSpawner;

    @Override
    protected float getLevelLength() {
        return LEVEL_LENGTH;
    }

    @Override
    protected float getMaxSpeed() {
        return MAX_SPEED;
    }

    @Override
    protected float getAcceleration() {
        return ACCELERATION;
    }

    @Override
    protected float getBrakeRate() {
        return BRAKE_RATE;
    }

    @Override
    protected String getBgmPath() {
        return "bgm.ogg";
    }

    @Override
    protected void initLevelData() {
        Gdx.app.log("Level1Scene", "=== INIT LEVEL DATA START ===");
        
        // Create NPC car spawner
        npcSpawner = new NPCCarSpawner(
            getEntityManager(),
            getWorld(),
            VIRTUAL_HEIGHT,
            NPC_SPAWN_INTERVAL
        );
        
        Gdx.app.log("Level1Scene", "NPC spawner created");
        
        // Register collision sounds
        // If you don't have these sound files, create placeholder files or comment these out
        try {
            getSound().addSound("boundary_hit", "hit_sound.wav");
            Gdx.app.log("Level1Scene", "Registered boundary_hit sound");
        } catch (Exception e) {
            Gdx.app.log("Level1Scene", "WARNING: Could not load boundary_hit sound: " + e.getMessage());
        }
        
        try {
            getSound().addSound("crash", "crash_sound.wav");
            Gdx.app.log("Level1Scene", "Registered crash sound");
        } catch (Exception e) {
            Gdx.app.log("Level1Scene", "WARNING: Could not load crash sound: " + e.getMessage());
        }
        
        Gdx.app.log("Level1Scene", "=== INIT LEVEL DATA COMPLETE ===");
    }

    @Override
    protected void updateGame(float deltaTime) {
        // Update NPC spawner
        if (npcSpawner != null) {
            npcSpawner.update(deltaTime, getScrollOffset());
        }
        
        // Check level completion
        float progress = Math.min(1f, (-getScrollOffset()) / getLevelLength());
        
        if (progress >= 1.0f) {
            Gdx.app.log("Level1Scene", "Level 1 complete! Score: " + getScore());
            // TODO: Transition to results or next level
        }
    }

    @Override
    protected void disposeLevelData() {
        // Clean up NPC spawner
        if (npcSpawner != null) {
            npcSpawner.clearAll();
            npcSpawner = null;
        }
        
        Gdx.app.log("Level1Scene", "Level 1 data disposed");
    }
}
