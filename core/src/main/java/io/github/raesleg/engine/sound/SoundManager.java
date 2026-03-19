package io.github.raesleg.engine.sound;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;

import io.github.raesleg.engine.io.SoundDevice;

public class SoundManager implements SoundDevice {

    // Static variable to track mute state
    private boolean muted = false;
    private float masterVolume = 1.0f;

    public void toggleMute() {
        muted = !muted;
        Gdx.app.log("SoundManager", muted ? "Muted" : "Unmuted");
    }

    public boolean isMuted() {
        return muted;
    }

    // Map to hold sound effects
    private Map<String, SoundEffect> sounds = new HashMap<>();

    // Add sound into the list
    public void addSound(String name, String filePath) {
        sounds.put(name, new SoundEffect(filePath));
    }

    // Play sound by their name
    public void playSound(String name, float volume) {
        if (muted) {
            return;
        }
        if (sounds.containsKey(name)) {
            sounds.get(name).play(volume * masterVolume);
        }
    }

    // Loop sound by their name
    public void loopSound(String name) {
        if (muted) {
            return;
        }
        if (sounds.containsKey(name)) {
            SoundEffect sfx = sounds.get(name);
            sfx.loop();
            sfx.setVolume(masterVolume);
        }
    }

    // Stop sound by their name
    public void stopSound(String name) {
        if (sounds.containsKey(name)) {
            sounds.get(name).stop();
        }
    }

    // Set volume on a currently playing/looping sound (scaled by master)
    public void setSoundVolume(String name, float volume) {
        if (sounds.containsKey(name)) {
            sounds.get(name).setVolume(volume * masterVolume);
        }
    }

    // Stop all registered sounds
    public void stopAllSounds() {
        for (SoundEffect sound : sounds.values()) {
            sound.stop();
        }
    }

    // Check if a sound is currently looping
    public boolean isLooping(String name) {
        if (sounds.containsKey(name)) {
            return sounds.get(name).isLooping();
        }
        return false;
    }

    public float getMasterVolume() {
        return masterVolume;
    }

    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0f, Math.min(1f, volume));
    }

    public void dispose() {
        for (SoundEffect sound : sounds.values()) {
            sound.dispose();
        }
    }
}