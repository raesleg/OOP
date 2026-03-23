package io.github.raesleg.game.state;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

import io.github.raesleg.engine.io.SoundDevice;
import io.github.raesleg.engine.system.IGameSystem;

/**
 * AudioController — Manages the driving sound loop and background music
 * volume synchronisation.
 * <p>
 * Extracted from BaseGameScene to satisfy SRP: audio lifecycle is a
 * single responsibility independent of speed, scoring, or rendering.
 */
public final class AudioController implements IGameSystem {

    private final SoundDevice sound;
    private Music bgm;
    private static final float BGM_BASE_VOLUME = 0.3f;

    private boolean playerMoving;
    private boolean accelerating;
    private boolean braking;

    public AudioController(SoundDevice sound) {
        this.sound = sound;
    }

    /** Loads and starts the BGM track. Call once during scene show(). */
    public void startBgm(String bgmPath) {
        if (bgmPath != null) {
            bgm = Gdx.audio.newMusic(Gdx.files.internal(bgmPath));
            bgm.setLooping(true);
            syncBgmVolume();
            bgm.play();
        }
    }

    /**
     * Called by the scene each frame to supply whether the player car is moving.
     */
    public void setPlayerMoving(boolean moving) {
        this.playerMoving = moving;
    }
    
    public void setAccelerating(boolean accelerating) {
        this.accelerating = accelerating;
    }

    public void setBraking(boolean braking) {
        this.braking = braking;
    }

    @Override
    public void update(float deltaTime) {
        updateMoveLoop();
        syncBgmVolume();
    }

    @Override
    public void dispose() {
        sound.stopAllSounds();
        if (bgm != null) {
            bgm.stop();
            bgm.dispose();
            bgm = null;
        }
    }

    public void onPause() {
        stopMoveLoop();
        if (bgm != null)
            bgm.pause();
    }

    public void onResume() {
        if (bgm != null) {
            syncBgmVolume();
            bgm.play();
        }
    }

    public void toggleMute() {
        sound.toggleMute();
        syncBgmVolume();
        updateMoveLoop();
    }

    /** Lowers BGM volume for explosion effect. */
    public void dimBgm() {
        if (bgm != null)
            bgm.setVolume(0.05f);
    }

    /** Returns the BGM Music object (for external checks). */
    public Music getBgm() {
        return bgm;
    }

    private void stopMoveLoop() {
        sound.stopSound("drive");
    }

    private void updateMoveLoop() {
        if (sound.isMuted()) {
            sound.stopSound("drive");
            sound.stopSound("accelerate");
            sound.stopSound("brake");
            return;
        }

        if (braking) {
            sound.stopSound("drive");
            sound.stopSound("accelerate");

            if (!sound.isLooping("brake")) {
                sound.loopSound("brake");
            }
            return;
        }
        sound.stopSound("brake");

        // Accelerating
        if (accelerating) {
            sound.stopSound("drive");
            
            if(!sound.isLooping("accelerate")) {
                sound.loopSound("accelerate");
            }
            return;
        } 

        // Cruising
        if (playerMoving) {
            sound.stopSound("accelerate");

            if (!sound.isLooping("drive")) {
                sound.loopSound("drive");
            }
            return;
        }
        sound.stopSound("drive");
        sound.stopSound("accelerate");
    }

    private void syncBgmVolume() {
        if (bgm != null) {
            if (sound.isMuted()) {
                bgm.setVolume(0f);
            } else {
                bgm.setVolume(BGM_BASE_VOLUME * sound.getMasterVolume());
            }
        }
    }
}
